package com.example.multipromptchat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query(
        "SELECT * FROM messages WHERE conversation_id = :conversationId " +
            "ORDER BY created_at ASC, id ASC"
    )
    fun observeByConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query(
        "SELECT * FROM messages WHERE conversation_id = :conversationId " +
            "ORDER BY created_at ASC, id ASC"
    )
    suspend fun getByConversation(conversationId: Long): List<MessageEntity>

    @Query(
        "SELECT content FROM messages WHERE conversation_id = :conversationId AND role = 'user' " +
            "ORDER BY created_at ASC, id ASC LIMIT 1"
    )
    suspend fun firstUserMessage(conversationId: Long): String?

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("DELETE FROM messages WHERE conversation_id = :conversationId")
    suspend fun deleteByConversation(conversationId: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE conversation_id = :conversationId")
    suspend fun countByConversation(conversationId: Long): Int
}
