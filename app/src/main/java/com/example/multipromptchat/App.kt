package com.example.multipromptchat

import android.app.Application
import kotlinx.coroutines.launch

class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.applicationScope.launch {
            runCatching { container.promptRepository.ensureDefaultPrompts() }
        }
        container.applicationScope.launch {
            container.settingsRepository.refreshApiKey()
        }
    }
}
