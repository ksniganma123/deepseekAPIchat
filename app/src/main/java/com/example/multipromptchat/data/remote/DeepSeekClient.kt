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
data class ChatMessage(val role: String, val content: String)

/** One step of a streamed reply. */
sealed interface StreamChunk {
    data class Content(val content: String, val reasoning: String = "") : StreamChunk
    data class Usage(val usage: TokenUsage) : StreamChunk
    data object Done : StreamChunk
    data class Error(val message: String) : StreamChunk
}

/** 非流式请求的结果（增量 C-4：关闭流式时走这里）。 */
sealed interface CompletionResult {
    data class Success(val content: String, val usage: TokenUsage?) : CompletionResult
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
     * 增量 C-4：非流式补全，一次性返回完整回复（关闭「流式输出」时走这里）。
     */
    suspend fun complete(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        topP: Float,
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
                CompletionResult.Success(content = content, usage = usage)
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
     * 任何失败都返回 null，调用方保留默认标题、不打扰用户。
     */
    suspend fun generateTitle(apiKey: String, userMessage: String): String? {
        val messages = listOf(
            ChatMessage(role = "system", content = TITLE_SYSTEM_PROMPT),
            ChatMessage(role = "user", content = userMessage.take(400)),
        )
        val result = runCatching {
            complete(
                apiKey = apiKey,
                model = DEFAULT_MODEL,
                messages = messages,
                temperature = 0.3f,
                topP = 0.9f,
                maxTokens = 32,
            )
        }.getOrNull()
        val content = (result as? CompletionResult.Success)?.content ?: return null
        return sanitizeTitle(content)
    }

    private fun sanitizeTitle(raw: String): String? {
        val cleaned = raw
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\"", "")
            .replace("“", "")
            .replace("”", "")
            .replace("「", "")
            .replace("」", "")
            .replace("《", "")
            .replace("》", "")
            .trim()
        if (cleaned.isEmpty()) return null
        return cleaned.take(TITLE_MAX_CHARS).trim().ifEmpty { null }
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
        temperature: Float,
        topP: Float,
        stream: Boolean = true,
    ): JSONObject {
        val array = JSONArray()
        messages.forEach { message ->
            val item = JSONObject()
            item.put("role", message.role)
            item.put("content", message.content)
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
            put("temperature", temperature.coerceIn(0f, 2f).toDouble())
            put("top_p", topP.coerceIn(0f, 1f).toDouble())
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
        private const val TITLE_SYSTEM_PROMPT =
            "你负责给对话起标题。只输出一个不超过10个字的中文标题，" +
                "不要标点、不要引号、不要解释、不要换行。"

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
