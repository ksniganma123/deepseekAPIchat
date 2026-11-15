package com.example.multipromptchat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.data.repository.ChatRepository
import com.example.multipromptchat.data.repository.ChatStreamEvent
import com.example.multipromptchat.data.repository.ConversationRepository
import com.example.multipromptchat.data.repository.PromptRepository
import com.example.multipromptchat.data.repository.SettingsRepository
import com.example.multipromptchat.domain.Conversation
import com.example.multipromptchat.domain.Message
import com.example.multipromptchat.domain.PromptTemplate
import com.example.multipromptchat.ui.Strings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatUiState(
    val loading: Boolean = true,
    val conversation: Conversation? = null,
    val activePrompt: PromptTemplate? = null,
    val prompts: List<PromptTemplate> = emptyList(),
    val messages: List<Message> = emptyList(),
    val streamingText: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
)

private data class ChatCoreState(
    val conversation: Conversation?,
    val messages: List<Message>,
)

private data class ChatTransientState(
    val streamingText: String,
    val isStreaming: Boolean,
    val error: String?,
)

class ChatViewModel(
    private val conversationId: Long,
    private val chatRepository: ChatRepository,
    private val conversationRepository: ConversationRepository,
    private val promptRepository: PromptRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val streamingText = MutableStateFlow("")
    private val isStreaming = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private var streamJob: Job? = null

    val uiState: StateFlow<ChatUiState> = combine(
        combine(
            conversationRepository.observeConversation(conversationId),
            conversationRepository.observeMessages(conversationId),
        ) { conversation, messages -> ChatCoreState(conversation, messages) },
        combine(
            streamingText,
            isStreaming,
            errorMessage,
        ) { text, streaming, error -> ChatTransientState(text, streaming, error) },
        promptRepository.observePrompts(),
    ) { core, transient, prompts ->
        ChatUiState(
            loading = core.conversation == null,
            conversation = core.conversation,
            activePrompt = prompts.firstOrNull { it.id == core.conversation?.promptId },
            prompts = prompts,
            messages = core.messages,
            streamingText = transient.streamingText,
            isStreaming = transient.isStreaming,
            error = transient.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState(),
    )

    fun sendMessage(raw: String, imagePath: String? = null) {
        val text = raw.trim()
        // 1.2：允许「只发图片不发文字」。
        if ((text.isEmpty() && imagePath == null) || isStreaming.value) return
        if (settingsRepository.getApiKey().isNullOrBlank()) {
            errorMessage.value = Strings.apiKeyMissing
            return
        }

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            isStreaming.value = true
            errorMessage.value = null
            streamingText.value = ""

            val content = StringBuilder()
            val reasoning = StringBuilder()

            try {
                chatRepository.appendUserMessage(conversationId, text, imagePath)
                // 增量 C-2：标题生成单独跑，不阻塞回复；失败自己吞掉。
                viewModelScope.launch {
                    runCatching { chatRepository.autoGenerateTitleIfNeeded(conversationId) }
                }
                chatRepository.generateReply(conversationId).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Delta -> {
                            if (event.content.isNotEmpty()) {
                                content.append(event.content)
                                streamingText.value = content.toString()
                            }
                            if (event.reasoning.isNotEmpty()) {
                                reasoning.append(event.reasoning)
                            }
                        }
                        is ChatStreamEvent.Failed -> errorMessage.value = event.message
                        // token 统计已由 Repository 落库，界面通过会话数据自动刷新。
                        is ChatStreamEvent.Usage -> Unit
                        ChatStreamEvent.Completed -> Unit
                    }
                }
            } catch (cancelled: CancellationException) {
                // Stop button: keep whatever has already streamed in.
            } catch (t: Throwable) {
                errorMessage.value = t.message ?: Strings.unknownError
            } finally {
                try {
                    // Persist the partial answer even when the user stopped generation,
                    // so what is on screen is also what is stored.
                    val partial = content.toString()
                    val partialReasoning = reasoning.toString().takeIf { it.isNotBlank() }
                    withContext(NonCancellable) {
                        runCatching {
                            chatRepository.saveAssistantMessage(
                                conversationId,
                                partial,
                                partialReasoning,
                            )
                        }
                    }
                } finally {
                    // 无论落库成功与否，「正在输入」状态都必须被清掉：
                    // 右上角转圈、消息气泡里的闪烁光标、发送键与停止键都看这个值。
                    streamingText.value = ""
                    isStreaming.value = false
                }
            }
        }
    }

    fun stopGeneration() {
        streamJob?.cancel()
        streamJob = null
    }

    fun switchPrompt(prompt: PromptTemplate, clearHistory: Boolean) {
        viewModelScope.launch {
            runCatching {
                conversationRepository.switchPrompt(conversationId, prompt, clearHistory)
            }.onFailure { errorMessage.value = it.message ?: Strings.unknownError }
        }
    }

    fun dismissError() {
        errorMessage.value = null
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(container: AppContainer, conversationId: Long) = viewModelFactory {
            initializer {
                ChatViewModel(
                    conversationId = conversationId,
                    chatRepository = container.chatRepository,
                    conversationRepository = container.conversationRepository,
                    promptRepository = container.promptRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
