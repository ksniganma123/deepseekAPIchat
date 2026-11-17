package com.example.multipromptchat.ui.navigation

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.asImageBitmap
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.multipromptchat.AppContainer
import com.example.multipromptchat.ui.chat.ChatScreen
import com.example.multipromptchat.ui.conversations.ConversationListScreen
import com.example.multipromptchat.ui.onboarding.OnboardingScreen
import com.example.multipromptchat.ui.prompts.PromptEditScreen
import com.example.multipromptchat.ui.prompts.PromptListScreen
import com.example.multipromptchat.ui.settings.SettingsScreen
import com.example.multipromptchat.ui.theme.LocalWallpaperActive

object Routes {
    const val CONVERSATIONS = "conversations"
    const val PROMPTS = "prompts"
    const val SETTINGS = "settings"

    const val CHAT = "chat"
    const val PROMPT_NEW = "prompt_new"
    const val PROMPT_EDIT = "prompt_edit"

    fun chat(conversationId: Long): String = "$CHAT/$conversationId"

    fun promptEdit(promptId: Long): String = "$PROMPT_EDIT/$promptId"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && currentRoute in bottomTabRoutes

    // 增量 A2（大屏版）：首次启动、且确实没配密钥时，全屏引导盖住整个界面。
    // 等密钥读取完成后才判断，避免冷启动瞬间误弹。
    val onboardingDone by container.settingsRepository.onboardingDone.collectAsState()
    val apiKeyLoaded by container.settingsRepository.apiKeyLoaded.collectAsState()
    val apiKey by container.settingsRepository.apiKey.collectAsState()
    var manualOnboarding by remember { mutableStateOf(false) }
    val showOnboarding = manualOnboarding ||
        (apiKeyLoaded && !onboardingDone && apiKey.isNullOrBlank())

    // 1.2：自定义背景图。文件缺失 / 解码失败时自动退回纯色背景。
    val appearance by container.settingsRepository.appearance.collectAsState()
    val wallpaper = remember(appearance.backgroundPath) {
        appearance.backgroundPath?.let { path ->
            runCatching { android.graphics.BitmapFactory.decodeFile(path) }.getOrNull()
        }
    }
    val hasWallpaper = wallpaper != null

    CompositionLocalProvider(LocalWallpaperActive provides hasWallpaper) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (wallpaper != null) {
                androidx.compose.foundation.Image(
                    bitmap = wallpaper.asImageBitmap(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // 蒙层：保证卡片与文字在任何图片上都可读。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.82f)),
                )
            }

            // 1.3：够宽的横屏切三栏（左选项 / 中列表 / 右详情），竖屏与手机横屏保持原布局。
            val configuration = LocalConfiguration.current
            val isWideLandscape = configuration.screenWidthDp > configuration.screenHeightDp &&
                configuration.screenWidthDp >= LANDSCAPE_MIN_WIDTH_DP

            if (isWideLandscape) {
                LandscapeShell(container = container)
            } else {
            Scaffold(
                containerColor = if (hasWallpaper) {
                    androidx.compose.ui.graphics.Color.Transparent
                } else {
                    MaterialTheme.colorScheme.background
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showBottomBar) {
                        BottomBar(
                            currentRoute = currentRoute,
                            onSelect = { route ->
                                if (route != currentRoute) {
                                    navController.navigate(route) {
                                        popUpTo(Routes.CONVERSATIONS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }
                },
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.CONVERSATIONS,
                    // 1.3：横屏时系统栏/刘海在左右两侧，这里统一吃掉水平安全区；
                    // 竖屏时左右 inset 为 0，等同原行为。
                    modifier = Modifier
                        .padding(padding)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                        ),
                ) {
                composable(Routes.CONVERSATIONS) {
                    ConversationListScreen(
                        container = container,
                        onOpenConversation = { conversationId ->
                            navController.navigate(Routes.chat(conversationId))
                        },
                        onNavigateToPrompts = {
                            navController.navigate(Routes.PROMPTS) {
                                popUpTo(Routes.CONVERSATIONS) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }

                composable(Routes.PROMPTS) {
                    PromptListScreen(
                        container = container,
                        onCreatePrompt = { navController.navigate(Routes.PROMPT_NEW) },
                        onEditPrompt = { promptId ->
                            navController.navigate(Routes.promptEdit(promptId))
                        },
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(container = container)
                }

                composable(
                    route = "${Routes.CHAT}/{conversationId}",
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.LongType },
                    ),
                ) { entry ->
                    val conversationId = entry.arguments?.getLong("conversationId") ?: -1L
                    if (conversationId > 0L) {
                        ChatScreen(
                            conversationId = conversationId,
                            container = container,
                            onBack = { navController.popBackStack() },
                            // 聊天页的缺钥卡片直接拉起全屏引导，不再绕去设置页。
                            onRequestKeySetup = { manualOnboarding = true },
                        )
                    }
                }

                composable(Routes.PROMPT_NEW) {
                    PromptEditScreen(
                        container = container,
                        promptId = null,
                        onDone = { navController.popBackStack() },
                    )
                }

                composable(
                    route = "${Routes.PROMPT_EDIT}/{promptId}",
                    arguments = listOf(
                        navArgument("promptId") { type = NavType.LongType },
                    ),
                ) { entry ->
                    val promptId = entry.arguments?.getLong("promptId") ?: -1L
                    if (promptId > 0L) {
                        PromptEditScreen(
                            container = container,
                            promptId = promptId,
                            onDone = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
            }

        if (showOnboarding) {
            OnboardingScreen(
                container = container,
                onFinish = {
                    container.settingsRepository.setOnboardingDone(true)
                    manualOnboarding = false
                },
                onSkip = {
                    container.settingsRepository.setOnboardingDone(true)
                    manualOnboarding = false
                },
            )
        }
        }
    }
}
