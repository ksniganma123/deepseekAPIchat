package com.example.multipromptchat

import android.content.Context
import com.example.multipromptchat.data.local.AppDatabase
import com.example.multipromptchat.data.remote.DeepSeekClient
import com.example.multipromptchat.data.repository.ChatRepository
import com.example.multipromptchat.data.repository.ConversationRepository
import com.example.multipromptchat.data.repository.PromptRepository
import com.example.multipromptchat.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

/**
 * Hand-rolled dependency container (v1 deliberately avoids Hilt/Koin).
 * Everything is lazy so app start-up stays cheap.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Long-lived scope for app-level work such as seeding default prompts. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    private val httpClient: OkHttpClient by lazy { DeepSeekClient.defaultClient() }

    private val deepSeekClient: DeepSeekClient by lazy { DeepSeekClient(httpClient) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val promptRepository: PromptRepository by lazy {
        PromptRepository(
            promptDao = database.promptDao(),
            conversationDao = database.conversationDao(),
        )
    }

    val conversationRepository: ConversationRepository by lazy {
        ConversationRepository(
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
        )
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            promptDao = database.promptDao(),
            deepSeekClient = deepSeekClient,
            settingsRepository = settingsRepository,
        )
    }
}
