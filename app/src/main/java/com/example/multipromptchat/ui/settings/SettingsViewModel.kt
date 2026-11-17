package com.example.multipromptchat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.data.remote.KeyCheckResult
import com.example.multipromptchat.data.image.ImageStore
import com.example.multipromptchat.data.remote.ModelInfo
import com.example.multipromptchat.data.remote.ModelListResult
import com.example.multipromptchat.data.repository.AppearanceSettings
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

/** 1.2：模型列表拉取状态 */
sealed interface ModelFetchState {
    data object Idle : ModelFetchState
    data object Loading : ModelFetchState
    data class Loaded(val count: Int) : ModelFetchState
    data class Failed(val message: String) : ModelFetchState
}

private data class ModelUiState(
    val models: List<ModelInfo> = emptyList(),
    val state: ModelFetchState = ModelFetchState.Idle,
)

data class SettingsUiState(
    val maskedKey: String = "",
    val hasKey: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val message: String? = null,
    val testState: ConnectionTestState = ConnectionTestState.Idle,
    /** 增量 C：聊天体验设置快照。 */
    val chatExperience: ChatExperienceSettings = ChatExperienceSettings(),
    /** 1.2：外观设置（背景图 + 主色调）。 */
    val appearance: AppearanceSettings = AppearanceSettings(),
    /** 1.2：当前模型与可用模型列表。 */
    val modelName: String = "",
    val models: List<ModelInfo> = emptyList(),
    val modelState: ModelFetchState = ModelFetchState.Idle,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    private val testState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)

    private val modelUi = MutableStateFlow(ModelUiState())

    init {
        // Make sure the masked value is populated even on a cold start.
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.refreshApiKey() }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsRepository.apiKey,
            settingsRepository.themeMode,
            message,
        ) { apiKey, themeMode, currentMessage -> Triple(apiKey, themeMode, currentMessage) },
        combine(
            settingsRepository.chatExperience,
            settingsRepository.appearance,
            settingsRepository.modelName,
        ) { experience, appearance, modelName -> Triple(experience, appearance, modelName) },
        combine(testState, modelUi) { test, model -> test to model },
    ) { (apiKey, themeMode, currentMessage), (experience, appearance, modelName), (currentTest, model) ->
        SettingsUiState(
            maskedKey = settingsRepository.maskApiKey(apiKey),
            hasKey = !apiKey.isNullOrBlank(),
            themeMode = themeMode,
            message = currentMessage,
            testState = currentTest,
            chatExperience = experience,
            appearance = appearance,
            modelName = modelName,
            models = model.models,
            modelState = model.state,
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

    // ---- 1.2：模型 ---------------------------------------------------------

    /** 用当前密钥拉一次 /models；若当前模型不在列表里，自动选中第一个。 */
    fun fetchModels() {
        val key = settingsRepository.getApiKey()
        if (key.isNullOrBlank()) {
            modelUi.value = modelUi.value.copy(state = ModelFetchState.Failed(Strings.modelNeedKey))
            return
        }
        if (modelUi.value.state == ModelFetchState.Loading) return
        modelUi.value = modelUi.value.copy(state = ModelFetchState.Loading)
        viewModelScope.launch {
            val result = runCatching { chatRepository.fetchModels(key) }
                .getOrElse { ModelListResult.Failure(Strings.unknownError) }
            when (result) {
                is ModelListResult.Success -> {
                    val current = settingsRepository.modelName.value
                    if (result.models.none { it.id == current }) {
                        settingsRepository.setModelName(result.models.first().id)
                    }
                    modelUi.value = ModelUiState(
                        models = result.models,
                        state = ModelFetchState.Loaded(result.models.size),
                    )
                }
                is ModelListResult.Failure -> {
                    modelUi.value = modelUi.value.copy(
                        state = ModelFetchState.Failed(result.message),
                    )
                }
            }
        }
    }

    fun selectModel(id: String) {
        settingsRepository.setModelName(id)
    }

    // ---- 1.2：外观 ---------------------------------------------------------

    fun setBackgroundPath(path: String?) {
        settingsRepository.setBackgroundPath(path)
    }

    fun setAccentColor(argb: Long) {
        settingsRepository.setAccentColor(argb)
    }

    /** 1.5：背景图透明度（显示强度），拖动即时生效 */
    fun setBackgroundOpacity(value: Float) {
        settingsRepository.setBackgroundOpacity(value)
    }

    /** 1.2：把图片导入失败的原因翻译成人话显示在设置页顶部。 */
    fun reportImageError(failure: ImageStore.Failure) {
        message.value = when (failure) {
            ImageStore.Failure.TOO_LARGE -> Strings.imageTooLarge(8)
            ImageStore.Failure.UNSUPPORTED_FORMAT -> Strings.imageUnsupported()
            ImageStore.Failure.DECODE_FAILED -> Strings.imageDecodeFailed()
            ImageStore.Failure.IO_ERROR -> Strings.imageReadFailed()
        }
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
