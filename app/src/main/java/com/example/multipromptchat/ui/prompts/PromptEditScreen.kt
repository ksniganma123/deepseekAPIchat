package com.example.multipromptchat.ui.prompts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.ui.Strings
import com.example.multipromptchat.ui.theme.screenBackgroundColor
import com.example.multipromptchat.ui.components.ErrorBanner
import com.example.multipromptchat.ui.components.HarmonyCard
import com.example.multipromptchat.ui.components.LoadingBox
import com.example.multipromptchat.ui.components.PillButton

@Composable
fun PromptEditScreen(
    container: AppContainer,
    promptId: Long?,
    onDone: () -> Unit,
) {
    val viewModel: PromptEditViewModel = viewModel(
        key = "prompt-edit-${promptId ?: 0L}",
        factory = PromptEditViewModel.factory(container, promptId),
    )
    val state by viewModel.uiState.collectAsState()

    // 增量 B1：指引卡折叠状态持久化在 SettingsRepository（沿用已有 SharedPreferences）。
    val promptGuideCollapsed by container.settingsRepository.promptGuideCollapsed.collectAsState()
    var pendingExample by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundColor()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = Strings.back,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (state.isNew) Strings.newPromptTitle else Strings.editPromptTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.loading) {
            LoadingBox(modifier = Modifier.weight(1f), label = Strings.loading)
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 20.dp),
            ) {
                // 增量 B1：编辑页顶部的「角色设定」写法指引。
                PromptGuideCard(
                    expanded = !promptGuideCollapsed,
                    onToggle = {
                        container.settingsRepository.setPromptGuideCollapsed(!promptGuideCollapsed)
                    },
                    onUseExample = { example ->
                        if (state.systemPrompt.isBlank()) {
                            viewModel.onSystemPromptChange(example)
                        } else {
                            pendingExample = example
                        }
                    },
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                HarmonyCard {
                    Text(
                        text = Strings.promptNameLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        placeholder = { Text(Strings.promptNamePlaceholder) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = Strings.systemPromptLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.systemPrompt,
                        onValueChange = viewModel::onSystemPromptChange,
                        placeholder = { Text(Strings.systemPromptPlaceholder) },
                        minLines = 6,
                        maxLines = 12,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // 增量 B2：实时字数 + 辅助文案。
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Strings.charCount(state.systemPrompt.length),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.promptHelper,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HarmonyCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Strings.temperatureLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = String.format("%.1f", state.temperature),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = state.temperature,
                        onValueChange = viewModel::onTemperatureChange,
                        valueRange = 0f..2f,
                        steps = 19,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Strings.topPLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = String.format("%.1f", state.topP),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = state.topP,
                        onValueChange = viewModel::onTopPChange,
                        valueRange = 0f..1f,
                        steps = 9,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                HarmonyCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Strings.resetSwitchLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Strings.resetSwitchDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = state.resetContextOnSwitch,
                            onCheckedChange = viewModel::onResetContextChange,
                        )
                    }
                }
            }

            state.error?.let { message ->
                ErrorBanner(
                    message = message,
                    onDismiss = viewModel::dismissError,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            PillButton(
                text = Strings.save,
                onClick = { viewModel.save(onSaved = onDone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
            )
        }
    }

    // 增量 B1：示例覆盖已有内容前二次确认。
    val exampleToApply = pendingExample
    if (exampleToApply != null) {
        AlertDialog(
            onDismissRequest = { pendingExample = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(text = Strings.overwriteConfirmTitle, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(
                    text = Strings.overwriteConfirmMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingExample = null
                        viewModel.onSystemPromptChange(exampleToApply)
                    },
                ) {
                    Text(
                        text = Strings.overwriteConfirmOk,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingExample = null }) { Text(Strings.cancel) }
            },
        )
    }
}
