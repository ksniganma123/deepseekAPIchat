package com.example.multipromptchat.domain

/** Endpoint overrides go here so no key or host is scattered across the codebase. */
const val DEFAULT_BASE_URL = "https://api.deepseek.com"
const val DEFAULT_MODEL = "deepseek-chat"

const val DEFAULT_CONVERSATION_TITLE = "新会话"

/** A reusable persona template. Each conversation binds exactly one of these. */
data class PromptTemplate(
    val id: Long = 0L,
    val name: String,
    val systemPrompt: String,
    val temperature: Float = 0.7f,
    val topP: Float = 1f,
    val resetContextOnSwitch: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /** Short one-line teaser used in pickers and tags. */
    fun summary(maxChars: Int = 42): String {
        val flat = systemPrompt.replace('\n', ' ').trim()
        return if (flat.length <= maxChars) flat else flat.take(maxChars) + "…"
    }
}

data class Conversation(
    val id: Long = 0L,
    val title: String,
    val promptId: Long,
    val promptSnapshot: String,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val promptTokens: Long = 0L,
    val completionTokens: Long = 0L,
    val cacheHitTokens: Long = 0L,
    val cacheMissTokens: Long = 0L,
) {
    /** 本会话累计 token 消耗（输入 + 输出）。 */
    val totalTokens: Long get() = promptTokens + completionTokens

    /** 缓存命中率 0f..1f；没有任何输入 token 时为 null。 */
    val cacheHitRate: Float?
        get() {
            val total = cacheHitTokens + cacheMissTokens
            return if (total <= 0L) null else cacheHitTokens.toFloat() / total.toFloat()
        }
}

enum class MessageRole(val apiValue: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    ;

    companion object {
        fun fromApi(value: String?): MessageRole = when (value) {
            "user" -> USER
            "assistant" -> ASSISTANT
            "system" -> SYSTEM
            else -> USER
        }
    }
}

data class Message(
    val id: Long = 0L,
    val conversationId: Long,
    val role: MessageRole,
    val content: String,
    val reasoning: String? = null,
    val createdAt: Long = 0L,
)

/** Row model for the conversation list (conversation + prompt name + last message preview). */
data class ConversationSummary(
    val conversation: Conversation,
    val promptName: String?,
    val lastMessage: String?,
    val lastMessageAt: Long?,
)

data class PromptUsage(
    val prompt: PromptTemplate,
    val conversationCount: Int,
)
