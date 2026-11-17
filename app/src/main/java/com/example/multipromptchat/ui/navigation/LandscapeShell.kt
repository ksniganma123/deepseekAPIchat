package com.example.multipromptchat.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.ui.Strings
import com.example.multipromptchat.ui.chat.ChatScreen
import com.example.multipromptchat.ui.conversations.ConversationListScreen
import com.example.multipromptchat.ui.prompts.PromptEditScreen
import com.example.multipromptchat.ui.prompts.PromptListScreen
import com.example.multipromptchat.ui.settings.SettingsScreen

/**
 * 1.3：微信桌面式的横屏三栏布局。
 *
 * ```
 * ┌────────┬──────────────┬────────────────────────────┐
 * │ 选项    │ 当前选项的列表页 │ 右侧详情：会话 / 人设编辑   │
 * │ 列表    │              │ 未选择时显示占位提示         │
 * └────────┴──────────────┴────────────────────────────┘
 *      左 + 中 ≈ 总宽 1/3              右 ≈ 2/3
 * ```
 *
 * 只在「够宽的横屏」（宽度 ≥ [LANDSCAPE_MIN_WIDTH_DP]）启用；手机横屏仍走竖屏那套
 * 单页 + 底部导航，避免三栏各自只剩几个字宽。
 */
const val LANDSCAPE_MIN_WIDTH_DP = 700

private data class RailTab(val route: String, val label: String, val icon: ImageVector)

private val railTabs = listOf(
    RailTab(Routes.CONVERSATIONS, Strings.tabConversations, Icons.Filled.MailOutline),
    RailTab(Routes.PROMPTS, Strings.tabPrompts, Icons.Filled.Face),
    RailTab(Routes.SETTINGS, Strings.tabSettings, Icons.Filled.Settings),
)

private val RailWidth = 96.dp

@Composable
fun LandscapeShell(container: AppContainer) {
    var currentTab by rememberSaveable { mutableStateOf(Routes.CONVERSATIONS) }
    var openConversationId by rememberSaveable { mutableStateOf(-1L) }
    var openPromptId by rememberSaveable { mutableStateOf(-1L) } // -2 = 新建

    Surface(color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            // 目标比例：左栏 + 中栏 = 总宽 1/3，右侧详情 = 2/3。
            // 左栏固定 96dp，中栏用「总宽三分之一减去左栏」精确补齐；窗口过窄时给 200dp 下限。
            val middleWidth = (maxWidth / 3 - RailWidth).coerceAtLeast(180.dp)
            val rightWidth = (maxWidth - RailWidth - middleWidth).coerceAtLeast(0.dp)

            Row(modifier = Modifier.fillMaxSize()) {
                Rail(
                    currentTab = currentTab,
                    onSelect = { route -> currentTab = route },
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                when (currentTab) {
                    Routes.PROMPTS -> {
                        Box(modifier = Modifier.width(middleWidth).fillMaxHeight()) {
                            PromptListScreen(
                                container = container,
                                onCreatePrompt = { openPromptId = NEW_PROMPT_ID },
                                onEditPrompt = { id -> openPromptId = id },
                                compact = true,
                            )
                        }
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Box(modifier = Modifier.width(rightWidth).fillMaxHeight()) {
                            if (openPromptId == NO_SELECTION) {
                                PanePlaceholder(Strings.detailPickPrompt)
                            } else {
                                PromptEditScreen(
                                    container = container,
                                    promptId = openPromptId.takeIf { it != NEW_PROMPT_ID },
                                    onDone = { openPromptId = NO_SELECTION },
                                )
                            }
                        }
                    }

                    Routes.SETTINGS -> {
                        // 设置没有"详情"，直接占满中 + 右，避免留一块空面板。
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            SettingsScreen(container = container)
                        }
                    }

                    else -> {
                        Box(modifier = Modifier.width(middleWidth).fillMaxHeight()) {
                            ConversationListScreen(
                                container = container,
                                onOpenConversation = { id -> openConversationId = id },
                                onNavigateToPrompts = { currentTab = Routes.PROMPTS },
                                compact = true,
                            )
                        }
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Box(modifier = Modifier.width(rightWidth).fillMaxHeight()) {
                            if (openConversationId <= 0L) {
                                PanePlaceholder(Strings.detailPickConversation)
                            } else {
                                ChatScreen(
                                    conversationId = openConversationId,
                                    container = container,
                                    // 横屏下「返回」只清空右栏，不涉及返回栈
                                    onBack = { openConversationId = NO_SELECTION },
                                    onRequestKeySetup = { currentTab = Routes.SETTINGS },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val NO_SELECTION = -1L
private const val NEW_PROMPT_ID = -2L

@Composable
private fun Rail(
    currentTab: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(RailWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        railTabs.forEach { tab ->
            val selected = tab.route == currentTab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .clickable { onSelect(tab.route) }
                    .padding(vertical = 10.dp),
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PanePlaceholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
