package com.example.multipromptchat.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Conversation row joined with the bound prompt name and a one-message preview. */
data class ConversationListItem(
    @Embedded val conversation: ConversationEntity,
    val promptName: String?,
    val lastMessage: String?,
    val lastMessageAt: Long?,
)

@Dao
interface ConversationDao {

    @Query(
        """
        SELECT c.*,
            p.name AS promptName,
            (SELECT m.content FROM messages m WHERE m.conversation_id = c.id
                ORDER BY m.created_at DESC, m.id DESC LIMIT 1) AS lastMessage,
            (SELECT m.created_at FROM messages m WHERE m.conversation_id = c.id
                ORDER BY m.created_at DESC, m.id DESC LIMIT 1) AS lastMessageAt
        FROM conversations c
        LEFT JOIN prompt_templates p ON p.id = c.prompt_id
        ORDER BY c.updated_at DESC
        """
    )
    fun observeListItems(): Flow<List<ConversationListItem>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeById(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Query("SELECT COUNT(*) FROM conversations WHERE prompt_id = :promptId")
    suspend fun countByPrompt(promptId: Long): Int

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :title, updated_at = :now WHERE id = :id")
    suspend fun rename(id: Long, title: String, now: Long)

    @Query(
        """
        UPDATE conversations
        SET prompt_id = :promptId, prompt_snapshot = :snapshot, updated_at = :now
        WHERE id = :id
        """
    )
    suspend fun updatePrompt(id: Long, promptId: Long, snapshot: String, now: Long)

    @Query("UPDATE conversations SET updated_at = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)

    /** 增量 C-5：把一次请求的 usage 累加到会话上。 */
    @Query(
        """
        UPDATE conversations
        SET prompt_tokens = prompt_tokens + :promptTokens,
            completion_tokens = completion_tokens + :completionTokens,
            cache_hit_tokens = cache_hit_tokens + :cacheHitTokens,
            cache_miss_tokens = cache_miss_tokens + :cacheMissTokens
        WHERE id = :id
        """
    )
    suspend fun addTokenUsage(
        id: Long,
        promptTokens: Long,
        completionTokens: Long,
        cacheHitTokens: Long,
        cacheMissTokens: Long,
    )

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
