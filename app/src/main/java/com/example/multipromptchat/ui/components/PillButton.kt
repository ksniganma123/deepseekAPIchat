package com.example.multipromptchat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.multipromptchat.ui.theme.PillShape

enum class PillVariant { Primary, Tonal, Outline, Ghost }

/**
 * Capsule button in four weights. Primary is filled with the accent colour,
 * everything else stays quiet so screens do not look busy.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: PillVariant = PillVariant.Primary,
    leadingIcon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    val contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

    when (variant) {
        PillVariant.Primary -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                disabledContainerColor = colors.primary.copy(alpha = 0.35f),
                disabledContentColor = colors.onPrimary.copy(alpha = 0.8f),
            ),
            contentPadding = contentPadding,
        ) {
            PillContent(text = text, leadingIcon = leadingIcon)
        }

        PillVariant.Tonal -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            ),
            contentPadding = contentPadding,
        ) {
            PillContent(text = text, leadingIcon = leadingIcon)
        }

        PillVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = PillShape,
            border = BorderStroke(1.dp, colors.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
            contentPadding = contentPadding,
        ) {
            PillContent(text = text, leadingIcon = leadingIcon)
        }

        PillVariant.Ghost -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = PillShape,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurfaceVariant),
            contentPadding = contentPadding,
        ) {
            PillContent(text = text, leadingIcon = leadingIcon)
        }
    }
}

@Composable
private fun RowScope.PillContent(text: String, leadingIcon: ImageVector?) {
    if (leadingIcon != null) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge)
}
