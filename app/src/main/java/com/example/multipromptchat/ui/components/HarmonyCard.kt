package com.example.multipromptchat.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.multipromptchat.ui.theme.CardShape

/**
 * The base surface of the whole app: 20dp corners, whisper-soft shadow, 16dp inner padding.
 * When [onClick] is set the card scales slightly on press (no heavy ripple).
 */
@Composable
fun HarmonyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier.fillMaxWidth()

    if (onClick == null) {
        Surface(
            modifier = base,
            shape = CardShape,
            color = containerColor,
            shadowElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.98f else 1f,
            label = "harmonyCardScale",
        )
        Surface(
            modifier = base
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CardShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            shape = CardShape,
            color = containerColor,
            shadowElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}
