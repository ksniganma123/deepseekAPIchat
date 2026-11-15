package com.example.multipromptchat.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.multipromptchat.data.repository.DEFAULT_ACCENT_ARGB

/** 一组强调色：主色 / 主色上的文字 / 容器 / 容器上的文字。 */
internal data class AccentTones(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
)

/**
 * 1.2：从用户的种子色派生强调色。
 * 刻意把饱和度收在 0.25~0.75，保持 HarmonyOS 那种低饱和观感。
 */
internal fun deriveAccentTones(seed: Color, dark: Boolean): AccentTones {
    val hsl = seed.toHsl()
    val hue = hsl[0]
    val sat = hsl[1].coerceIn(0.25f, 0.75f)
    return if (dark) {
        AccentTones(
            primary = hslToColor(hue, sat * 0.85f, 0.70f),
            onPrimary = hslToColor(hue, sat, 0.12f),
            container = hslToColor(hue, sat * 0.85f, 0.20f),
            onContainer = hslToColor(hue, sat * 0.65f, 0.88f),
        )
    } else {
        AccentTones(
            primary = hslToColor(hue, sat, 0.52f),
            onPrimary = Color.White,
            container = hslToColor(hue, sat * 0.70f, 0.94f),
            onContainer = hslToColor(hue, sat, 0.32f),
        )
    }
}

/** 默认（不自定义主色）时沿用主题里手工调过的值，保证观感和 1.1 一致。 */
internal val DefaultLightTones = AccentTones(
    primary = LightAccent,
    onPrimary = LightSurface,
    container = LightAccentSoft,
    onContainer = LightAccentDeep,
)

internal val DefaultDarkTones = AccentTones(
    primary = DarkAccent,
    onPrimary = DarkBackground,
    container = DarkAccentSoft,
    onContainer = DarkAccentDeep,
)

internal fun lightScheme(tones: AccentTones): ColorScheme = lightColorScheme(
    primary = tones.primary,
    onPrimary = tones.onPrimary,
    primaryContainer = tones.container,
    onPrimaryContainer = tones.onContainer,
    secondary = LightSecondary,
    onSecondary = LightSurface,
    secondaryContainer = LightSecondarySoft,
    onSecondaryContainer = LightSecondary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = LightError,
    onError = LightSurface,
    errorContainer = LightErrorSoft,
    onErrorContainer = LightError,
)

internal fun darkScheme(tones: AccentTones): ColorScheme = darkColorScheme(
    primary = tones.primary,
    onPrimary = tones.onPrimary,
    primaryContainer = tones.container,
    onPrimaryContainer = tones.onContainer,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSecondarySoft,
    onSecondaryContainer = DarkSecondary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = DarkError,
    onError = DarkBackground,
    errorContainer = DarkErrorSoft,
    onErrorContainer = DarkError,
)

/** 是否正在使用自定义背景图；页面根容器据此决定要不要画不透明底色。 */
val LocalWallpaperActive: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

/** 页面根背景：有背景图时透明（让下面的图透出来），否则用主题背景色。 */
@Composable
fun screenBackgroundColor(): Color =
    if (LocalWallpaperActive.current) Color.Transparent else androidx.compose.material3.MaterialTheme.colorScheme.background

/** 主色调是否为默认值 */
fun isDefaultAccent(argb: Long): Boolean = argb == DEFAULT_ACCENT_ARGB

/** 色相滑块 → ARGB（低饱和，与 [deriveAccentTones] 同一套取值口径） */
fun hueToAccentArgb(hue: Float): Long = argbOf(hslToColor(hue, 0.55f, 0.52f))

/** ARGB → 色相（0..360），用于把滑杆位置对齐当前主色 */
fun accentHueOf(argb: Long): Float = Color(argb.toInt()).toHsl()[0]

private fun argbOf(color: Color): Long {
    val a = (color.alpha * 255f).toInt() and 0xFF
    val r = (color.red * 255f).toInt() and 0xFF
    val g = (color.green * 255f).toInt() and 0xFF
    val b = (color.blue * 255f).toInt() and 0xFF
    return ((a shl 24) or (r shl 16) or (g shl 8) or b).toLong() and 0xFFFFFFFFL
}

// ---- HSL helpers ---------------------------------------------------------

private fun hslToColor(hueDeg: Float, saturation: Float, lightness: Float): Color {
    val h = ((hueDeg % 360f) + 360f) % 360f / 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    if (s == 0f) return Color(l, l, l)

    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    return Color(
        red = hueToRgb(p, q, h + 1f / 3f),
        green = hueToRgb(p, q, h),
        blue = hueToRgb(p, q, h - 1f / 3f),
    )
}

private fun hueToRgb(p: Float, q: Float, t: Float): Float {
    var x = t
    if (x < 0f) x += 1f
    if (x > 1f) x -= 1f
    return when {
        x < 1f / 6f -> p + (q - p) * 6f * x
        x < 1f / 2f -> q
        x < 2f / 3f -> p + (q - p) * (2f / 3f - x) * 6f
        else -> p
    }
}

/** 返回 [hue(0..360), saturation, lightness] */
private fun Color.toHsl(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val lightness = (max + min) / 2f
    if (max == min) return floatArrayOf(0f, 0f, lightness)

    val delta = max - min
    val saturation = if (lightness > 0.5f) delta / (2f - max - min) else delta / (max + min)
    val hue = when (max) {
        r -> ((g - b) / delta + if (g < b) 6f else 0f)
        g -> ((b - r) / delta + 2f)
        else -> ((r - g) / delta + 4f)
    } * 60f
    return floatArrayOf(hue, saturation, lightness)
}
