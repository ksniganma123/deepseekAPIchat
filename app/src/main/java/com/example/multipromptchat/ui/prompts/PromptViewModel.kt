package com.example.multipromptchat.ui.prompts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.data.repository.BatchDeleteResult
import com.example.multipromptchat.data.repository.PromptDeleteResult
import com.example.multipromptchat.data.repository.PromptRepository
import com.example.multipromptchat.domain.PromptTemplate
import com.example.multipromptchat.domain.PromptUsage
import com.example.multipromptchat.ui.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 增量 E：多选状态。选中集合只在选择模式下有意义。 */
data class PromptSelection(
    val active: Boolean = false,
    val ids: Set<Long> = emptySet(),
)

data class PromptListUiState(
    val loading: Boolean = true,
    val prompts: List<PromptUsage> = emptyList(),
    val message: String? = null,
    val selection: PromptSelection = PromptSelection(),
    val deleting: Boolean = false,
) {
    val selectedCount: Int get() = selection.ids.size

    /** 以当前列表为准判断是否已全选，避免残留的失效 id 干扰。 */
    val allSelected: Boolean
        get() = prompts.isNotEmpty() && prompts.all { it.prompt.id in selection.ids }

    val canDeleteSelected: Boolean
        get() = selectedCount > 0 && !deleting
}

class PromptListViewModel(
    private val promptRepository: PromptRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)
    private val selection = MutableStateFlow(PromptSelection())
    private val deleting = MutableStateFlow(false)

    val uiState: StateFlow<PromptListUiState> = combine(
        promptRepository.observePromptsWithUsage(),
        message,
        selection,
        deleting,
    ) { prompts, currentMessage, currentSelection, isDeleting ->
        PromptListUiState(
            loading = false,
            prompts = prompts,
            message = currentMessage,
            selection = currentSelection,
            deleting = isDeleting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PromptListUiState(),
    )

    // ---- 增量 E：多选与批量删除 -----------------------------------------

    fun enterSelectionMode() {
        selection.value = PromptSelection(active = true, ids = emptySet())
    }

    fun exitSelectionMode() {
        selection.value = PromptSelection()
    }

    fun toggleSelected(id: Long) {
        val current = selection.value
        if (!current.active) return
        val nextIds = if (id in current.ids) current.ids - id else current.ids + id
        selection.value = current.copy(ids = nextIds)
    }

    fun selectAll() {
        val ids = uiState.value.prompts.map { it.prompt.id }.toSet()
        selection.value = selection.value.copy(ids = ids)
    }

    fun clearSelection() {
        selection.value = selection.value.copy(ids = emptySet())
    }

    fun toggleSelectAll() {
        if (uiState.value.allSelected) clearSelection() else selectAll()
    }

    /** 一次删除全部选中项；结束后一定清空选中并退出选择模式。 */
    fun deleteSelected() {
        val current = uiState.value
        // 只删当前列表里还存在的条目，避免列表被别处改动后残留的 id。
        val visibleIds = current.prompts.map { it.prompt.id }.toSet()
        val ids = current.selection.ids.filter { it in visibleIds }
        if (ids.isEmpty() || current.deleting) return

        viewModelScope.launch {
            deleting.value = true
            val result = runCatching { promptRepository.deletePrompts(ids) }
                .getOrElse { BatchDeleteResult(failed = ids.size) }
            deleting.value = false

            message.value = when {
                result.deleted == 0 -> Strings.promptsNotDeleted(result.blocked, result.failed)
                result.blocked == 0 && result.failed == 0 -> Strings.promptsDeleted(result.deleted)
                else -> Strings.promptsDeletedPartial(
                    deleted = result.deleted,
                    blocked = result.blocked,
                    failed = result.failed,
                )
            }
            // 清空选中并退出选择模式：列表随之刷新，计数由 prompts.size 自动更新。
            selection.value = PromptSelection()
        }
    }

    // ---- 单条删除（原有路径，保留） -------------------------------------

    fun deletePrompt(prompt: PromptTemplate) {
        viewModelScope.launch {
            runCatching { promptRepository.deletePrompt(prompt.id) }
                .onSuccess { result ->
                    when (result) {
                        is PromptDeleteResult.InUse -> {
                            message.value = Strings.deletePromptInUse(result.conversationCount)
                        }
                        PromptDeleteResult.Deleted -> {
                            message.value = Strings.promptsDeleted(1)
                            // 若该条目恰好也在选中集合里，顺手移除，避免残留。
                            selection.value = selection.value.copy(
                                ids = selection.value.ids - prompt.id,
                            )
                        }
                    }
                }
                .onFailure { message.value = it.message ?: Strings.unknownError }
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { PromptListViewModel(container.promptRepository) }
        }
    }
}

data class PromptEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val topP: Float = 1f,
    val resetContextOnSwitch: Boolean = true,
    val error: String? = null,
)

class PromptEditViewModel(
    private val promptRepository: PromptRepository,
    private val promptId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PromptEditUiState(
            loading = promptId != null,
            isNew = promptId == null,
        )
    )
    val uiState: StateFlow<PromptEditUiState> = _uiState

    init {
        if (promptId != null) {
            viewModelScope.launch {
                val prompt = runCatching { promptRepository.getPrompt(promptId) }.getOrNull()
                _uiState.value = if (prompt == null) {
                    _uiState.value.copy(loading = false, error = Strings.unknownError)
                } else {
                    _uiState.value.copy(
                        loading = false,
                        name = prompt.name,
                        systemPrompt = prompt.systemPrompt,
                        temperature = prompt.temperature,
                        topP = prompt.topP,
                        resetContextOnSwitch = prompt.resetContextOnSwitch,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onSystemPromptChange(value: String) {
        _uiState.value = _uiState.value.copy(systemPrompt = value)
    }

    fun onTemperatureChange(value: Float) {
        _uiState.value = _uiState.value.copy(temperature = value)
    }

    fun onTopPChange(value: Float) {
        _uiState.value = _uiState.value.copy(topP = value)
    }

    fun onResetContextChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(resetContextOnSwitch = value)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun save(onSaved: () -> Unit) {
        val current = _uiState.value
        if (current.name.isBlank()) {
            _uiState.value = current.copy(error = Strings.nameRequired)
            return
        }
        if (current.systemPrompt.isBlank()) {
            _uiState.value = current.copy(error = Strings.systemPromptRequired)
            return
        }

        val editingId = promptId
        viewModelScope.launch {
            runCatching {
                if (editingId == null) {
                    promptRepository.createPrompt(
                        name = current.name,
                        systemPrompt = current.systemPrompt,
                        temperature = current.temperature,
                        topP = current.topP,
                        resetContextOnSwitch = current.resetContextOnSwitch,
                    )
                } else {
                    promptRepository.updatePrompt(
                        id = editingId,
                        name = current.name,
                        systemPrompt = current.systemPrompt,
                        temperature = current.temperature,
                        topP = current.topP,
                        resetContextOnSwitch = current.resetContextOnSwitch,
                    )
                }
            }.onSuccess {
                onSaved()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message ?: Strings.unknownError)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, promptId: Long?) = viewModelFactory {
            initializer { PromptEditViewModel(container.promptRepository, promptId) }
        }
    }
}
