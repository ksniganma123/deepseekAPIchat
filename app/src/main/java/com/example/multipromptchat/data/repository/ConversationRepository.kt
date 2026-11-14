package com.example.multipromptchat.data.repository

import com.example.multipromptchat.data.local.ConversationDao
import com.example.multipromptchat.data.local.ConversationEntity
import com.example.multipromptchat.data.local.MessageDao
import com.example.multipromptchat.data.local.toDomain
import com.example.multipromptchat.domain.Conversation
import com.example.multipromptchat.domain.ConversationSummary
import com.example.multipromptchat.domain.DEFAULT_CONVERSATION_TITLE
import com.example.multipromptchat.domain.Message
import com.example.multipromptchat.domain.PromptTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
) {

    fun observeSummaries(): Flow<List<ConversationSummary>> =
        conversationDao.observeListItems().map { rows ->
            rows.map { row ->
                ConversationSummary(
                    conversation = row.conversation.toDomain(),
                    promptName = row.promptName,
                    lastMessage = row.lastMessage,
                    lastMessageAt = row.lastMessageAt,
                )
            }
        }

    fun observeConversation(id: Long): Flow<Conversation?> =
        conversationDao.observeById(id).map { it?.toDomain() }

    fun observeMessages(conversationId: Long): Flow<List<Message>> =
        messageDao.observeByConversation(conversationId).map { rows -> rows.map { it.toDomain() } }

    suspend fun getConversation(id: Long): Conversation? = conversationDao.getById(id)?.toDomain()

    suspend fun createConversation(prompt: PromptTemplate, title: String?): Long {
        val now = System.currentTimeMillis()
        val resolvedTitle = title?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_CONVERSATION_TITLE
        return conversationDao.insert(
            ConversationEntity(
                title = resolvedTitle,
                promptId = prompt.id,
                promptSnapshot = prompt.systemPrompt,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun renameConversation(id: Long, title: String) {
        val resolved = title.trim().ifEmpty { DEFAULT_CONVERSATION_TITLE }
        conversationDao.rename(id, resolved, System.currentTimeMillis())
    }

    suspend fun deleteConversation(id: Long) {
        conversationDao.deleteById(id)
    }

    /**
     * Rebinds the conversation to [prompt]. Only the system instruction of *later*
     * requests changes; history is dropped only when the caller asks for it.
     */
    suspend fun switchPrompt(
        conversationId: Long,
        prompt: PromptTemplate,
        clearHistory: Boolean,
    ) {
        conversationDao.updatePrompt(
            id = conversationId,
            promptId = prompt.id,
            snapshot = prompt.systemPrompt,
            now = System.currentTimeMillis(),
        )
        if (clearHistory) messageDao.deleteByConversation(conversationId)
    }
}
