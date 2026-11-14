package com.example.multipromptchat.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.R
import com.example.multipromptchat.ui.Strings
import com.example.multipromptchat.ui.components.GuideScreenshot
import com.example.multipromptchat.ui.components.PillButton
import com.example.multipromptchat.ui.components.PillVariant
import com.example.multipromptchat.ui.settings.ConnectionTestState
import com.example.multipromptchat.ui.settings.SettingsViewModel
import com.example.multipromptchat.ui.theme.CardShape

/**
 * 初次使用时的全屏三步引导。
 *
 * - 第 1 步：欢迎 + 这个软件是干什么的（先介绍，再要密钥）。
 * - 第 2 步：怎么在开放平台拿到密钥（三张真实截图）。
 * - 第 3 步：直接在本页粘贴、保存、测试连接 —— 不需要先跳去设置页。
 *
 * 复用已有的 SettingsViewModel 做保存与测试，不新增状态机。
 */
@Composable
fun OnboardingScreen(
    container: AppContainer,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by viewModel.uiState.collectAsState()

    var step by remember { mutableStateOf(0) }
    var keyInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    BackHandler(enabled = step > 0) { step -= 1 }

    val swallowTaps = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // 拦住点击，避免穿透到下面的 Scaffold / 底部导航。
            .clickable(
                interactionSource = swallowTaps,
                indication = null,
                onClick = {},
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onSkip) {
                Text(
                    text = Strings.onboardingSkip,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        StepDots(current = step, total = 3)
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            when (step) {
                0 -> StepWelcome()
                1 -> StepGetKey(onOpenPlatform = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(Strings.deepseekApiKeysUrl))
                        )
                    }
                })
                else -> StepPasteKey(
                    keyInput = keyInput,
                    onKeyInputChange = { keyInput = it },
                    maskedKey = state.maskedKey,
                    hasKey = state.hasKey,
                    testState = state.testState,
                    onSave = {
                        viewModel.saveApiKey(keyInput)
                        keyInput = ""
                    },
                    onTest = viewModel::testConnection,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) {
                PillButton(
                    text = Strings.onboardingBack,
                    onClick = { step -= 1 },
                    variant = PillVariant.Ghost,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            PillButton(
                text = when (step) {
                    0 -> Strings.onboardingStart
                    1 -> Strings.onboardingNext
                    else -> Strings.onboardingFinish
                },
                onClick = {
                    if (step < 2) step += 1 else onFinish()
                },
                enabled = step < 2 || state.hasKey,
            )
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(if (index == current) 16.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    ),
            )
        }
    }
}

// ---- 第 1 步：欢迎 -------------------------------------------------------

@Composable
private fun StepWelcome() {
    Text(
        text = Strings.onboardingWelcomeTitle,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(8.dp))
    // 主标语：直接点出「会话内换人设」这个最核心的差异点
    Text(
        text = Strings.onboardingWelcomeLead,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = Strings.welcomeCoreLabel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(10.dp))
    FeatureHighlight(
        icon = Icons.Filled.Face,
        title = Strings.welcomeFeatureSwitchTitle,
        body = Strings.welcomeFeatureSwitchBody,
    )
    Spacer(modifier = Modifier.height(12.dp))
    FeatureHighlight(
        icon = Icons.Filled.List,
        title = Strings.welcomeFeatureBatchTitle,
        body = Strings.welcomeFeatureBatchBody,
    )

    Spacer(modifier = Modifier.height(20.dp))
    QuietPoint(Strings.onboardingWelcomePoint2)
    Spacer(modifier = Modifier.height(10.dp))
    QuietPoint(Strings.onboardingWelcomePoint3)

    Spacer(modifier = Modifier.height(24.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = Strings.onboardingWelcomeFootTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Strings.onboardingWelcomeFoot,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 核心卖点卡：图标 + 加重标题 + 一句解释，比普通列表项更抢眼。 */
@Composable
private fun FeatureHighlight(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 次要卖点：低对比、小圆点，不跟上面两张卡抢注意力。 */
@Composable
private fun QuietPoint(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---- 第 2 步：怎么拿到密钥（真实截图） -----------------------------------

@Composable
private fun StepGetKey(onOpenPlatform: () -> Unit) {
    Text(
        text = Strings.onboardingStepKeyTitle,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = Strings.onboardingStepKeyDesc,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(20.dp))
    NumberedLine(index = "1", text = Strings.apiGuideStep1Title)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = Strings.apiGuideStep1Desc,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 30.dp),
    )
    Spacer(modifier = Modifier.height(10.dp))
    PillButton(
        text = Strings.apiGuideOpenPlatform,
        onClick = onOpenPlatform,
        variant = PillVariant.Outline,
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

    Spacer(modifier = Modifier.height(20.dp))
    NumberedLine(index = "2", text = Strings.apiGuideStep2Title)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = Strings.apiGuideStep2Desc,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 30.dp),
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

    Spacer(modifier = Modifier.height(20.dp))
    NumberedLine(index = "3", text = Strings.apiGuideStep3Title)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = Strings.apiGuideStep3Desc,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 30.dp),
    )

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = Strings.apiGuideBillingNote,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---- 第 3 步：粘贴并验证 -------------------------------------------------

@Composable
private fun StepPasteKey(
    keyInput: String,
    onKeyInputChange: (String) -> Unit,
    maskedKey: String,
    hasKey: Boolean,
    testState: ConnectionTestState,
    onSave: () -> Unit,
    onTest: () -> Unit,
) {
    Text(
        text = Strings.onboardingStepPasteTitle,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = Strings.onboardingStepPasteDesc,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(20.dp))

    OutlinedTextField(
        value = keyInput,
        onValueChange = onKeyInputChange,
        label = { Text(Strings.apiKeyLabel) },
        placeholder = { Text(Strings.apiKeyPlaceholder) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )

    if (hasKey) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = Strings.apiKeyStored + maskedKey,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        PillButton(
            text = Strings.saveKey,
            onClick = onSave,
            enabled = keyInput.isNotBlank(),
        )
        Spacer(modifier = Modifier.width(10.dp))
        PillButton(
            text = Strings.testConnection,
            onClick = onTest,
            variant = PillVariant.Outline,
            enabled = hasKey && testState != ConnectionTestState.Testing,
        )
    }

    when (testState) {
        ConnectionTestState.Idle -> Unit
        ConnectionTestState.Testing -> {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Strings.testingConnection,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ConnectionTestState.Success -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = Strings.connectionOk,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        is ConnectionTestState.Failed -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = testState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = Strings.apiKeyPrivacy,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NumberedLine(index: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
