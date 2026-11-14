package com.example.multipromptchat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.data.remote.KeyCheckResult
import com.example.multipromptchat.data.repository.ChatExperienceSettings
import com.example.multipromptchat.data.repository.ChatRepository
import com.example.multipromptchat.data.repository.SettingsRepository
import com.example.multipromptchat.data.repository.ThemeMode
import com.example.multipromptchat.ui.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

/** 三态：未测试 / 测试中 / 结果（成功或失败）。 */
sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data object Success : ConnectionTestState
    data class Failed(val message: String) : ConnectionTestState
}

data class SettingsUiState(
    val maskedKey: String = "",
    val hasKey: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val message: String? = null,
    val testState: ConnectionTestState = ConnectionTestState.Idle,
    /** 增量 C：聊天体验设置快照。 */
    val chatExperience: ChatExperienceSettings = ChatExperienceSettings(),
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    private val testState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)

    init {
        // Make sure the masked value is populated even on a cold start.
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.refreshApiKey() }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.apiKey,
        settingsRepository.themeMode,
        message,
        testState,
        settingsRepository.chatExperience,
    ) { apiKey, themeMode, currentMessage, currentTest, experience ->
        SettingsUiState(
            maskedKey = settingsRepository.maskApiKey(apiKey),
            hasKey = !apiKey.isNullOrBlank(),
            themeMode = themeMode,
            message = currentMessage,
            testState = currentTest,
            chatExperience = experience,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun saveApiKey(raw: String) {
        if (raw.isBlank()) {
            message.value = Strings.apiKeyEmpty
            return
        }
        settingsRepository.setApiKey(raw)
        message.value = Strings.apiKeySaved
        // 新密钥还没验证过，把上一次的测试结果清掉。
        testState.value = ConnectionTestState.Idle
    }

    fun clearApiKey() {
        settingsRepository.clearApiKey()
        message.value = Strings.apiKeyCleared
        testState.value = ConnectionTestState.Idle
    }

    /** 增量 A1：用当前密钥发一次最小请求，结果落到三态。 */
    fun testConnection() {
        val key = settingsRepository.getApiKey()
        if (key.isNullOrBlank()) {
            testState.value = ConnectionTestState.Failed(Strings.apiKeyMissing)
            return
        }
        testState.value = ConnectionTestState.Testing
        viewModelScope.launch {
            val result = runCatching { chatRepository.testConnection(key) }
                .getOrElse { KeyCheckResult.NetworkError }
            testState.value = when (result) {
                KeyCheckResult.Success -> ConnectionTestState.Success
                KeyCheckResult.InvalidKey -> ConnectionTestState.Failed(Strings.apiKeyInvalid)
                KeyCheckResult.NoBalance -> ConnectionTestState.Failed(Strings.apiKeyNoBalance)
                KeyCheckResult.RateLimited -> ConnectionTestState.Failed(Strings.apiKeyRateLimited)
                KeyCheckResult.NetworkError -> ConnectionTestState.Failed(Strings.networkUnavailable)
                is KeyCheckResult.ServerError -> ConnectionTestState.Failed(
                    Strings.serverUnavailable(result.code)
                )
                is KeyCheckResult.Failed -> ConnectionTestState.Failed(result.message)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    // ---- 增量 C：聊天体验 --------------------------------------------------

    fun setSendOnEnter(sendOnEnter: Boolean) {
        settingsRepository.updateChatExperience { it.copy(sendOnEnter = sendOnEnter) }
    }

    fun setAutoGenerateTitle(enabled: Boolean) {
        settingsRepository.updateChatExperience { it.copy(autoGenerateTitle = enabled) }
    }

    fun setContextMessageCount(count: Int) {
        settingsRepository.updateChatExperience { it.copy(contextMessageCount = count) }
    }

    fun setStreamingOutput(enabled: Boolean) {
        settingsRepository.updateChatExperience { it.copy(streamingOutput = enabled) }
    }

    fun setShowTokenUsage(enabled: Boolean) {
        settingsRepository.updateChatExperience { it.copy(showTokenUsage = enabled) }
    }

    fun dismissMessage() {
        message.value = null
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    chatRepository = container.chatRepository,
                )
            }
        }
    }
}
