package com.example.multipromptchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.multipromptchat.data.repository.ThemeMode
import com.example.multipromptchat.ui.navigation.AppNavHost
import com.example.multipromptchat.ui.theme.MultipromptChatTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as App).container

        setContent {
            val themeMode by container.settingsRepository.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MultipromptChatTheme(darkTheme = darkTheme) {
                AppNavHost(container = container)
            }
        }
    }
}
