package com.example.multipromptchat.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.example.multipromptchat.data.remote.ModelInfo
import com.example.multipromptchat.ui.components.PromptTag
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.multipromptchat.data.image.ImageStore
import com.example.multipromptchat.data.repository.DEFAULT_ACCENT_ARGB
import com.example.multipromptchat.ui.theme.accentHueOf
import com.example.multipromptchat.ui.theme.hueToAccentArgb
import com.example.multipromptchat.ui.theme.isDefaultAccent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.data.repository.CONTEXT_MESSAGES_MAX
import com.example.multipromptchat.data.repository.CONTEXT_MESSAGES_MIN
import com.example.multipromptchat.data.repository.ThemeMode
import com.example.multipromptchat.ui.Strings
import com.example.multipromptchat.ui.theme.screenBackgroundColor
import com.example.multipromptchat.ui.components.ApiKeyMissingCard
import com.example.multipromptchat.ui.components.HarmonyCard
import com.example.multipromptchat.ui.components.PageTitle
import com.example.multipromptchat.ui.components.PillButton
import com.example.multipromptchat.ui.components.PillVariant
import com.example.multipromptchat.ui.components.SectionHeader

@Composable
fun SettingsScreen(
    container: AppContainer,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by viewModel.uiState.collectAsState()

    var keyInput by remember { mutableStateOf("") }
    var showLicense by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val apiGuideCollapsed by container.settingsRepository.apiGuideCollapsed.collectAsState()

    // ---- 1.2：背景图选择 + 主色调滑杆 ------------------------------------
    var wallpaperBusy by remember { mutableStateOf(false) }
    var accentHue by remember { mutableStateOf(accentHueOf(container.settingsRepository.appearance.value.accentArgb)) }
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current
    LaunchedEffect(state.appearance.accentArgb) {
        accentHue = accentHueOf(state.appearance.accentArgb)
    }
    val backgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            wallpaperBusy = true
            scope.launch {
                val result = ImageStore.import(
                    context = appContext,
                    uri = uri,
                    dirName = ImageStore.DIR_WALLPAPER,
                    maxEdge = 1600,
                )
                wallpaperBusy = false
                when (result) {
                    is ImageStore.Result.Success -> {
                        ImageStore.deleteQuietly(viewModel.uiState.value.appearance.backgroundPath)
                        viewModel.setBackgroundPath(result.file.absolutePath)
                    }
                    is ImageStore.Result.Error -> viewModel.reportImageError(result.failure)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundColor())
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            PageTitle(title = Strings.settingsTitle, subtitle = Strings.appTagline)
        }

        // 增量 A3：没有密钥时给一张中性引导卡（点击即展开下面的三步引导）。
        if (!state.hasKey) {
            ApiKeyMissingCard(
                onClick = { container.settingsRepository.setApiGuideCollapsed(false) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.message != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.message.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::dismissMessage) {
                        Text(Strings.dismiss, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // ---- API key ------------------------------------------------------
        SectionHeader(
            title = Strings.apiKeySection,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        HarmonyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = if (state.hasKey) {
                    Strings.apiKeyStored + state.maskedKey
                } else {
                    Strings.apiKeyEmpty
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text(Strings.apiKeyLabel) },
                placeholder = { Text(Strings.apiKeyPlaceholder) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillButton(
                    text = Strings.saveKey,
                    onClick = {
                        viewModel.saveApiKey(keyInput)
                        keyInput = ""
                    },
                    enabled = keyInput.isNotBlank(),
                )
                Spacer(modifier = Modifier.width(10.dp))
                PillButton(
                    text = Strings.testConnection,
                    onClick = viewModel::testConnection,
                    variant = PillVariant.Outline,
                    enabled = state.hasKey && state.testState != ConnectionTestState.Testing,
                )
                Spacer(modifier = Modifier.width(10.dp))
                PillButton(
                    text = Strings.clearKey,
                    onClick = { showClearConfirm = true },
                    variant = PillVariant.Outline,
                    enabled = state.hasKey,
                )
            }

            // 增量 A1：测试连接三态。
            when (val test = state.testState) {
                ConnectionTestState.Idle -> Unit
                ConnectionTestState.Testing -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    TestStatusRow(
                        text = Strings.testingConnection,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        showSpinner = true,
                    )
                }
                ConnectionTestState.Success -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    TestStatusRow(
                        text = Strings.connectionOk,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                is ConnectionTestState.Failed -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    TestStatusRow(
                        text = test.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Strings.modelSectionTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = state.modelName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Strings.modelHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PillButton(
                text = Strings.modelFetch,
                onClick = viewModel::fetchModels,
                variant = PillVariant.Outline,
                enabled = state.hasKey && state.modelState != ModelFetchState.Loading,
            )

            when (val modelState = state.modelState) {
                ModelFetchState.Idle -> Unit
                ModelFetchState.Loading -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    TestStatusRow(
                        text = Strings.modelFetching,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        showSpinner = true,
                    )
                }
                is ModelFetchState.Loaded -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    TestStatusRow(
                        text = Strings.modelFetched(modelState.count),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                is ModelFetchState.Failed -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    TestStatusRow(
                        text = modelState.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (state.models.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    state.models.forEach { model ->
                        ModelRow(
                            model = model,
                            selected = model.id == state.modelName,
                            onClick = { viewModel.selectModel(model.id) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = Strings.apiKeyPrivacy,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 增量 A2：三步图文引导（折叠状态持久化）。
        ApiKeyGuideCard(
            expanded = !apiGuideCollapsed,
            onToggle = { container.settingsRepository.setApiGuideCollapsed(!apiGuideCollapsed) },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        // ---- 增量 C：聊天体验 -----------------------------------------------
        SectionHeader(
            title = Strings.chatExperienceSection,
            subtitle = "改完立即生效，只影响后续请求",
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 8.dp),
        )

        val experience = state.chatExperience

        // 1. 发送快捷键
        HarmonyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = Strings.sendShortcutTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Strings.sendShortcutDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(
                    label = Strings.sendShortcutEnter,
                    selected = experience.sendOnEnter,
                    onClick = { viewModel.setSendOnEnter(true) },
                )
                ChoiceChip(
                    label = Strings.sendShortcutNewline,
                    selected = !experience.sendOnEnter,
                    onClick = { viewModel.setSendOnEnter(false) },
                )
            }
            if (!experience.sendOnEnter) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = Strings.sendShortcutNewlineHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. 自动生成会话标题
        HarmonyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            SwitchSettingRow(
                title = Strings.autoTitleTitle,
                description = Strings.autoTitleDesc,
                checked = experience.autoGenerateTitle,
                onCheckedChange = viewModel::setAutoGenerateTitle,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. 上下文消息条数
        HarmonyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Strings.contextCountTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Strings.contextCountValue(experience.contextMessageCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Strings.contextCountDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = experience.contextMessageCount.toFloat(),
                onValueChange = { value -> viewModel.setContextMessageCount(value.roundToInt()) },
                valueRange = CONTEXT_MESSAGES_MIN.toFloat()..CONTEXT_MESSAGES_MAX.toFloat(),
                steps = CONTEXT_MESSAGES_MAX - CONTEXT_MESSAGES_MIN - 1,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. 流式输出
        HarmonyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            SwitchSettingRow(
                title = Strings.streamingTitle,
                description = Strings.streamingDesc,
                checked = experience.streamingOutput,
                onCheckedChange = viewModel::setStreamingOutput,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. token 消耗与缓存命中率
        HarmonyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            SwitchSettingRow(
                title = Strings.tokenUsageTitle,
                description = Strings.tokenUsageDesc,
                checked = experience.showTokenUsage,
                onCheckedChange = viewModel::setShowTokenUsage,
            )
        }

        // ---- 外观（深浅色 + 背景图 + 主色调，统一一组）----------------------
        SectionHeader(
            title = Strings.themeSection,
            subtitle = Strings.appearanceSectionDesc,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 8.dp),
        )

        // 1) 深浅色模式
        HarmonyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = Strings.themeModeTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Strings.themeModeDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(
                    label = Strings.themeSystem,
                    selected = state.themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                )
                ChoiceChip(
                    label = Strings.themeLight,
                    selected = state.themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                )
                ChoiceChip(
                    label = Strings.themeDark,
                    selected = state.themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2) 背景图
        HarmonyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = Strings.appearanceBackgroundTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Strings.appearanceBackgroundDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))

            val backgroundPreview = remember(state.appearance.backgroundPath) {
                state.appearance.backgroundPath?.let { path ->
                    runCatching { android.graphics.BitmapFactory.decodeFile(path) }.getOrNull()
                }
            }
            if (backgroundPreview != null) {
                androidx.compose.foundation.Image(
                    bitmap = backgroundPreview.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(MaterialTheme.shapes.medium),
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Text(
                    text = Strings.appearanceBackgroundEmpty,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                PillButton(
                    text = if (wallpaperBusy) {
                        Strings.appearanceBackgroundBusy
                    } else {
                        Strings.appearanceBackgroundPick
                    },
                    onClick = {
                        backgroundPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !wallpaperBusy,
                )
                if (state.appearance.hasBackground) {
                    Spacer(modifier = Modifier.width(10.dp))
                    PillButton(
                        text = Strings.appearanceBackgroundRemove,
                        variant = PillVariant.Outline,
                        onClick = {
                            ImageStore.deleteQuietly(state.appearance.backgroundPath)
                            viewModel.setBackgroundPath(null)
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Strings.imageLimitsHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3) 主色调
        HarmonyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Strings.appearanceAccentTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = CircleShape,
                    color = Color(state.appearance.accentArgb.toInt()),
                    modifier = Modifier.size(22.dp),
                ) {}
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Strings.appearanceAccentDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ACCENT_PRESETS.forEach { argb ->
                    val selected = state.appearance.accentArgb == argb
                    Surface(
                        shape = CircleShape,
                        color = Color(argb.toInt()),
                        border = if (selected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                        } else {
                            null
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.setAccentColor(argb) },
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                value = accentHue,
                onValueChange = { hue ->
                    accentHue = hue
                    viewModel.setAccentColor(hueToAccentArgb(hue))
                },
                valueRange = 0f..360f,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isDefaultAccent(state.appearance.accentArgb)) {
                        Strings.appearanceAccentDefault
                    } else {
                        "#%06X".format(state.appearance.accentArgb and 0xFFFFFFL)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.setAccentColor(DEFAULT_ACCENT_ARGB) }) {
                    Text(
                        text = Strings.appearanceAccentReset,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        // ---- About --------------------------------------------------------
        SectionHeader(
            title = Strings.aboutSection,
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 8.dp),
        )
        HarmonyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = Strings.appName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Strings.appVersion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Strings.copyrightHolder,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = Strings.licenseSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            PillButton(
                text = Strings.viewLicense,
                onClick = { showLicense = true },
                variant = PillVariant.Tonal,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // 增量 A1：清除密钥前二次确认。
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(text = Strings.clearKeyConfirmTitle, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(
                    text = Strings.clearKeyConfirmMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearApiKey()
                    },
                ) {
                    Text(text = Strings.clearKey, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(Strings.cancel) }
            },
        )
    }

    if (showLicense) {
        AlertDialog(
            onDismissRequest = { showLicense = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = Strings.licenseTitle, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = Strings.licenseBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicense = false }) { Text(Strings.dismiss) }
            },
        )
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        PillButton(text = label, onClick = onClick)
    } else {
        PillButton(text = label, onClick = onClick, variant = PillVariant.Outline)
    }
}

/** 增量 C：开关型设置行（名称 + 简短说明 + 右侧开关）。 */
@Composable
private fun SwitchSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 增量 A1：一行测试结果，三态共用。 */
@Composable
private fun TestStatusRow(
    text: String,
    color: Color,
    showSpinner: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showSpinner) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

/** 1.2：预设主色调（低饱和）。第一项是默认色。 */
private val ACCENT_PRESETS: List<Long> = listOf(
    0xFF3A7AFEL,
    0xFF2F9C95L,
    0xFF7C5CFFL,
    0xFFE4739AL,
    0xFFE2823FL,
    0xFF4CA65CL,
    0xFF4A5BC4L,
    0xFF6B7280L,
)

/** 1.2：可选的模型行（点选即生效） */
@Composable
private fun ModelRow(
    model: ModelInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = model.id,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        if (model.looksVisionCapable) {
            PromptTag(text = Strings.modelVisionTag)
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
