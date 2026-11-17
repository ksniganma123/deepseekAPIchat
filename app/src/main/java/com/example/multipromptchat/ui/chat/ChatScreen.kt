package com.example.multipromptchat.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.multipromptchat.data.image.ImageStore
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.domain.Message
import com.example.multipromptchat.domain.MessageRole
import com.example.multipromptchat.domain.PromptTemplate
import androidx.compose.ui.unit.Dp
import com.example.multipromptchat.ui.Strings
import com.example.multipromptchat.ui.theme.screenBackgroundColor
import com.example.multipromptchat.ui.components.ApiKeyMissingCard
import com.example.multipromptchat.ui.components.EmptyState
import com.example.multipromptchat.ui.components.ErrorBanner
import com.example.multipromptchat.ui.components.HarmonyCard
import com.example.multipromptchat.ui.components.LoadingBox
import com.example.multipromptchat.ui.components.MarkdownText
import com.example.multipromptchat.ui.components.PromptTag
import com.example.multipromptchat.ui.theme.SheetShape
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(
    conversationId: Long,
    container: AppContainer,
    onBack: () -> Unit,
    onRequestKeySetup: () -> Unit,
) {
    val viewModel: ChatViewModel = viewModel(
        key = "chat-$conversationId",
        factory = ChatViewModel.factory(container, conversationId),
    )
    val state by viewModel.uiState.collectAsState()

    // 增量 A3：直接读仓库里的密钥流，不改动 ChatViewModel 的既有状态组合。
    val apiKey by container.settingsRepository.apiKey.collectAsState()
    val hasApiKey = !apiKey.isNullOrBlank()
    // 增量 C：聊天体验设置（即时生效）。
    val experience by container.settingsRepository.chatExperience.collectAsState()

    var input by remember { mutableStateOf("") }
    var showPromptSheet by remember { mutableStateOf(false) }
    var pendingPrompt by remember { mutableStateOf<PromptTemplate?>(null) }

    // ---- 1.2：图片附件 ----------------------------------------------------
    var attachedPath by remember { mutableStateOf<String?>(null) }
    var attachedBytes by remember { mutableStateOf(0L) }
    var imageBusy by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            imageBusy = true
            scope.launch {
                when (val result = ImageStore.import(appContext, uri, ImageStore.DIR_CHAT)) {
                    is ImageStore.Result.Success -> {
                        ImageStore.deleteQuietly(attachedPath)
                        attachedPath = result.file.absolutePath
                        attachedBytes = result.bytes
                        imageError = null
                    }
                    is ImageStore.Result.Error -> imageError = when (result.failure) {
                        ImageStore.Failure.TOO_LARGE -> Strings.imageTooLarge(8)
                        ImageStore.Failure.UNSUPPORTED_FORMAT -> Strings.imageUnsupported()
                        ImageStore.Failure.DECODE_FAILED -> Strings.imageDecodeFailed()
                        ImageStore.Failure.IO_ERROR -> Strings.imageReadFailed()
                    }
                }
                imageBusy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundColor()),
    ) {
        ChatHeader(
            title = state.conversation?.title ?: Strings.tabConversations,
            promptName = state.activePrompt?.name ?: Strings.noPrompt,
            isStreaming = state.isStreaming,
            onBack = onBack,
            onSwitchPrompt = { showPromptSheet = true },
        )

        if (!hasApiKey) {
            ApiKeyMissingCard(
                onClick = onRequestKeySetup,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> LoadingBox(label = Strings.loading)
                state.messages.isEmpty() && !state.isStreaming -> EmptyState(
                    title = Strings.emptyChatTitle,
                    description = Strings.emptyChatDesc,
                    icon = Icons.Filled.Send,
                )
                else -> MessageList(
                    messages = state.messages,
                    streamingText = state.streamingText,
                    isStreaming = state.isStreaming,
                )
            }
        }

        state.error?.let { message ->
            ErrorBanner(
                message = message,
                onDismiss = viewModel::dismissError,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        imageError?.let { message ->
            ErrorBanner(
                message = message,
                onDismiss = { imageError = null },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        val activeConversation = state.conversation
        ChatInputBar(
            value = input,
            onValueChange = { input = it },
            isStreaming = state.isStreaming,
            sendOnEnter = experience.sendOnEnter,
            tokenSummary = if (experience.showTokenUsage && activeConversation != null) {
                Strings.tokenUsage(activeConversation.totalTokens, activeConversation.cacheHitRate)
            } else {
                null
            },
            attachedImage = attachedPath?.let { File(it) },
            attachedBytes = attachedBytes,
            imageBusy = imageBusy,
            onPickImage = {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemoveImage = {
                ImageStore.deleteQuietly(attachedPath)
                attachedPath = null
                attachedBytes = 0L
            },
            onSend = {
                val text = input
                val image = attachedPath
                input = ""
                attachedPath = null
                attachedBytes = 0L
                viewModel.sendMessage(text, image)
            },
            onStop = viewModel::stopGeneration,
        )
    }

    if (showPromptSheet) {
        PromptPickerSheet(
            currentPromptId = state.conversation?.promptId,
            prompts = state.prompts,
            onDismiss = { showPromptSheet = false },
            onPick = { picked ->
                showPromptSheet = false
                val currentId = state.conversation?.promptId
                when {
                    picked.id == currentId -> Unit
                    picked.resetContextOnSwitch -> pendingPrompt = picked
                    else -> viewModel.switchPrompt(picked, clearHistory = false)
                }
            },
        )
    }

    val promptToConfirm = pendingPrompt
    if (promptToConfirm != null) {
        AlertDialog(
            onDismissRequest = { pendingPrompt = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = Strings.switchPromptResetTitle,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = Strings.switchPromptResetMessage(promptToConfirm.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingPrompt = null
                        viewModel.switchPrompt(promptToConfirm, clearHistory = true)
                    },
                ) {
                    Text(text = Strings.clearHistory, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingPrompt = null
                        viewModel.switchPrompt(promptToConfirm, clearHistory = false)
                    },
                ) {
                    Text(Strings.keepHistory)
                }
            },
        )
    }
}

@Composable
private fun ChatHeader(
    title: String,
    promptName: String,
    isStreaming: Boolean,
    onBack: () -> Unit,
    onSwitchPrompt: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = Strings.back,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isStreaming) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 44.dp),
        ) {
            PromptTag(
                text = promptName,
                emphasised = true,
                onClick = onSwitchPrompt,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Strings.switchPromptHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<Message>,
    streamingText: String,
    isStreaming: Boolean,
) {
    val visible = remember(messages) { messages.filter { it.role != MessageRole.SYSTEM } }

    // 1.3：横屏/平板下把消息列限制在 CHAT_CONTENT_MAX_WIDTH 内并居中，
    // 避免气泡被拉到屏幕两端、中间大片空白；竖屏（<960dp）宽度不受影响。
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // reverseLayout keeps the newest message pinned to the bottom, so streaming
        // output is always visible without manual scroll bookkeeping.
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = CHAT_CONTENT_MAX_WIDTH),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            reverseLayout = true,
        ) {
        if (isStreaming) {
            item(key = "streaming-bubble") {
                StreamingBubble(text = streamingText)
            }
        }
        items(items = visible.asReversed(), key = { message -> message.id }) { message ->
            MessageBubble(message = message)
        }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = if (isUser) {
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 8.dp, bottomStart = 20.dp)
            } else {
                RoundedCornerShape(topStart = 8.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
            },
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            shadowElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = bubbleMaxWidth()),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // 1.2：用户消息若带图，先把图显示出来
                val attached = message.imagePath
                if (attached != null) {
                    val bitmap = remember(attached) {
                        runCatching { android.graphics.BitmapFactory.decodeFile(attached) }.getOrNull()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(MaterialTheme.shapes.medium),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (message.content.isNotBlank()) {
                    if (isUser) {
                        // 用户消息保持纯文本：输入的是原文，不该被当成 Markdown 解释
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        // 助手消息：流式结束后再做一次完整 Markdown 渲染
                        MarkdownText(markdown = message.content)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(text: String) {
    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            cursorVisible = !cursorVisible
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = bubbleMaxWidth()),
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = if (text.isEmpty()) Strings.thinking else text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (cursorVisible) "▍" else " ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isStreaming: Boolean,
    sendOnEnter: Boolean,
    tokenSummary: String?,
    attachedImage: File?,
    attachedBytes: Long,
    imageBusy: Boolean,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val canSend = value.isNotBlank() || attachedImage != null

    // 1.3：输入区同样限制内容宽度并居中，和消息列对齐；insets 仍由外层承担。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = CHAT_CONTENT_MAX_WIDTH)
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 12.dp),
    ) {
        // 1.2：已选图片的预览条（发送前可移除）
        if (attachedImage != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val preview = remember(attachedImage.absolutePath) {
                        runCatching {
                            android.graphics.BitmapFactory.decodeFile(attachedImage.absolutePath)
                        }.getOrNull()
                    }
                    if (preview != null) {
                        Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = Strings.imageAttached(attachedBytes / 1024),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onRemoveImage, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = Strings.removeImage,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            // 1.2：添加图片
            IconButton(
                onClick = onPickImage,
                enabled = !imageBusy && !isStreaming,
                modifier = Modifier.size(44.dp),
            ) {
                if (imageBusy) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = Strings.attachImage,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = Strings.inputHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                // 增量 C-1：回车发送 / 回车换行两种模式。
                keyboardOptions = if (sendOnEnter) {
                    KeyboardOptions(imeAction = ImeAction.Send)
                } else {
                    KeyboardOptions.Default
                },
                keyboardActions = if (sendOnEnter) {
                    KeyboardActions(onSend = { if (canSend) onSend() })
                } else {
                    KeyboardActions.Default
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        handleEnterKey(
                            event = event,
                            sendOnEnter = sendOnEnter,
                            canSend = canSend,
                            onSend = onSend,
                        )
                    },
            )
            Spacer(modifier = Modifier.width(10.dp))
            SendButton(
                isStreaming = isStreaming,
                canSend = canSend,
                onSend = onSend,
                onStop = onStop,
            )
        }

        // 增量 C-5：本会话 token 消耗与缓存命中率（开关关闭时不显示）。
        if (tokenSummary != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tokenSummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
    }
}

/** 1.3：横屏/平板下聊天内容列的最大宽度（超过则居中，不再拉满全屏）。 */
private val CHAT_CONTENT_MAX_WIDTH = 960.dp

/**
 * 1.3 横屏适配：气泡最大宽度随窗口变化，而不是写死 300dp。
 * 竖屏（<600dp）沿用接近原来的比例，横屏/折叠屏/平板按窗口比例放大并设上限，
 * 让长段落和表格有足够横向空间，同时不出现「一行只有几个字」的超宽气泡。
 */
@Composable
private fun bubbleMaxWidth(): Dp {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 900 -> minOf(widthDp.dp * 0.58f, 720.dp)
        widthDp >= 600 -> minOf(widthDp.dp * 0.66f, 620.dp)
        else -> minOf(widthDp.dp * 0.86f, 420.dp)
    }
}
/**
 * 增量 C-1：
 * - 回车发送模式：Enter 发送，Shift+Enter 换行。
 * - 回车换行模式：Enter 换行，Ctrl+Enter 发送。
 */
private fun handleEnterKey(
    event: KeyEvent,
    sendOnEnter: Boolean,
    canSend: Boolean,
    onSend: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
    if (!isEnter) return false

    val withCtrl = event.isCtrlPressed
    val withShift = event.isShiftPressed
    val shouldSend = if (sendOnEnter) {
        !withCtrl && !withShift
    } else {
        withCtrl
    }
    if (!shouldSend) return false
    if (canSend) onSend()
    // 即使输入为空也吃掉这次回车，避免多插一个空行。
    return true
}

@Composable
private fun SendButton(
    isStreaming: Boolean,
    canSend: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val enabled = isStreaming || canSend
    val container = if (isStreaming) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val content = if (isStreaming) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Surface(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled) {
                if (isStreaming) onStop() else onSend()
            },
        shape = CircleShape,
        color = if (enabled) container else container.copy(alpha = 0.35f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isStreaming) Icons.Filled.Close else Icons.Filled.Send,
                contentDescription = if (isStreaming) Strings.stop else Strings.send,
                tint = content,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptPickerSheet(
    currentPromptId: Long?,
    prompts: List<PromptTemplate>,
    onDismiss: () -> Unit,
    onPick: (PromptTemplate) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = Strings.switchPromptTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Strings.switchPromptSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                prompts.forEach { prompt ->
                    HarmonyCard(
                        modifier = Modifier.padding(bottom = 12.dp),
                        onClick = { onPick(prompt) },
                        containerColor = if (prompt.id == currentPromptId) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prompt.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = prompt.summary(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (prompt.id == currentPromptId) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

