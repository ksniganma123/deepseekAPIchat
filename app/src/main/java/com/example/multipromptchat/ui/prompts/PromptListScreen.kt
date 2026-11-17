package com.example.multipromptchat.ui.prompts

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.domain.PromptTemplate
import com.example.multipromptchat.domain.PromptUsage
import com.example.multipromptchat.ui.Strings
import com.example.multipromptchat.ui.theme.screenBackgroundColor
import com.example.multipromptchat.ui.components.EmptyState
import com.example.multipromptchat.ui.components.ErrorBanner
import com.example.multipromptchat.ui.components.HarmonyCard
import com.example.multipromptchat.ui.components.LoadingBox
import com.example.multipromptchat.ui.components.PageTitle
import com.example.multipromptchat.ui.components.PillButton
import com.example.multipromptchat.ui.components.PillVariant
import com.example.multipromptchat.ui.components.PromptTag

@Composable
fun PromptListScreen(
    container: AppContainer,
    onCreatePrompt: () -> Unit,
    onEditPrompt: (Long) -> Unit,
    /** 1.3：横屏窄栏下用紧凑表头（小标题、无副标题、隐藏「批量选择」入口） */
    compact: Boolean = false,
) {
    val viewModel: PromptListViewModel = viewModel(factory = PromptListViewModel.factory(container))
    val state by viewModel.uiState.collectAsState()

    var deleteTarget by remember { mutableStateOf<PromptTemplate?>(null) }
    var showBatchConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundColor())
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            if (state.selection.active) {
                // 增量 E：选择模式下的顶部区（已选数量 / 退出 / 全选 / 删除）
                PageTitle(
                    title = Strings.promptSelectedCount(state.selectedCount),
                    subtitle = if (compact) null else Strings.promptCount(state.prompts.size),
                    titleStyle = if (compact) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    action = {
                        PillButton(
                            text = Strings.promptSelectFinish,
                            onClick = viewModel::exitSelectionMode,
                            variant = PillVariant.Ghost,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PillButton(
                        text = if (state.allSelected) Strings.promptClearAll else Strings.promptSelectAll,
                        onClick = viewModel::toggleSelectAll,
                        variant = PillVariant.Outline,
                        enabled = state.prompts.isNotEmpty(),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    PillButton(
                        text = if (state.deleting) Strings.promptDeleting else Strings.promptBatchDelete,
                        onClick = { showBatchConfirm = true },
                        enabled = state.canDeleteSelected,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = Strings.promptSelectHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                PageTitle(
                    title = Strings.promptsTitle,
                    subtitle = if (compact) null else Strings.promptCount(state.prompts.size),
                    titleStyle = if (compact) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    action = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!compact) {
                                PillButton(
                                    text = Strings.promptSelectMode,
                                    onClick = viewModel::enterSelectionMode,
                                    variant = PillVariant.Ghost,
                                    enabled = state.prompts.isNotEmpty(),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            PillButton(
                                text = Strings.create,
                                onClick = onCreatePrompt,
                                leadingIcon = Icons.Filled.Add,
                            )
                        }
                    },
                )
            }
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
                state.prompts.isEmpty() -> EmptyState(
                    title = Strings.emptyPromptsTitle,
                    description = Strings.emptyPromptsDesc,
                    icon = Icons.Filled.Face,
                    action = {
                        PillButton(text = Strings.create, onClick = onCreatePrompt)
                    },
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.prompts, key = { usage -> usage.prompt.id }) { usage ->
                        PromptCard(
                            usage = usage,
                            compact = compact,
                            selectionMode = state.selection.active,
                            selected = usage.prompt.id in state.selection.ids,
                            onToggleSelected = { viewModel.toggleSelected(usage.prompt.id) },
                            onEdit = { onEditPrompt(usage.prompt.id) },
                            onDelete = { deleteTarget = usage.prompt },
                        )
                    }
                }
            }
        }
    }

    // 单条删除确认（原有路径）
    val target = deleteTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = Strings.deletePromptTitle, style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "「${target.name}」",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        viewModel.deletePrompt(target)
                    },
                ) {
                    Text(text = Strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(Strings.cancel) }
            },
        )
    }

    // 增量 E：批量删除二次确认
    if (showBatchConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchConfirm = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = Strings.promptBatchDeleteTitle,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column {
                    Text(
                        text = Strings.promptSelectedCount(state.selectedCount),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = Strings.promptBatchDeleteMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatchConfirm = false
                        viewModel.deleteSelected()
                    },
                ) {
                    Text(text = Strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchConfirm = false }) { Text(Strings.cancel) }
            },
        )
    }
}

@Composable
private fun PromptCard(
    usage: PromptUsage,
    compact: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    if (selectionMode) {
        HarmonyCard(
            onClick = onToggleSelected,
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    PromptCardBody(usage = usage, compact = compact)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })
            }
        }
    } else {
        HarmonyCard {
            PromptCardBody(usage = usage, compact = compact)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = Strings.edit)
                }
                TextButton(onClick = onDelete) {
                    Text(text = Strings.delete, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PromptCardBody(usage: PromptUsage, compact: Boolean) {
    Text(
        text = usage.prompt.name,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = usage.prompt.systemPrompt,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // 窄栏（横屏中栏）下少两行，避免卡片高得离谱
        maxLines = if (compact) 2 else 3,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(10.dp))

    // 标签行与用量文字分成两行：之前挤在同一个 Row 里用 weight(1f)，
    // 卡片变窄时用量文字会被压到 0 宽，变成「一列一个字」的竖排。
    // 极窄时标签行可横向滚动，不会被裁掉半截。
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PromptTag(text = "t=${formatParam(usage.prompt.temperature)}")
        PromptTag(text = "p=${formatParam(usage.prompt.topP)}")
        if (usage.prompt.resetContextOnSwitch) {
            PromptTag(text = "切人设清空")
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = Strings.usageCount(usage.conversationCount),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun formatParam(value: Float): String {
    val rounded = (value * 100).toInt() / 100f
    return if (rounded == rounded.toInt().toFloat()) {
        "${rounded.toInt()}.0"
    } else {
        rounded.toString()
    }
}
