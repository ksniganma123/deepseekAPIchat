package com.example.multipromptchat.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.multipromptchat.R
import com.example.multipromptchat.ui.Strings
import com.example.multipromptchat.ui.components.CollapsibleGuideCard
import com.example.multipromptchat.ui.components.GuideScreenshot
import com.example.multipromptchat.ui.components.PillButton
import com.example.multipromptchat.ui.components.PillVariant

/**
 * A2：三步图文引导（设置页内嵌版）。图片为真实截图。
 */
@Composable
fun ApiKeyGuideCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    CollapsibleGuideCard(
        title = Strings.apiGuideTitle,
        subtitle = Strings.apiGuideSubtitle,
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
    ) {
        GuideStep(
            number = "1",
            title = Strings.apiGuideStep1Title,
            description = Strings.apiGuideStep1Desc,
        )
        Spacer(modifier = Modifier.height(10.dp))
        PillButton(
            text = Strings.apiGuideOpenPlatform,
            variant = PillVariant.Outline,
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(Strings.deepseekApiKeysUrl))
                    )
                }
            },
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = Strings.deepseekApiKeysUrl,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        GuideScreenshot(
            imageRes = R.drawable.guide_platform_home,
            caption = Strings.shotCaptionPlatform,
        )

        Spacer(modifier = Modifier.height(18.dp))
        GuideStep(
            number = "2",
            title = Strings.apiGuideStep2Title,
            description = Strings.apiGuideStep2Desc,
        )
        Spacer(modifier = Modifier.height(12.dp))
        GuideScreenshot(
            imageRes = R.drawable.guide_create_key_dialog,
            caption = Strings.shotCaptionCreate,
        )
        Spacer(modifier = Modifier.height(12.dp))
        GuideScreenshot(
            imageRes = R.drawable.guide_key_created,
            caption = Strings.shotCaptionCopy,
        )

        Spacer(modifier = Modifier.height(18.dp))
        GuideStep(
            number = "3",
            title = Strings.apiGuideStep3Title,
            description = Strings.apiGuideStep3Desc,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = Strings.apiGuideBillingNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GuideStep(
    number: String,
    title: String,
    description: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
