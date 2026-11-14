package com.example.multipromptchat.ui.prompts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.multipromptchat.ui.Strings
import com.example.multipromptchat.ui.components.CollapsibleGuideCard

/**
 * B1：人格设定写法指引。正反示例各两组，✅ 示例可一键填入输入框。
 */
@Composable
fun PromptGuideCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    onUseExample: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CollapsibleGuideCard(
        title = Strings.promptGuideTitle,
        subtitle = Strings.promptGuideSubtitle,
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
    ) {
        Strings.promptGuidePoints.forEach { point ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = point,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = Strings.promptGuideExampleTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))
        ExamplePair(
            bad = Strings.promptGuideBad1,
            good = Strings.promptGuideGood1,
            onUse = { onUseExample(Strings.promptGuideGood1) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        ExamplePair(
            bad = Strings.promptGuideBad2,
            good = Strings.promptGuideGood2,
            onUse = { onUseExample(Strings.promptGuideGood2) },
        )
    }
}

@Composable
private fun ExamplePair(
    bad: String,
    good: String,
    onUse: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "❌",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = bad,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "✅",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = good,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onUse) {
                    Text(
                        text = Strings.useExample,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
