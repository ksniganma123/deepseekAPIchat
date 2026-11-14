package com.example.multipromptchat.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.data.repository.ConversationRepository
import com.example.multipromptchat.data.repository.PromptRepository
import com.example.multipromptchat.domain.ConversationSummary
import com.example.multipromptchat.domain.PromptTemplate
import com.example.multipromptchat.ui.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationListUiState(
    val loading: Boolean = true,
    val summaries: List<ConversationSummary> = emptyList(),
    val prompts: List<PromptTemplate> = emptyList(),
    val message: String? = null,
)

class ConversationListViewModel(
    private val conversationRepository: ConversationRepository,
    private val promptRepository: PromptRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ConversationListUiState> = combine(
        conversationRepository.observeSummaries(),
        promptRepository.observePrompts(),
        message,
    ) { summaries, prompts, currentMessage ->
        ConversationListUiState(
            loading = false,
            summaries = summaries,
            prompts = prompts,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConversationListUiState(),
    )

    fun createConversation(prompt: PromptTemplate, title: String?, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            runCatching { conversationRepository.createConversation(prompt, title) }
                .onSuccess { conversationId -> onCreated(conversationId) }
                .onFailure { message.value = it.message ?: Strings.unknownError }
        }
    }

    fun renameConversation(id: Long, title: String) {
        viewModelScope.launch {
            runCatching { conversationRepository.renameConversation(id, title) }
                .onFailure { message.value = it.message ?: Strings.unknownError }
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            runCatching { conversationRepository.deleteConversation(id) }
                .onFailure { message.value = it.message ?: Strings.unknownError }
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                ConversationListViewModel(
                    conversationRepository = container.conversationRepository,
                    promptRepository = container.promptRepository,
                )
            }
        }
    }
}
