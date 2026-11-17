package com.example.multipromptchat.ui.conversations

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.domain.ConversationSummary
import com.example.multipromptchat.domain.PromptTemplate
import com.example.multipromptchat.ui.Strings
import com.example.multipromptchat.ui.theme.screenBackgroundColor
import com.example.multipromptchat.ui.components.EmptyState
import com.example.multipromptchat.ui.components.ErrorBanner
import com.example.multipromptchat.ui.components.HarmonyCard
import com.example.multipromptchat.ui.components.LoadingBox
import com.example.multipromptchat.ui.components.PageTitle
import com.example.multipromptchat.ui.components.PillButton
import com.example.multipromptchat.ui.components.PromptTag
import com.example.multipromptchat.ui.components.SectionHeader
import com.example.multipromptchat.ui.util.TimeFormat

@Composable
fun ConversationListScreen(
    container: AppContainer,
    onOpenConversation: (Long) -> Unit,
    onNavigateToPrompts: () -> Unit,
    /** 1.3：横屏窄栏下用紧凑表头（小标题、无副标题），避免标题与按钮挤在一起 */
    compact: Boolean = false,
) {
    val viewModel: ConversationListViewModel =
        viewModel(factory = ConversationListViewModel.factory(container))
    val state by viewModel.uiState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ConversationSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<ConversationSummary?>(null) }
    var menuForId by remember { mutableStateOf(-1L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundColor())
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            PageTitle(
                title = Strings.conversationsTitle,
                subtitle = if (compact) null else Strings.conversationCount(state.summaries.size),
                titleStyle = if (compact) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                action = {
                    PillButton(
                        text = Strings.create,
                        onClick = {
                            if (state.prompts.isEmpty()) onNavigateToPrompts() else showCreateDialog = true
                        },
                        leadingIcon = Icons.Filled.Add,
                    )
                },
            )
        }

        if (state.message != null) {
            ErrorBanner(
                message = state.message.orEmpty(),
                onDismiss = viewModel::dismissMessage,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> LoadingBox()
                state.summaries.isEmpty() -> EmptyState(
                    title = Strings.emptyConversationsTitle,
                    description = Strings.emptyConversationsDesc,
                    icon = Icons.Filled.MailOutline,
                    action = {
                        PillButton(
                            text = if (state.prompts.isEmpty()) Strings.goCreatePrompt else Strings.create,
                            onClick = {
                                if (state.prompts.isEmpty()) onNavigateToPrompts() else showCreateDialog = true
                            },
                        )
                    },
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.summaries, key = { summary -> summary.conversation.id }) { summary ->
                        ConversationCard(
                            summary = summary,
                            menuExpanded = menuForId == summary.conversation.id,
                            onOpen = { onOpenConversation(summary.conversation.id) },
                            onMenuToggle = { expanded ->
                                menuForId = if (expanded) summary.conversation.id else -1L
                            },
                            onRename = { renameTarget = summary },
                            onDelete = { deleteTarget = summary },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateConversationDialog(
            prompts = state.prompts,
            onDismiss = { showCreateDialog = false },
            onCreate = { prompt, title ->
                showCreateDialog = false
                viewModel.createConversation(prompt, title) { conversationId ->
                    onOpenConversation(conversationId)
                }
            },
        )
    }

    val currentRename = renameTarget
    if (currentRename != null) {
        RenameConversationDialog(
            initialTitle = currentRename.conversation.title,
            onDismiss = { renameTarget = null },
            onConfirm = { title ->
                renameTarget = null
                viewModel.renameConversation(currentRename.conversation.id, title)
            },
        )
    }

    val currentDelete = deleteTarget
    if (currentDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(Strings.deleteConversationTitle, style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = Strings.deleteConversationMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        viewModel.deleteConversation(currentDelete.conversation.id)
                    },
                ) {
                    Text(Strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(Strings.cancel) }
            },
        )
    }
}

@Composable
private fun ConversationCard(
    summary: ConversationSummary,
    menuExpanded: Boolean,
    onOpen: () -> Unit,
    onMenuToggle: (Boolean) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    HarmonyCard(onClick = onOpen) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = summary.lastMessage?.takeIf { it.isNotBlank() } ?: Strings.noMessagesPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = TimeFormat.chatTimestamp(
                        summary.lastMessageAt ?: summary.conversation.updatedAt
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box {
                    IconButton(
                        onClick = { onMenuToggle(true) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = Strings.more,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { onMenuToggle(false) },
                    ) {
                        DropdownMenuItem(
                            text = { Text(Strings.rename) },
                            onClick = { onMenuToggle(false); onRename() },
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.delete) },
                            onClick = { onMenuToggle(false); onDelete() },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PromptTag(text = summary.promptName ?: Strings.promptDeleted)
        }
    }
}

@Composable
private fun CreateConversationDialog(
    prompts: List<PromptTemplate>,
    onDismiss: () -> Unit,
    onCreate: (PromptTemplate, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf(-1L) }
    val selected = prompts.firstOrNull { it.id == selectedId }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = Strings.newConversationTitle, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = Strings.conversationTitleLabel) },
                    placeholder = { Text(text = Strings.conversationTitlePlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = Strings.promptRequired)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    prompts.forEach { prompt ->
                        SelectablePromptRow(
                            prompt = prompt,
                            selected = prompt.id == selectedId,
                            onClick = { selectedId = prompt.id },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val chosen = selected
                    if (chosen != null) onCreate(chosen, title)
                },
                enabled = selected != null,
            ) {
                Text(Strings.create)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        },
    )
}

@Composable
private fun RenameConversationDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = Strings.renameConversationTitle, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank(),
            ) {
                Text(Strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        },
    )
}

@Composable
private fun SelectablePromptRow(
    prompt: PromptTemplate,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HarmonyCard(
        modifier = modifier,
        onClick = onClick,
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentPadding = PaddingValues(14.dp),
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
            if (selected) {
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
