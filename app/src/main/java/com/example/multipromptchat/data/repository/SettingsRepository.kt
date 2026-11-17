package com.example.multipromptchat.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.multipromptchat.domain.DEFAULT_MODEL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

const val CONTEXT_MESSAGES_MIN = 5
const val CONTEXT_MESSAGES_MAX = 50
const val CONTEXT_MESSAGES_DEFAULT = 20

/** 默认主色调（与 ui/theme/Color.kt 的 LightAccent 一致） */
const val DEFAULT_ACCENT_ARGB = 0xFF3A7AFEL

const val BACKGROUND_OPACITY_MIN = 0.05f
const val BACKGROUND_OPACITY_MAX = 0.95f
const val DEFAULT_BACKGROUND_OPACITY = 0.18f

/**
 * 1.2 外观设置：自定义背景图 + 自定义主色调。两者都有默认值兜底，
 * 背景图为空 / 文件丢失时退回纯色背景。
 */
data class AppearanceSettings(
    /** 背景图落盘绝对路径；null = 不使用图片背景。 */
    val backgroundPath: String? = null,
    /** 主色调种子色（ARGB）。 */
    val accentArgb: Long = DEFAULT_ACCENT_ARGB,
    /**
     * 背景图的显示强度 0.05~0.95（越大图片越明显，越小越接近纯色）。
     * 渲染时用 (1 - 该值) 作为蒙层透明度，默认 0.18 与 1.2/1.3 的观感一致。
     */
    val backgroundOpacity: Float = DEFAULT_BACKGROUND_OPACITY,
) {
    val hasBackground: Boolean get() = !backgroundPath.isNullOrBlank()
}

/**
 * 增量 C：设置页「聊天体验」分组。整体作为一个不可变快照，改动后立即被
 * ChatRepository / ChatScreen 读到，作用于后续请求。
 */
data class ChatExperienceSettings(
    /** true = 回车发送；false = 回车换行、Ctrl+Enter 发送。 */
    val sendOnEnter: Boolean = true,
    /** 首条消息后是否用一次极短请求生成 10 字以内的会话标题。 */
    val autoGenerateTitle: Boolean = true,
    /** 仅发送最近 N 条历史消息（system 提示词始终保留）。 */
    val contextMessageCount: Int = CONTEXT_MESSAGES_DEFAULT,
    /** false = 走非流式请求，一次性显示完整回复。 */
    val streamingOutput: Boolean = true,
    /** 是否在输入框下方显示本会话 token 与缓存命中率。 */
    val showTokenUsage: Boolean = false,
)

/**
 * API key lives in [EncryptedSharedPreferences] (AES-256 Keystore-backed).
 * The UI theme preference is not secret, so it uses a plain preference file.
 */
class SettingsRepository(context: Context) {

    private val appContext: Context = context.applicationContext

    private val securePreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val plainPreferences: SharedPreferences by lazy {
        appContext.getSharedPreferences(PLAIN_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _apiKey = MutableStateFlow<String?>(null)
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    /** 密钥是否已从加密存储里读过一次（避免启动瞬间把新手引导误当「没配密钥」）。 */
    private val _apiKeyLoaded = MutableStateFlow(false)
    val apiKeyLoaded: StateFlow<Boolean> = _apiKeyLoaded.asStateFlow()

    /** 新手全屏引导是否已完成（完成或跳过都算）。 */
    private val _onboardingDone = MutableStateFlow(false)
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    private val _chatExperience = MutableStateFlow(ChatExperienceSettings())
    val chatExperience: StateFlow<ChatExperienceSettings> = _chatExperience.asStateFlow()

    /** 1.2：当前选中的模型（设置页拉取 /models 后选择，默认 DEFAULT_MODEL）。 */
    private val _modelName = MutableStateFlow(DEFAULT_MODEL)
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    /** 1.2：外观（背景图 + 主色调），改动即时生效。 */
    private val _appearance = MutableStateFlow(AppearanceSettings())
    val appearance: StateFlow<AppearanceSettings> = _appearance.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // 增量 A2 / B1：引导卡的折叠状态（默认展开 = false），沿用本类已有的普通 SharedPreferences。
    private val _apiGuideCollapsed = MutableStateFlow(false)
    val apiGuideCollapsed: StateFlow<Boolean> = _apiGuideCollapsed.asStateFlow()

    private val _promptGuideCollapsed = MutableStateFlow(false)
    val promptGuideCollapsed: StateFlow<Boolean> = _promptGuideCollapsed.asStateFlow()

    init {
        // Only the (non-secret) theme is touched eagerly: reading the encrypted store
        // costs a Keystore round trip, so the key is warmed up in the background instead
        // of on the first frame.
        val stored = runCatching { plainPreferences.getString(KEY_THEME_MODE, null) }.getOrNull()
        _themeMode.value = ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
        _apiGuideCollapsed.value =
            runCatching { plainPreferences.getBoolean(KEY_API_GUIDE_COLLAPSED, false) }.getOrDefault(false)
        _promptGuideCollapsed.value =
            runCatching { plainPreferences.getBoolean(KEY_PROMPT_GUIDE_COLLAPSED, false) }.getOrDefault(false)
        _onboardingDone.value =
            runCatching { plainPreferences.getBoolean(KEY_ONBOARDING_DONE, false) }.getOrDefault(false)

        val defaults = ChatExperienceSettings()
        _chatExperience.value = ChatExperienceSettings(
            sendOnEnter = runCatching {
                plainPreferences.getBoolean(KEY_SEND_ON_ENTER, defaults.sendOnEnter)
            }.getOrDefault(defaults.sendOnEnter),
            autoGenerateTitle = runCatching {
                plainPreferences.getBoolean(KEY_AUTO_TITLE, defaults.autoGenerateTitle)
            }.getOrDefault(defaults.autoGenerateTitle),
            contextMessageCount = runCatching {
                plainPreferences.getInt(KEY_CONTEXT_MESSAGES, defaults.contextMessageCount)
            }.getOrDefault(defaults.contextMessageCount)
                .coerceIn(CONTEXT_MESSAGES_MIN, CONTEXT_MESSAGES_MAX),
            streamingOutput = runCatching {
                plainPreferences.getBoolean(KEY_STREAMING, defaults.streamingOutput)
            }.getOrDefault(defaults.streamingOutput),
            showTokenUsage = runCatching {
                plainPreferences.getBoolean(KEY_SHOW_TOKEN_USAGE, defaults.showTokenUsage)
            }.getOrDefault(defaults.showTokenUsage),
        )

        _modelName.value = runCatching {
            plainPreferences.getString(KEY_CHAT_MODEL, null)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL

        val storedBackground = runCatching {
            plainPreferences.getString(KEY_APPEARANCE_BACKGROUND, null)
        }.getOrNull()
        // 默认值兜底：路径存在但文件已被删掉时，视为没有背景图。
        val validBackground = storedBackground?.takeIf { path ->
            runCatching { java.io.File(path).isFile }.getOrDefault(false)
        }
        _appearance.value = AppearanceSettings(
            backgroundPath = validBackground,
            backgroundOpacity = runCatching {
                plainPreferences.getFloat(KEY_APPEARANCE_OPACITY, DEFAULT_BACKGROUND_OPACITY)
            }.getOrDefault(DEFAULT_BACKGROUND_OPACITY)
                .coerceIn(BACKGROUND_OPACITY_MIN, BACKGROUND_OPACITY_MAX),
            accentArgb = runCatching {
                plainPreferences.getLong(KEY_APPEARANCE_ACCENT, DEFAULT_ACCENT_ARGB)
            }.getOrDefault(DEFAULT_ACCENT_ARGB),
        )
    }

    /** Reads the key from the encrypted store into the cache. Call from a background thread. */
    fun refreshApiKey() {
        _apiKey.value = runCatching { securePreferences.getString(KEY_API_KEY, null) }.getOrNull()
        _apiKeyLoaded.value = true
    }

    fun getApiKey(): String? {
        val cached = _apiKey.value
        if (!cached.isNullOrBlank()) return cached
        return runCatching { securePreferences.getString(KEY_API_KEY, null) }.getOrNull()
    }

    fun setApiKey(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            clearApiKey()
            return
        }
        securePreferences.edit().putString(KEY_API_KEY, trimmed).apply()
        _apiKey.value = trimmed
    }

    fun clearApiKey() {
        runCatching { securePreferences.edit().remove(KEY_API_KEY).apply() }
        _apiKey.value = null
    }

    fun setThemeMode(mode: ThemeMode) {
        plainPreferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    /** 三步引导卡的折叠状态；从别处跳进来时用 setApiGuideCollapsed(false) 自动展开。 */
    fun setApiGuideCollapsed(collapsed: Boolean) {
        plainPreferences.edit().putBoolean(KEY_API_GUIDE_COLLAPSED, collapsed).apply()
        _apiGuideCollapsed.value = collapsed
    }

    /** 角色设定指引卡的折叠状态。 */
    fun setPromptGuideCollapsed(collapsed: Boolean) {
        plainPreferences.edit().putBoolean(KEY_PROMPT_GUIDE_COLLAPSED, collapsed).apply()
        _promptGuideCollapsed.value = collapsed
    }

    /** 标记新手引导已完成（完成或跳过），之后不再自动全屏弹出。 */
    fun setOnboardingDone(done: Boolean) {
        plainPreferences.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
        _onboardingDone.value = done
    }

    /** 1.2：选择模型 */
    fun setModelName(id: String) {
        val trimmed = id.trim()
        if (trimmed.isEmpty()) return
        plainPreferences.edit().putString(KEY_CHAT_MODEL, trimmed).apply()
        _modelName.value = trimmed
    }

    /** 1.2：设置背景图（传 null 表示移除） */
    fun setBackgroundPath(path: String?) {
        val current = _appearance.value
        plainPreferences.edit().putString(KEY_APPEARANCE_BACKGROUND, path).apply()
        _appearance.value = current.copy(backgroundPath = path)
    }

    /** 1.5：设置背景图透明度（显示强度），即时生效并持久化 */
    fun setBackgroundOpacity(value: Float) {
        val current = _appearance.value
        val clamped = value.coerceIn(BACKGROUND_OPACITY_MIN, BACKGROUND_OPACITY_MAX)
        plainPreferences.edit().putFloat(KEY_APPEARANCE_OPACITY, clamped).apply()
        _appearance.value = current.copy(backgroundOpacity = clamped)
    }

    /** 1.2：设置主色调（ARGB），即时生效 */
    fun setAccentColor(argb: Long) {
        val current = _appearance.value
        plainPreferences.edit().putLong(KEY_APPEARANCE_ACCENT, argb).apply()
        _appearance.value = current.copy(accentArgb = argb)
    }

    /** 增量 C：「聊天体验」的任一开关/滑块都走这里，落盘并立即生效。 */
    fun updateChatExperience(transform: (ChatExperienceSettings) -> ChatExperienceSettings) {        val current = _chatExperience.value
        val next = transform(current).let { candidate ->
            candidate.copy(
                contextMessageCount = candidate.contextMessageCount
                    .coerceIn(CONTEXT_MESSAGES_MIN, CONTEXT_MESSAGES_MAX),
            )
        }
        plainPreferences.edit()
            .putBoolean(KEY_SEND_ON_ENTER, next.sendOnEnter)
            .putBoolean(KEY_AUTO_TITLE, next.autoGenerateTitle)
            .putInt(KEY_CONTEXT_MESSAGES, next.contextMessageCount)
            .putBoolean(KEY_STREAMING, next.streamingOutput)
            .putBoolean(KEY_SHOW_TOKEN_USAGE, next.showTokenUsage)
            .apply()
        _chatExperience.value = next
    }

    /** Shows the first few and last few characters only; the full key never reaches the UI. */
    fun maskApiKey(key: String?): String {
        if (key.isNullOrBlank()) return ""
        if (key.length <= 10) return "•".repeat(key.length)
        return key.take(5) + "•".repeat(6) + key.takeLast(4)
    }

    private companion object {
        const val SECURE_PREFS_NAME = "multipromptchat_secure"
        const val PLAIN_PREFS_NAME = "multipromptchat_prefs"
        const val KEY_API_KEY = "deepseek_api_key"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_API_GUIDE_COLLAPSED = "api_guide_collapsed"
        const val KEY_PROMPT_GUIDE_COLLAPSED = "prompt_guide_collapsed"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_SEND_ON_ENTER = "chat_send_on_enter"
        const val KEY_AUTO_TITLE = "chat_auto_title"
        const val KEY_CONTEXT_MESSAGES = "chat_context_messages"
        const val KEY_STREAMING = "chat_streaming"
        const val KEY_SHOW_TOKEN_USAGE = "chat_show_token_usage"
        const val KEY_CHAT_MODEL = "chat_model"
        const val KEY_APPEARANCE_BACKGROUND = "appearance_background_path"
        const val KEY_APPEARANCE_ACCENT = "appearance_accent_argb"
        const val KEY_APPEARANCE_OPACITY = "appearance_background_opacity"
    }
}
