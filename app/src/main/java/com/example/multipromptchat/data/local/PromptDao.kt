package com.example.multipromptchat.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Prompt template plus how many conversations currently bind it. */
data class PromptWithUsage(
    @Embedded val prompt: PromptTemplateEntity,
    val usageCount: Int,
)

@Dao
interface PromptDao {

    @Query("SELECT * FROM prompt_templates ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<PromptTemplateEntity>>

    @Query(
        """
        SELECT p.*, (SELECT COUNT(*) FROM conversations c WHERE c.prompt_id = p.id) AS usageCount
        FROM prompt_templates p
        ORDER BY p.updated_at DESC
        """
    )
    fun observeAllWithUsage(): Flow<List<PromptWithUsage>>

    @Query("SELECT * FROM prompt_templates WHERE id = :id")
    fun observeById(id: Long): Flow<PromptTemplateEntity?>

    @Query("SELECT * FROM prompt_templates WHERE id = :id")
    suspend fun getById(id: Long): PromptTemplateEntity?

    @Query("SELECT * FROM prompt_templates ORDER BY created_at ASC")
    suspend fun getAllOnce(): List<PromptTemplateEntity>

    @Query("SELECT COUNT(*) FROM prompt_templates")
    suspend fun count(): Int

    @Insert
    suspend fun insert(prompt: PromptTemplateEntity): Long

    @Update
    suspend fun update(prompt: PromptTemplateEntity)

    /** Field-level update so `created_at` is never overwritten by an edit. */
    @Query(
        """
        UPDATE prompt_templates
        SET name = :name,
            system_prompt = :systemPrompt,
            temperature = :temperature,
            top_p = :topP,
            reset_context_on_switch = :resetContextOnSwitch,
            updated_at = :now
        WHERE id = :id
        """
    )
    suspend fun updateFields(
        id: Long,
        name: String,
        systemPrompt: String,
        temperature: Float,
        topP: Float,
        resetContextOnSwitch: Boolean,
        now: Long,
    )

    @Delete
    suspend fun delete(prompt: PromptTemplateEntity)

    @Query("DELETE FROM prompt_templates WHERE id = :id")
    suspend fun deleteById(id: Long)
}
