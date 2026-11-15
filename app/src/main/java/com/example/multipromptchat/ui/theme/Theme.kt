package com.example.multipromptchat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.multipromptchat.data.repository.DEFAULT_ACCENT_ARGB

/**
 * 单一主题入口。
 *
 * - 颜色只有一个来源：本文件 + [DynamicColor]；页面里不写死色值。
 * - 1.2 起支持自定义主色调：[accentSeed] 为 ARGB，等于默认值时沿用 1.1 手工调好的配色，
 *   否则从种子色派生一组低饱和强调色。
 */
@Composable
fun MultipromptChatTheme(
    darkTheme: Boolean = false,
    accentSeed: Long = DEFAULT_ACCENT_ARGB,
    content: @Composable () -> Unit,
) {
    val scheme = if (accentSeed == DEFAULT_ACCENT_ARGB) {
        if (darkTheme) darkScheme(DefaultDarkTones) else lightScheme(DefaultLightTones)
    } else {
        val seedColor = Color(accentSeed.toInt())
        val tones = remember(accentSeed, darkTheme) { deriveAccentTones(seedColor, darkTheme) }
        if (darkTheme) darkScheme(tones) else lightScheme(tones)
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = HarmonyTypography,
        shapes = HarmonyShapes,
        content = content,
    )
}
