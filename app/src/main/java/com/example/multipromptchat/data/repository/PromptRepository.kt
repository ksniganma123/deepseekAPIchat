package com.example.multipromptchat.data.repository

import com.example.multipromptchat.data.local.ConversationDao
import com.example.multipromptchat.data.local.PromptDao
import com.example.multipromptchat.data.local.PromptTemplateEntity
import com.example.multipromptchat.data.local.toDomain
import com.example.multipromptchat.data.local.toEntity
import com.example.multipromptchat.domain.PromptTemplate
import com.example.multipromptchat.domain.PromptUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface PromptDeleteResult {
    data object Deleted : PromptDeleteResult
    data class InUse(val conversationCount: Int) : PromptDeleteResult
}

/** 增量 E：批量删除的聚合结果。 */
data class BatchDeleteResult(
    val deleted: Int = 0,
    val blocked: Int = 0,
    val failed: Int = 0,
) {
    val total: Int get() = deleted + blocked + failed
}

class PromptRepository(
    private val promptDao: PromptDao,
    private val conversationDao: ConversationDao,
) {

    fun observePrompts(): Flow<List<PromptTemplate>> =
        promptDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observePromptsWithUsage(): Flow<List<PromptUsage>> =
        promptDao.observeAllWithUsage().map { rows ->
            rows.map { PromptUsage(prompt = it.prompt.toDomain(), conversationCount = it.usageCount) }
        }

    fun observePrompt(id: Long): Flow<PromptTemplate?> =
        promptDao.observeById(id).map { it?.toDomain() }

    suspend fun getPrompt(id: Long): PromptTemplate? = promptDao.getById(id)?.toDomain()

    suspend fun createPrompt(
        name: String,
        systemPrompt: String,
        temperature: Float,
        topP: Float,
        resetContextOnSwitch: Boolean,
    ): Long {
        val now = System.currentTimeMillis()
        return promptDao.insert(
            PromptTemplateEntity(
                name = name.trim(),
                systemPrompt = systemPrompt.trim(),
                temperature = temperature,
                topP = topP,
                resetContextOnSwitch = resetContextOnSwitch,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun updatePrompt(
        id: Long,
        name: String,
        systemPrompt: String,
        temperature: Float,
        topP: Float,
        resetContextOnSwitch: Boolean,
    ) {
        promptDao.updateFields(
            id = id,
            name = name.trim(),
            systemPrompt = systemPrompt.trim(),
            temperature = temperature,
            topP = topP,
            resetContextOnSwitch = resetContextOnSwitch,
            now = System.currentTimeMillis(),
        )
    }

    suspend fun deletePrompt(id: Long): PromptDeleteResult {
        val used = conversationDao.countByPrompt(id)
        if (used > 0) return PromptDeleteResult.InUse(used)
        promptDao.deleteById(id)
        return PromptDeleteResult.Deleted
    }

    /**
     * 增量 E：批量删除。逐条复用 [deletePrompt]，所以「被会话引用则跳过」的保护
     * 对批量删除同样生效；单条异常不会中断整批。
     */
    suspend fun deletePrompts(ids: List<Long>): BatchDeleteResult {
        if (ids.isEmpty()) return BatchDeleteResult()
        var deleted = 0
        var blocked = 0
        var failed = 0
        ids.forEach { id ->
            when (runCatching { deletePrompt(id) }.getOrElse { null }) {
                PromptDeleteResult.Deleted -> deleted++
                is PromptDeleteResult.InUse -> blocked++
                null -> failed++
            }
        }
        return BatchDeleteResult(deleted = deleted, blocked = blocked, failed = failed)
    }

    suspend fun countPrompts(): Int = promptDao.count()

    /** Seeds a few useful personas on first launch so the app is usable immediately. */
    suspend fun ensureDefaultPrompts() {
        if (promptDao.count() > 0) return
        val now = System.currentTimeMillis()
        DEFAULT_PROMPTS.forEach { seed ->
            promptDao.insert(
                PromptTemplateEntity(
                    name = seed.name,
                    systemPrompt = seed.systemPrompt,
                    temperature = seed.temperature,
                    topP = seed.topP,
                    resetContextOnSwitch = seed.resetContextOnSwitch,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    private data class SeedPrompt(
        val name: String,
        val systemPrompt: String,
        val temperature: Float,
        val topP: Float,
        val resetContextOnSwitch: Boolean,
    )

    private companion object {
        val DEFAULT_PROMPTS = listOf(
            SeedPrompt(
                name = "通用助手",
                systemPrompt = "你是一个中文 AI 助手。回答简洁、准确，优先给结论，必要时再给步骤。",
                temperature = 0.7f,
                topP = 1f,
                resetContextOnSwitch = true,
            ),
            SeedPrompt(
                name = "代码搭档",
                systemPrompt = "你是一名资深 Kotlin / Android 工程师。先给思路，再给可直接运行的代码，" +
                    "最后指出潜在坑与优化点。代码使用 4 空格缩进。",
                temperature = 0.3f,
                topP = 0.9f,
                resetContextOnSwitch = true,
            ),
            SeedPrompt(
                name = "翻译润色",
                systemPrompt = "你是专业中英翻译。只输出译文，保留原文的段落与格式，不添加任何解释或前言。",
                temperature = 0.2f,
                topP = 0.9f,
                resetContextOnSwitch = false,
            ),
        )
    }
}
