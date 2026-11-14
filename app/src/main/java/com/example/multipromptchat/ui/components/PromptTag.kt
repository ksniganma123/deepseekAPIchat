package com.example.multipromptchat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.multipromptchat.ui.theme.PillShape

/** Small capsule showing the persona bound to a conversation. */
@Composable
fun PromptTag(
    text: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val base = modifier
    val clickableModifier = if (onClick != null) base.clickable(onClick = onClick) else base

    Surface(
        modifier = clickableModifier,
        shape = PillShape,
        color = if (emphasised) colors.primary else colors.primaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (emphasised) colors.onPrimary else colors.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}
