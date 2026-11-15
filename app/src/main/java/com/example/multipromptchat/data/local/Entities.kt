package com.example.multipromptchat.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "prompt_templates")
data class PromptTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "system_prompt")
    val systemPrompt: String,
    @ColumnInfo(name = "temperature")
    val temperature: Float,
    @ColumnInfo(name = "top_p")
    val topP: Float,
    @ColumnInfo(name = "reset_context_on_switch")
    val resetContextOnSwitch: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

/**
 * A conversation always points at a prompt template, and keeps a snapshot of the
 * system prompt *at bind time* so history stays explainable even if the template
 * is later edited. RESTRICT keeps a referenced template undeletable.
 */
@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = PromptTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["prompt_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["prompt_id"])],
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "prompt_id")
    val promptId: Long,
    @ColumnInfo(name = "prompt_snapshot")
    val promptSnapshot: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    // 增量 C-5：本会话累计 token 用量（用于在聊天页显示消耗与缓存命中率）。
    @ColumnInfo(name = "prompt_tokens", defaultValue = "0")
    val promptTokens: Long = 0L,
    @ColumnInfo(name = "completion_tokens", defaultValue = "0")
    val completionTokens: Long = 0L,
    @ColumnInfo(name = "cache_hit_tokens", defaultValue = "0")
    val cacheHitTokens: Long = 0L,
    @ColumnInfo(name = "cache_miss_tokens", defaultValue = "0")
    val cacheMissTokens: Long = 0L,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["conversation_id"])],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "conversation_id")
    val conversationId: Long,
    /** One of "user" / "assistant" / "system" (see MessageRole.apiValue). */
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "content")
    val content: String,
    /** Raw `reasoning_content` of the reply, if the model sent one. Not rendered in v1. */
    @ColumnInfo(name = "reasoning")
    val reasoning: String? = null,
    /** 1.2：用户消息附带的图片（落盘绝对路径），助手消息为 null。 */
    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
