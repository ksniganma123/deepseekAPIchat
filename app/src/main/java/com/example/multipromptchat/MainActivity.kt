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
            val appearance by container.settingsRepository.appearance.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // 1.2：主色调改动后整棵树即时重组，无需重启。
            MultipromptChatTheme(
                darkTheme = darkTheme,
                accentSeed = appearance.accentArgb,
            ) {
                AppNavHost(container = container)
            }
        }
    }
}
