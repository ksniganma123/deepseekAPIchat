package com.example.multipromptchat.data.remote

import org.json.JSONObject

/** 增量 C-5：一次请求的 token 用量（DeepSeek 会返回缓存命中/未命中的输入 token）。 */
data class TokenUsage(
    val promptTokens: Long = 0L,
    val completionTokens: Long = 0L,
    val cacheHitTokens: Long = 0L,
    val cacheMissTokens: Long = 0L,
) {
    val totalTokens: Long get() = promptTokens + completionTokens

    companion object {
        const val KEY_PROMPT_TOKENS = "prompt_tokens"
        const val KEY_COMPLETION_TOKENS = "completion_tokens"
        const val KEY_CACHE_HIT = "prompt_cache_hit_tokens"
        const val KEY_CACHE_MISS = "prompt_cache_miss_tokens"

        fun fromJson(json: JSONObject?): TokenUsage? {
            if (json == null) return null
            return TokenUsage(
                promptTokens = json.optLong(KEY_PROMPT_TOKENS),
                completionTokens = json.optLong(KEY_COMPLETION_TOKENS),
                cacheHitTokens = json.optLong(KEY_CACHE_HIT),
                cacheMissTokens = json.optLong(KEY_CACHE_MISS),
            )
        }
    }
}

/** Result of interpreting one SSE `data:` payload. */
sealed interface SsePayload {

    /** A streamed delta; either visible content, hidden reasoning, or both. */
    data class Content(val content: String, val reasoning: String) : SsePayload

    /** A usage-only chunk (stream ends with `choices: []` + `usage`). */
    data class Usage(val usage: TokenUsage) : SsePayload

    /** The `[DONE]` sentinel that terminates a stream. */
    data object Done : SsePayload

    /** The server reported an error inside the stream. */
    data class Error(val message: String) : SsePayload

    /** Keep-alive comment, empty line, unknown shape, or unparsable JSON. */
    data object Ignored : SsePayload
}

/**
 * Parses DeepSeek (OpenAI-compatible) `chat.completion.chunk` events.
 *
 * okhttp-sse already strips the `data: ` prefix and joins multi-line payloads,
 * so this works on the raw payload only. Malformed JSON is never fatal: it is
 * reported back as [SsePayload.Ignored].
 */
object SseEventParser {

    private const val DONE_MARKER = "[DONE]"
    private const val KEY_ERROR = "error"
    private const val KEY_CHOICES = "choices"
    private const val KEY_DELTA = "delta"
    private const val KEY_CONTENT = "content"
    private const val KEY_REASONING = "reasoning_content"
    private const val KEY_USAGE = "usage"

    fun parse(raw: String): SsePayload {
        val payload = raw.trim()
        if (payload.isEmpty()) return SsePayload.Ignored
        if (payload == DONE_MARKER) return SsePayload.Done

        val json: JSONObject = runCatching { JSONObject(payload) }.getOrNull()
            ?: return SsePayload.Ignored

        val error = json.optJSONObject(KEY_ERROR)
        if (error != null) {
            val message = error.optString("message").takeIf { it.isNotBlank() }
            return SsePayload.Error(message ?: "服务返回错误")
        }

        val usageJson = if (json.isNull(KEY_USAGE)) null else json.optJSONObject(KEY_USAGE)

        val choices = json.optJSONArray(KEY_CHOICES)
        if (choices == null || choices.length() == 0) {
            // 纯 usage 收尾块。
            val usage = TokenUsage.fromJson(usageJson) ?: return SsePayload.Ignored
            return SsePayload.Usage(usage)
        }

        val choice = choices.optJSONObject(0) ?: return SsePayload.Ignored
        val delta = choice.optJSONObject(KEY_DELTA) ?: return SsePayload.Ignored

        // DeepSeek sends explicit JSON nulls (e.g. content == null on reasoning chunks),
        // so isNull() must be checked before optString() to avoid a literal "null".
        val content = if (delta.isNull(KEY_CONTENT)) "" else delta.optString(KEY_CONTENT, "")
        val reasoning = if (delta.isNull(KEY_REASONING)) "" else delta.optString(KEY_REASONING, "")
        if (content.isEmpty() && reasoning.isEmpty()) {
            // 有些实现在最后一个普通块里带 usage。
            val usage = TokenUsage.fromJson(usageJson) ?: return SsePayload.Ignored
            return SsePayload.Usage(usage)
        }

        return SsePayload.Content(content = content, reasoning = reasoning)
    }
}
