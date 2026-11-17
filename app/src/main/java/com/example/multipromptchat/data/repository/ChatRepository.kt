package com.example.multipromptchat.data.repository

import com.example.multipromptchat.data.image.ImageStore
import com.example.multipromptchat.data.local.ConversationDao
import com.example.multipromptchat.data.local.MessageDao
import com.example.multipromptchat.data.local.MessageEntity
import com.example.multipromptchat.data.local.PromptDao
import com.example.multipromptchat.data.local.toDomain
import com.example.multipromptchat.data.remote.ChatMessage
import com.example.multipromptchat.data.remote.CompletionResult
import com.example.multipromptchat.data.remote.DeepSeekClient
import com.example.multipromptchat.data.remote.KeyCheckResult
import com.example.multipromptchat.data.remote.ModelListResult
import com.example.multipromptchat.data.remote.StreamChunk
import com.example.multipromptchat.data.remote.TitleBuilder
import com.example.multipromptchat.data.remote.TokenUsage
import com.example.multipromptchat.domain.DEFAULT_CONVERSATION_TITLE
import com.example.multipromptchat.domain.DEFAULT_MODEL
import com.example.multipromptchat.domain.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import java.io.File

/** Events surfaced to the chat screen while a reply streams in. */
sealed interface ChatStreamEvent {
    data class Delta(val content: String, val reasoning: String) : ChatStreamEvent
    /** 增量 C-5：本次请求的 token 用量（已写入会话累计值）。 */
    data class Usage(val usage: TokenUsage) : ChatStreamEvent
    data object Completed : ChatStreamEvent
    data class Failed(val message: String) : ChatStreamEvent
}

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val promptDao: PromptDao,
    private val deepSeekClient: DeepSeekClient,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun appendUserMessage(
        conversationId: Long,
        text: String,
        imagePath: String? = null,
    ) {
        val now = System.currentTimeMillis()
        messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                role = MessageRole.USER.apiValue,
                content = text,
                reasoning = null,
                imagePath = imagePath,
                createdAt = now,
            )
        )
        conversationDao.touch(conversationId, now)
    }

    suspend fun saveAssistantMessage(conversationId: Long, content: String, reasoning: String?) {
        if (content.isBlank()) return
        val now = System.currentTimeMillis()
        messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT.apiValue,
                content = content,
                reasoning = reasoning?.takeIf { it.isNotBlank() },
                imagePath = null,
                createdAt = now,
            )
        )
        conversationDao.touch(conversationId, now)
    }

    /**
     * 增量 C-2：首条消息后自动给会话起个 ≤10 字的标题。
     *
     * 1.3 修复：
     * - 判定放宽：标题为默认值**或空串**都视为可生成（导入/迁移来的空标题也能补上）；
     * - 不再"AI 失败就什么都不做"：AI 拿不到结果时用首条消息做**本地兜底标题**，
     *   保证新建会话聊完第一轮一定能看到标题；
     * - 关闭开关时仍然完全不改标题。
     */
    suspend fun autoGenerateTitleIfNeeded(conversationId: Long) {
        if (!settingsRepository.chatExperience.value.autoGenerateTitle) return

        val conversation = conversationDao.getById(conversationId) ?: return
        val currentTitle = conversation.title.trim()
        val isGeneric = currentTitle.isEmpty() || currentTitle == DEFAULT_CONVERSATION_TITLE
        if (!isGeneric) return

        val first = messageDao.firstUserMessage(conversationId)?.trim().orEmpty()
        if (first.isEmpty()) return

        val fallback = TitleBuilder.localFallback(first)

        val apiKey = settingsRepository.getApiKey()
        val generated = if (apiKey.isNullOrBlank()) {
            null
        } else {
            runCatching {
                deepSeekClient.generateTitle(
                    apiKey = apiKey,
                    userMessage = first,
                    model = currentModel(),
                )
            }.getOrNull()
        }

        val finalTitle = generated ?: fallback ?: return
        conversationDao.rename(conversationId, finalTitle, System.currentTimeMillis())
    }

    /** 增量 C-5：把一次请求的 usage 累加到会话上。 */
    private suspend fun persistUsage(conversationId: Long, usage: TokenUsage) {
        conversationDao.addTokenUsage(
            id = conversationId,
            promptTokens = usage.promptTokens,
            completionTokens = usage.completionTokens,
            cacheHitTokens = usage.cacheHitTokens,
            cacheMissTokens = usage.cacheMissTokens,
        )
    }

    /**
     * 增量 A1：转发一次最小探活请求，供设置页「测试连接」使用。
     * 不改变原有发送/流式逻辑。
     */
    suspend fun testConnection(apiKey: String): KeyCheckResult = deepSeekClient.checkApiKey(apiKey)

    /** 1.2：拉取可用模型，供设置页选择。 */
    suspend fun fetchModels(apiKey: String): ModelListResult = deepSeekClient.listModels(apiKey)

    /** 1.2：当前生效的模型（设置里选的，兜底 DEFAULT_MODEL）。 */
    private fun currentModel(): String =
        settingsRepository.modelName.value.ifBlank { DEFAULT_MODEL }

    /**
     * Builds the request from the conversation's *current* prompt binding plus the
     * stored history, then streams the reply. Cancelling the collector cancels the
     * HTTP call (see DeepSeekClient.awaitClose).
     */
    fun generateReply(conversationId: Long): Flow<ChatStreamEvent> = flow {
        val conversation = conversationDao.getById(conversationId)
        if (conversation == null) {
            emit(ChatStreamEvent.Failed("会话不存在或已被删除"))
            return@flow
        }

        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isNullOrBlank()) {
            emit(ChatStreamEvent.Failed("请先在设置页填写 DeepSeek API Key"))
            return@flow
        }

        val template = promptDao.getById(conversation.promptId)
        // Fall back to the snapshot when the template disappeared, so the persona survives.
        val systemPrompt = template?.systemPrompt ?: conversation.promptSnapshot
        val temperature = template?.temperature ?: 0.7f
        val topP = template?.topP ?: 1f

        // 增量 C：这些设置只影响“后续请求”，不改动任何已存数据。
        val experience = settingsRepository.chatExperience.value
        val model = currentModel()

        val history = messageDao.getByConversation(conversationId)
            .map { it.toDomain() }
            .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
            .filter { it.content.isNotBlank() || it.imagePath != null }
            // 增量 C-3：只带最近 N 条历史；system 提示词下面单独加，永远保留。
            .takeLast(experience.contextMessageCount.coerceAtLeast(1))

        val apiMessages = buildList {
            if (systemPrompt.isNotBlank()) {
                add(ChatMessage(role = MessageRole.SYSTEM.apiValue, content = systemPrompt))
            }
            history.forEach { message ->
                // 1.2：带图消息按 OpenAI 兼容的 image_url 重新附上（转 data URL）。
                val imageDataUrl = message.imagePath
                    ?.let { path -> runCatching { ImageStore.toDataUrl(File(path)) }.getOrNull() }
                add(
                    ChatMessage(
                        role = message.role.apiValue,
                        content = message.content,
                        imageDataUrl = imageDataUrl,
                    )
                )
            }
        }

        if (!experience.streamingOutput) {
            // 增量 C-4：非流式，一次性把完整回复作为单个 Delta 发出。
            when (
                val result = deepSeekClient.complete(
                    apiKey = apiKey,
                    model = model,
                    messages = apiMessages,
                    temperature = temperature,
                    topP = topP,
                )
            ) {
                is CompletionResult.Success -> {
                    if (result.content.isNotBlank()) {
                        emit(ChatStreamEvent.Delta(content = result.content, reasoning = ""))
                    }
                    result.usage?.let { usage ->
                        persistUsage(conversationId, usage)
                        emit(ChatStreamEvent.Usage(usage))
                    }
                    emit(ChatStreamEvent.Completed)
                }
                is CompletionResult.Failure -> emit(ChatStreamEvent.Failed(result.message))
            }
            return@flow
        }

        // 第二道保险：把 Done 当作上游终点。
        // takeWhile 在遇到 Done 时会取消上游（从而触发 DeepSeekClient 的 awaitClose），
        // 这样即使底层通道因为任何原因没有关闭，collect 也一定会返回，
        // 上层 finally 里的“清除正在输入状态”必定执行。
        var sawError = false
        deepSeekClient.streamChat(
            apiKey = apiKey,
            model = model,
            messages = apiMessages,
            temperature = temperature,
            topP = topP,
        ).takeWhile { chunk -> chunk !is StreamChunk.Done }.collect { chunk ->
            when (chunk) {
                is StreamChunk.Content -> emit(
                    ChatStreamEvent.Delta(content = chunk.content, reasoning = chunk.reasoning)
                )
                is StreamChunk.Usage -> {
                    persistUsage(conversationId, chunk.usage)
                    emit(ChatStreamEvent.Usage(chunk.usage))
                }
                is StreamChunk.Error -> {
                    sawError = true
                    emit(ChatStreamEvent.Failed(chunk.message))
                }
                StreamChunk.Done -> Unit
            }
        }
        if (!sawError) emit(ChatStreamEvent.Completed)
    }

}
