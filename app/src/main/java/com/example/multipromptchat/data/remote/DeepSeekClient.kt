package com.example.multipromptchat.data.remote

import com.example.multipromptchat.domain.DEFAULT_BASE_URL
import com.example.multipromptchat.domain.DEFAULT_MODEL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** One entry of the `messages` array sent to the API. */
data class ChatMessage(
    val role: String,
    val content: String,
    /** 1.2：OpenAI 兼容的 `image_url`（data URL）。非空时 content 会变成多模态数组。 */
    val imageDataUrl: String? = null,
)

/** One step of a streamed reply. */
sealed interface StreamChunk {
    data class Content(val content: String, val reasoning: String = "") : StreamChunk
    data class Usage(val usage: TokenUsage) : StreamChunk
    data object Done : StreamChunk
    data class Error(val message: String) : StreamChunk
}

/** 1.2：一个可用模型 */
data class ModelInfo(
    val id: String,
    val ownedBy: String? = null,
    /** 疑似支持图片输入（含 flash / vl / vision 字样），仅用于界面提示 */
    val looksVisionCapable: Boolean = false,
)

/** 1.2：拉取模型列表的结果 */
sealed interface ModelListResult {
    data class Success(val models: List<ModelInfo>) : ModelListResult
    data class Failure(val message: String) : ModelListResult
}

/** 非流式请求的结果（增量 C-4：关闭流式时走这里）。 */
sealed interface CompletionResult {
    data class Success(
        val content: String,
        val usage: TokenUsage?,
        /** deepseek-reasoner 等模型会把思考过程放在这里，内容为空时可作为降级候选。 */
        val reasoning: String? = null,
    ) : CompletionResult
    data class Failure(val message: String) : CompletionResult
}

/**
 * 增量 A1「测试连接」的结果。由调用方翻译成面向用户的话术，
 * 这里只区分技术原因，不拼中文文案。
 */
sealed interface KeyCheckResult {
    data object Success : KeyCheckResult
    data object InvalidKey : KeyCheckResult
    data object NoBalance : KeyCheckResult
    data object RateLimited : KeyCheckResult
    data object NetworkError : KeyCheckResult
    data class ServerError(val code: Int) : KeyCheckResult
    data class Failed(val message: String) : KeyCheckResult
}

/**
 * Minimal DeepSeek chat client built directly on OkHttp + okhttp-sse.
 * No OpenAI SDK; the payload is assembled with org.json.
 *
 * The API key is only ever read from memory and put in the Authorization header —
 * it is never logged.
 */
class DeepSeekClient(
    private val client: OkHttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {

    fun streamChat(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        topP: Float,
    ): Flow<StreamChunk> = callbackFlow {
        val request = buildRequest(
            apiKey = apiKey,
            payload = buildPayload(model, messages, temperature, topP, stream = true),
        )

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                when (val payload = SseEventParser.parse(data)) {
                    is SsePayload.Content -> trySend(
                        StreamChunk.Content(content = payload.content, reasoning = payload.reasoning)
                    )
                    is SsePayload.Usage -> trySend(StreamChunk.Usage(payload.usage))
                    SsePayload.Done -> {
                        // [DONE] 是协议终点：把 Done 发出去后立刻关闭通道。
                        // 不能只等 onClosed —— 服务端可能保持连接不 EOF，
                        // 那样收集方永远不返回，"正在输入"就会一直挂着。
                        trySend(StreamChunk.Done)
                        close()
                    }
                    is SsePayload.Error -> trySend(StreamChunk.Error(payload.message))
                    SsePayload.Ignored -> Unit
                }
            }

            override fun onClosed(eventSource: EventSource) {
                // 兜底：服务端直接断流、没发 [DONE] 时也要收尾。
                trySend(StreamChunk.Done)
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                trySend(StreamChunk.Error(describeFailure(t, response)))
                close()
            }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(request, listener)

        // Cancelling the collecting coroutine (e.g. the Stop button) cancels the
        // EventSource, which closes the underlying HTTP call.
        awaitClose { eventSource.cancel() }
    }

    /**
     * 增量 A1：最小代价探活。发一次极短的非流式请求（max_tokens = 1），
     * 只看状态码，不解析正文、不写日志、不落盘。
     */
    suspend fun checkApiKey(apiKey: String): KeyCheckResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("model", DEFAULT_MODEL)
            put("max_tokens", 1)
            put("stream", false)
            put(
                "messages",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    }
                )
            )
        }
        val request = buildRequest(apiKey = apiKey, payload = payload)
        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> KeyCheckResult.Success
                    response.code == 401 || response.code == 403 -> KeyCheckResult.InvalidKey
                    response.code == 402 -> KeyCheckResult.NoBalance
                    response.code == 429 -> KeyCheckResult.RateLimited
                    response.code in 500..599 -> KeyCheckResult.ServerError(response.code)
                    else -> KeyCheckResult.Failed("HTTP ${response.code}")
                }
            }
        } catch (t: SocketTimeoutException) {
            KeyCheckResult.NetworkError
        } catch (t: UnknownHostException) {
            KeyCheckResult.NetworkError
        } catch (t: IOException) {
            KeyCheckResult.NetworkError
        } catch (t: Exception) {
            KeyCheckResult.Failed(t.message ?: t.javaClass.simpleName)
        }
    }

    /**
     * 1.2：拉取账号可用模型（OpenAI 兼容的 GET /models）。
     * 只读 id / owned_by，失败返回可读文案。
     */
    suspend fun listModels(apiKey: String): ModelListResult = withContext(Dispatchers.IO) {
        val request = try {
            Request.Builder()
                .url(baseUrl.trimEnd('/') + "/models")
                .addHeader("Authorization", "Bearer " + apiKey.trim())
                .addHeader("Accept", "application/json")
                .get()
                .build()
        } catch (t: Exception) {
            return@withContext ModelListResult.Failure("请求构造失败")
        }

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ModelListResult.Failure(describeFailure(null, response))
                }
                val body = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(body) }.getOrNull()
                if (json == null) {
                    return@withContext ModelListResult.Failure("返回内容无法解析")
                }
                val error = json.optJSONObject("error")
                if (error != null) {
                    return@withContext ModelListResult.Failure(
                        error.optString("message").ifBlank { "服务返回错误" }
                    )
                }
                val data = json.optJSONArray("data") ?: JSONArray()
                val models = buildList {
                    for (index in 0 until data.length()) {
                        val item = data.optJSONObject(index) ?: continue
                        val id = item.optString("id").trim()
                        if (id.isEmpty()) continue
                        val ownedBy = if (item.isNull("owned_by")) null else item.optString("owned_by")
                        val lower = id.lowercase()
                        add(
                            ModelInfo(
                                id = id,
                                ownedBy = ownedBy,
                                looksVisionCapable = lower.contains("flash") ||
                                    lower.contains("vl") ||
                                    lower.contains("vision"),
                            )
                        )
                    }
                }.distinctBy { it.id }.sortedBy { it.id }

                if (models.isEmpty()) {
                    ModelListResult.Failure("这个账号没有返回任何可用模型")
                } else {
                    ModelListResult.Success(models)
                }
            }
        } catch (t: SocketTimeoutException) {
            ModelListResult.Failure("请求超时，请检查网络后重试")
        } catch (t: UnknownHostException) {
            ModelListResult.Failure("无法连接网络，请检查网络设置")
        } catch (t: IOException) {
            ModelListResult.Failure("网络异常：" + (t.message ?: t.javaClass.simpleName))
        } catch (t: Exception) {
            ModelListResult.Failure(t.message ?: t.javaClass.simpleName)
        }
    }

    /**
     * 增量 C-4：非流式补全，一次性返回完整回复（关闭「流式输出」时走这里）。
     */
    suspend fun complete(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float? = null,
        topP: Float? = null,
        maxTokens: Int? = null,
    ): CompletionResult = withContext(Dispatchers.IO) {
        val payload = buildPayload(model, messages, temperature, topP, stream = false).apply {
            if (maxTokens != null) put("max_tokens", maxTokens)
        }
        val request = buildRequest(apiKey = apiKey, payload = payload)
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext CompletionResult.Failure(describeFailure(null, response))
                }
                val body = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(body) }.getOrNull()
                    ?: return@withContext CompletionResult.Failure("返回内容无法解析")
                val error = json.optJSONObject("error")
                if (error != null) {
                    return@withContext CompletionResult.Failure(
                        error.optString("message").ifBlank { "服务返回错误" }
                    )
                }
                val content = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.let { message -> if (message.isNull("content")) "" else message.optString("content", "") }
                    .orEmpty()
                val usage = TokenUsage.fromJson(
                    if (json.isNull("usage")) null else json.optJSONObject("usage")
                )
                val message = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                val reasoning = if (message == null || message.isNull("reasoning_content")) {
                    null
                } else {
                    message.optString("reasoning_content", "").takeIf { it.isNotBlank() }
                }
                CompletionResult.Success(content = content, usage = usage, reasoning = reasoning)
            }
        } catch (t: SocketTimeoutException) {
            CompletionResult.Failure("请求超时，请检查网络后重试")
        } catch (t: UnknownHostException) {
            CompletionResult.Failure("无法连接网络，请检查网络设置")
        } catch (t: IOException) {
            CompletionResult.Failure("网络异常：" + (t.message ?: t.javaClass.simpleName))
        } catch (t: Exception) {
            CompletionResult.Failure(t.message ?: t.javaClass.simpleName)
        }
    }

    /**
     * 增量 C-2：用一次极短的非流式请求生成 10 字以内的会话标题。
     *
     * 1.3 修复：改成两段式，兼容不同模型的参数支持差异 ——
     * 官方文档里 `deepseek-reasoner` 这类模型不支持 system 消息、也不支持
     * temperature / top_p；老实现只会硬发一种组合，失败即静默放弃。
     *
     * 抛出的所有异常都被吞掉；调用方拿 null 时会用本地兜底标题（见 TitleBuilder）。
     */
    suspend fun generateTitle(
        apiKey: String,
        userMessage: String,
        model: String = DEFAULT_MODEL,
    ): String? {
        val prompt = userMessage.take(TITLE_INPUT_MAX_CHARS)

        // 尝试 1：system 指令 + 采样参数（deepseek-chat / flash 的常规路径）
        val firstAttempt = runCatching {
            complete(
                apiKey = apiKey,
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = TITLE_SYSTEM_PROMPT),
                    ChatMessage(role = "user", content = prompt),
                ),
                temperature = 0.3f,
                topP = 0.9f,
                maxTokens = TITLE_MAX_TOKENS,
            )
        }.getOrNull()

        (firstAttempt as? CompletionResult.Success)?.let { success ->
            TitleBuilder.sanitize(success.content)?.let { return it }
        }

        // 尝试 2：不带 system、不带温度参数（reasoner 等模型）
        val secondAttempt = runCatching {
            complete(
                apiKey = apiKey,
                model = model,
                messages = listOf(ChatMessage(role = "user", content = TITLE_USER_PROMPT + prompt)),
                temperature = null,
                topP = null,
                maxTokens = TITLE_MAX_TOKENS,
            )
        }.getOrNull()

        val second = secondAttempt as? CompletionResult.Success ?: return null
        // content 为空时退一步看 reasoning_content（文档里两者并列返回）
        return TitleBuilder.sanitize(second.content)
            ?: TitleBuilder.sanitize(second.reasoning)
    }

    private fun buildRequest(apiKey: String, payload: JSONObject): Request =
        Request.Builder()
            .url(baseUrl.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey.trim())
            .addHeader("Accept", "text/event-stream")
            .addHeader("Content-Type", JSON_MEDIA_TYPE_VALUE)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

    private fun buildPayload(
        model: String,
        messages: List<ChatMessage>,
        temperature: Float? = null,
        topP: Float? = null,
        stream: Boolean = true,
    ): JSONObject {
        val array = JSONArray()
        messages.forEach { message ->
            val item = JSONObject()
            item.put("role", message.role)
            val image = message.imageDataUrl
            if (image.isNullOrBlank()) {
                item.put("content", message.content)
            } else {
                // OpenAI 兼容的多模态消息：文本 + 图片各一个 part。
                val parts = JSONArray()
                if (message.content.isNotBlank()) {
                    parts.put(
                        JSONObject().apply {
                            put("type", "text")
                            put("text", message.content)
                        }
                    )
                }
                parts.put(
                    JSONObject().apply {
                        put("type", "image_url")
                        put(
                            "image_url",
                            JSONObject().apply { put("url", image) },
                        )
                    }
                )
                item.put("content", parts)
            }
            array.put(item)
        }
        return JSONObject().apply {
            put("model", model)
            put("stream", stream)
            if (stream) {
                // 让流式响应在结尾带 usage，用于统计 token 与缓存命中率。
                put(
                    "stream_options",
                    JSONObject().apply { put("include_usage", true) },
                )
            }
            // 可空：某些模型（如 deepseek-reasoner）不支持采样参数，此时不下发
            temperature?.let { put("temperature", it.coerceIn(0f, 2f).toDouble()) }
            topP?.let { put("top_p", it.coerceIn(0f, 1f).toDouble()) }
            put("messages", array)
        }
    }

    private fun describeFailure(t: Throwable?, response: Response?): String = when {
        response != null -> when (response.code) {
            400 -> "请求参数有误（400）"
            401 -> "API Key 无效或已过期（401）"
            402 -> "账户余额不足（402）"
            403 -> "该 API Key 无权访问（403）"
            404 -> "接口地址不存在（404）"
            429 -> "请求过于频繁，请稍后再试（429）"
            in 500..599 -> "服务端错误（${response.code}），请稍后重试"
            else -> "请求失败（HTTP ${response.code}）"
        }
        t is SocketTimeoutException -> "请求超时，请检查网络后重试"
        t is UnknownHostException -> "无法连接网络，请检查网络设置"
        t != null -> "网络异常：" + (t.message ?: t.javaClass.simpleName)
        else -> "未知错误"
    }

    companion object {
        private const val JSON_MEDIA_TYPE_VALUE = "application/json; charset=utf-8"
        private val JSON_MEDIA_TYPE = JSON_MEDIA_TYPE_VALUE.toMediaType()

        const val TITLE_MAX_CHARS = 10

        /** 标题请求的输出预算：太小会被推理内容吃掉，导致 content 为空。 */
        private const val TITLE_MAX_TOKENS = 64

        /** 送进标题请求的用户文本上限（字节/字符都不宜过大）。 */
        private const val TITLE_INPUT_MAX_CHARS = 400

        private const val TITLE_SYSTEM_PROMPT =
            "你负责给对话起标题。只输出一个不超过10个字的中文标题，" +
                "不要标点、不要引号、不要解释、不要换行。"

        /** 无 system 权限的模型走这条：把要求写进 user 里。 */
        private const val TITLE_USER_PROMPT =
            "用不超过10个字概括下面这句话的主题，只输出标题本身，不要标点、引号或解释：\n"

        /**
         * Streaming-friendly client: no overall call timeout, and a generous read
         * timeout so slow tokens do not abort the stream.
         */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
