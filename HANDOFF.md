# HANDOFF — 增量任务交接说明

> 本文件只记录**增量**内容。项目原有能力（多 Prompt 人设、流式聊天、会话管理、主题）见 `README.md`。

---

## 1. 本次增量做了什么

### A. API Key 配置引导

| 编号 | 内容 | 落地位置 |
| --- | --- | --- |
| A1 | 「测试连接」：用当前密钥发一次 `max_tokens=1` 的非流式请求，三态（测试中/成功/失败），失败按 401/402/429/网络/5xx 分成人话文案 | `DeepSeekClient.checkApiKey` → `ChatRepository.testConnection` → `SettingsViewModel.testConnection` → `SettingsScreen` 密钥卡内 + 全屏引导第 3 步 |
| A1 | 「清除密钥」二次确认 | `SettingsScreen`（`showClearConfirm` 本地状态 + AlertDialog，确认后才调原来的 `viewModel.clearApiKey()`） |
| A2 | **初次使用的全屏三步引导**：① **欢迎 + 这个软件是干什么的**（主标语点出「会话内换人设」，两张核心卖点卡分别讲「多人设随时切换」与「人设批量管理」，隐私/流式作为低对比小点）② 怎么拿到密钥（**三张真实截图**）③ 当场粘贴、保存、测试连接。完成/跳过都写入 `onboarding_done`，之后不再自动弹 | `ui/onboarding/OnboardingScreen.kt`；由 `AppNavHost` 以**覆盖层**形式挂在 Scaffold 之上（未改导航图） |
| A2 | 设置页仍保留内嵌的折叠式三步卡（默认展开、状态持久化），图片同样是真实截图 | `ui/settings/ApiKeyGuideCard.kt` + `ui/components/CollapsibleGuideCard.kt` |
| A2 | 引导截图共 3 张，压缩为 webp 放在 `res/drawable-nodpi/`（共 56 KB）：`guide_platform_home` / `guide_create_key_dialog` / `guide_key_created`，由 `ui/components/GuideScreenshot.kt` 统一渲染（圆角 + 描边 + 小字说明） | `res/drawable-nodpi/` |
| A3 | 未配置密钥时的中性引导卡（primaryContainer 配色，不是报错红）：聊天页 + 设置页。**聊天页的卡点击直接拉起全屏引导**（不再绕去设置页）；设置页的卡点击就地展开三步卡 | `ui/components/ApiKeyMissingCard.kt` |

### B. 人格设定写法指引

| 编号 | 内容 | 落地位置 |
| --- | --- | --- |
| B1 | 编辑页顶部可折叠指引卡（4 条要点 + 两组 ❌/✅ 对比），✅ 示例带「用这个示例」按钮；输入框非空时先弹覆盖确认 | `ui/prompts/PromptGuideCard.kt` + `PromptEditScreen`（`pendingExample` 本地状态） |
| B2 | 输入框下方实时字数「已写 N 字 · 写得越具体，回答越稳定。」；label 文案改为「角色设定」 | `PromptEditScreen` + `Strings.systemPromptLabel` |

### C. 聊天体验设置（设置页新增一组，全部可持久化、即时生效）

| 编号 | 设置项 | 实现要点 | 落地位置 |
| --- | --- | --- | --- |
| C-1 | 发送快捷键（单选，默认回车发送） | 回车发送模式：Enter 发送、Shift+Enter 换行；回车换行模式：Enter 换行、Ctrl+Enter 发送。用 `Modifier.onPreviewKeyEvent` 在输入框之前拦截，同时把 IME action 设成 Send | `SettingsScreen`（ChoiceChip）+ `ChatScreen.ChatInputBar` / `handleEnterKey` |
| C-2 | 自动生成会话标题（开关，默认开） | 首条消息后用一次极短的非流式请求生成 ≤10 字标题；失败/关闭/已有标题都不动，保留默认「新会话」。**原来是本地截断首句，已替换为 AI 生成** | `DeepSeekClient.generateTitle` → `ChatRepository.autoGenerateTitleIfNeeded` → `ChatViewModel.sendMessage`（单独协程，不阻塞回复） |
| C-3 | 上下文消息条数（滑块 5~50，默认 20） | 只把最近 N 条历史塞进 messages；system 提示词单独加、始终保留。存 Int，读取时再 coerce 一次 | `SettingsRepository.ChatExperienceSettings.contextMessageCount` + `ChatRepository.generateReply`（`.takeLast(N)`） |
| C-4 | 流式输出（开关，默认开） | 关闭时走 `complete()` 非流式请求，把整段回复作为单个 `Delta` 发出，UI 一次性显示（VM 无需改动） | `DeepSeekClient.complete` + `ChatRepository.generateReply` 分支 |
| C-5 | token 消耗与缓存命中率（开关，默认关） | 请求带 `stream_options.include_usage=true`；解析 `usage`（含 `prompt_cache_hit_tokens` / `prompt_cache_miss_tokens`），按会话累加到 4 个新列；聊天页输入框下方小字显示 | `SseEventParser.TokenUsage` + `ConversationDao.addTokenUsage` + `ChatScreen` |

配套：数据库升到 **v2**，新增 4 个 `INTEGER NOT NULL DEFAULT 0` 列，带手写 `MIGRATION_1_2`，已有会话数据不受影响。

### E. 人设批量删除（增量 E）

| 项 | 内容 |
| --- | --- |
| 交互 | 列表页右上「批量选择」进入选择模式 → 卡片右上出现勾选框、点卡片也可勾选 → 顶部显示「已选 N 项」、`全选/取消全选`、`删除选中`（未选中时禁用）→ 删除前弹二次确认（显示选中数量 + 「仍被会话引用的会被跳过」）→ 删除后清空选中并退出选择模式，列表与计数自动刷新 |
| 组件 | `PromptListScreen`：选择模式顶部区（`PageTitle` + 两个 `PillButton` + 提示）、`PromptCard` 增加 `selectionMode/selected/onToggleSelected`（选择模式下 `HarmonyCard(onClick)` + `Checkbox`，普通模式保留原「编辑/删除」按钮）、新增批量确认 `AlertDialog`；卡片正文抽成 `PromptCardBody` 避免两套重复 |
| 状态 | `PromptListUiState` 新增 `selection: PromptSelection(active, ids)`、`deleting`，派生 `selectedCount / allSelected / canDeleteSelected`；VM 内部新增 `selection`、`deleting` 两个 `MutableStateFlow`，`combine` 从 2 路变 4 路 |
| 接口 | `PromptRepository.deletePrompts(ids): BatchDeleteResult(deleted, blocked, failed)`（逐条复用 `deletePrompt`，所以「被引用则跳过」的保护同样生效）；`BatchDeleteResult` 新增；VM 新增 `enterSelectionMode / exitSelectionMode / toggleSelected / selectAll / clearSelection / toggleSelectAll / deleteSelected` |
| 防误删 | ① 必须显式进入选择模式；② 勾选操作永远不会触发删除；③ 删除需二次确认；④ 仍然走「被会话引用禁止删除」的既有保护，不提供强制删除；⑤ `deleting` 期间按钮禁用；⑥ 只删当前列表里仍存在的 id，避免残留 id 造成误删 |



| 项 | 内容 |
| --- | --- |
| 现象 | AI 回复已经打完，右上角转圈与气泡里的闪烁光标仍在；此时发送键还停在「停止」。 |
| 根因 | `DeepSeekClient.streamChat` 的 `callbackFlow` **只有出错时才 `close()`**：收到 `[DONE]` 和 `onClosed` 回调时都只 `trySend(Done)`，通道一直开着 → 上层 `collect` 永不返回 → `ChatViewModel` 的 `finally`（唯一清除 `isStreaming` / `streamingText` 的地方）永不执行。服务端保持长连接时尤其稳定复现。 |
| 修复 1（根因） | `onEvent` 收到 `SsePayload.Done` 时 `trySend(Done)` 后立即 `close()`；`onClosed` 里也补 `close()` 兜底。 |
| 修复 2（结构保险） | `ChatRepository.generateReply` 对上游加 `.takeWhile { it !is StreamChunk.Done }`：Done 一到就取消上游并结束收集，**不依赖通道是否被关闭**；收集结束后再补发 `Completed`（出错时不发）。 |
| 修复 3（清理保险） | `ChatViewModel` 的 `finally` 改成 `try { 落库 } finally { 清 streamingText / isStreaming }`，即使落库抛异常也一定清状态。 |
| 验证 | ① `assembleDebug` / `assembleRelease` 通过；② 用 kotlinc + kotlinx-coroutines 跑了最小复现脚本：旧写法（通道不关 + 直接 collect）1.5s 内无法结束＝`finished=false`，新写法 `close() on Done` 与 `takeWhile(Done)` 均 `finished=true cleanup=true`，`ALL CHECKS PASSED`；③ 真机手测清单见下。 |

### D. 缺陷修复：回复结束后「正在输入」不消失（bugfix）

见上表（现象 / 根因 / 三层修复 / 验证）。要改流式相关代码时，**必须**保留 `close()` 和 `takeWhile` 这两道防线。

### F. 1.2：外观自定义 + 图片对话

| 项 | 内容 |
| --- | --- |
| 版本 | versionCode 1 → 2，versionName 1.0.0 → 1.2 |
| 外观-背景图 | 设置页选图 → `ImageStore.import(DIR_WALLPAPER, maxEdge=1600)` → 路径存 plain prefs → `AppNavHost` 里 `Image` + 0.82 透明蒙层铺底，Scaffold 与各页根背景转透明（`LocalWallpaperActive` / `screenBackgroundColor()`）。文件丢失/解码失败自动回落纯色 |
| 外观-主色调 | `AppearanceSettings.accentArgb`（默认 `DEFAULT_ACCENT_ARGB`）→ `MultipromptTheme(accentSeed)` → `deriveAccentTones()` 派生 primary/container（HSL，饱和度收在 0.25~0.75）。8 个预设色 + 色相滑杆，slider 每帧写入，实时生效 |
| 图片对话 | 输入框 ⨁ 选图 → 压缩落盘 `filesDir/chat_images` → `MessageEntity.imagePath`（DB v2→v3，新增 `image_path TEXT`，`MIGRATION_2_3`）→ 发送时 `ChatMessage.imageDataUrl` 转成 `image_url` 多模态数组；历史里的图按上下文一并重发 |
| 限制 | 原图 ≤ 8 MB、单条 1 张、压缩后 ≤ 1 MB / 最长边 1280；格式 JPEG/PNG/WebP（其它依赖系统解码）；是否识别取决于服务端模型能力，不支持时原样显示接口错误 |
| 模型自动获取 | 设置页「连接码」分组内：`DeepSeekClient.listModels()` 调 `GET /models` → `ChatRepository.fetchModels()` → `SettingsViewModel.fetchModels()`；选中项存 `SettingsRepository.modelName`（key `chat_model`，默认 `DEFAULT_MODEL`），聊天与自动标题都用它；id 含 flash/vl/vision 的模型打「可识图」标签，`deepseek-flash` 支持图片识别 |

### G. 聊天页 Markdown 渲染

| 项 | 内容 |
| --- | --- |
| 实现 | 自研轻量渲染：`ui/components/MarkdownParser.kt`（块级解析）+ `MarkdownText.kt`（渲染、行内样式、代码高亮） |
| 降级策略 | **流式中不渲染 Markdown**：`StreamingBubble` 继续用纯 `Text`；回复落库后由 `MessageBubble` 用 `MarkdownText` 做一次完整渲染。用户消息永远纯文本 |
| 代码块 | 等宽 + 轻量词法高亮（注释/字符串/数字/关键字，颜色取 `theme/Color.kt` 的 `Code*` 常量）+ `horizontalScroll` + 右上角「复制」（`LocalClipboardManager`） |
| 容错 | 解析异常/空结果 → 退回纯文本；未闭合的 ``` 也会被当成代码块吃掉（已用 14 条断言脚本验证） |
| 为什么不用库 | compose-markdown 在 JitPack（额外仓库）；multiplatform-markdown-renderer-m3 会引入 Compose Multiplatform material3，与 androidx 冲突 |

### H. 1.3：横屏 / 平板 / 折叠屏适配

| 项 | 内容 |
| --- | --- |
| 版本 | versionCode 2 → 3，versionName 1.2 → 1.3 |
| 根因 | 消息气泡写死 `widthIn(max = 300.dp)`，横屏不随窗口放大；Markdown 表格写死 132dp 列宽 + 内部横向滚动，被塞进 300dp 后挤压变形、正文提前折行 |
| 气泡宽度 | 新增 `bubbleMaxWidth()`（`ChatScreen.kt`）：按 `LocalConfiguration.screenWidthDp` 分档——<600dp 用 86%、600~899 用 66% 封顶 620dp、≥900 用 58% 封顶 720dp。竖屏 360dp 下算得 309dp，与原观感一致 |
| 内容列 | 聊天页消息列表与输入区限制在 `CHAT_CONTENT_MAX_WIDTH = 960.dp` 并居中（`Box(contentAlignment = Center)`），避免平板下气泡贴到屏幕两端 |
| 表格 | `MdTable` 改为列宽等权 `weight(1f)` + `fillMaxWidth`，列数 >4 才回退横向滚动；新增私有 `RowScope.MdTableCell` |
| 系统栏 | `AppNavHost` 的 NavHost 增加 `windowInsetsPadding(WindowInsets.safeDrawing.only(Horizontal))`，横屏侧边系统栏/刘海不再压住内容；竖屏左右 inset 为 0，行为不变 |
| 验证 | `assembleDebug` / `assembleRelease` 通过；按 360 / 600 / 800 / 1280dp 手算气泡与内容列宽度，均无溢出重叠；**未在折叠屏真机复验** |
### 引导的判定规则（重要）

```
自动全屏 = apiKeyLoaded && !onboardingDone && apiKey 为空
手动全屏 = 聊天页缺钥卡被点击
```

- 用 `apiKeyLoaded` 而不是裸判断 `apiKey == null`，避免冷启动瞬间（密钥还在解密）误弹。
- `onboardingDone` 在「完成」和「稍后再说」时都会置 true；此后只有点聊天页的缺钥卡才会再次全屏。
- 用户清空密钥后不会重新全屏（避免打扰），此时靠聊天页/设置页的中性卡兜底。

---

## 2. 文件分类

### 新增文件（7 个源码 + 3 张截图 + 本文件）

```
ui/onboarding/OnboardingScreen.kt            # 全屏三步引导（欢迎 → 拿密钥 → 粘贴验证），复用 SettingsViewModel
ui/components/CollapsibleGuideCard.kt        # 通用可折叠卡（A2 内嵌版 / B1 共用）
ui/components/GuideScreenshot.kt             # 引导截图统一渲染（圆角 + 描边 + caption）
ui/components/ApiKeyMissingCard.kt           # 中性缺钥卡（A3）
ui/settings/ApiKeyGuideCard.kt               # 设置页内嵌三步卡（A2）
ui/prompts/PromptGuideCard.kt                # 角色设定指引（B1）
HANDOFF.md

res/drawable-nodpi/guide_platform_home.webp      # ① 平台侧栏的「API keys」入口
res/drawable-nodpi/guide_create_key_dialog.webp  # ② 创建 API key 弹窗
res/drawable-nodpi/guide_key_created.webp        # ③ 创建成功后点「复制」
```

### 修改文件

| 文件 | 插入/新增位置 |
| --- | --- |
| `data/repository/SettingsRepository.kt` | ① `apiKey` 之后新增 `apiKeyLoaded` / `onboardingDone` 两个 StateFlow；② `init` 末尾补 3 行读 pref；③ `refreshApiKey()` 末尾置 `apiKeyLoaded = true`；④ `setPromptGuideCollapsed` 之后新增 `setOnboardingDone`；⑤ `companion object` 加 3 个 key。**密钥读写逻辑一行未动。** |
| `data/remote/DeepSeekClient.kt` | ① import 补 `DEFAULT_MODEL` / `Dispatchers` / `withContext` / `IOException`；② `StreamChunk` 之后新增 `KeyCheckResult`；③ `streamChat` 与 `buildRequest` 之间新增 `checkApiKey`。**流式解析未动。** |
| `data/repository/ChatRepository.kt` | `saveAssistantMessage` 之后新增 `testConnection(apiKey)`（一行转发）。**`generateReply` / 标题逻辑未动。** |
| `ui/settings/SettingsViewModel.kt` | ① 新增 `ConnectionTestState`；② `SettingsUiState` 加 `testState`；③ 构造参数加 `chatRepository`；④ `combine` 3 路→4 路；⑤ 新增 `testConnection()`；⑥ 保存/清除后重置 `testState`；⑦ factory 补参数。 |
| `ui/settings/SettingsScreen.kt` | PageTitle 后（缺钥卡）、密钥卡按钮行（测试连接 + 清除确认）、按钮行后（三态）、密钥卡后（内嵌引导卡）、文末（确认弹窗 + 私有 `TestStatusRow`）。**布局层级未改。** |
| `ui/chat/ChatScreen.kt` | ① import 补 `ApiKeyMissingCard`；② 签名加 `onRequestKeySetup: () -> Unit`；③ 读 `settingsRepository.apiKey`；④ `ChatHeader` 与消息 `Box` 之间插缺钥卡。**流式/输入栏一行未动。** |
| `ui/prompts/PromptEditScreen.kt` | 表单 Column 顶部（指引卡）、角色设定输入框下（字数行）、文末（覆盖确认）。**保存逻辑未动。** |
| `ui/navigation/AppNavHost.kt` | ① 新增 4 个引导相关状态；② 把 Scaffold 包进 `Box(fillMaxSize)`，在其后挂 `OnboardingScreen` 覆盖层；③ `ChatScreen` 调用处传 `onRequestKeySetup = { manualOnboarding = true }`。**导航图（6 条 route）未增删。** |
| `ui/Strings.kt` | 新增引导 / 测试连接 / 角色设定指引 / 全屏引导 / 聊天体验五组常量；**修改** 6 条既有文案（见冲突 4）。 |

### 增量 C 额外修改的文件

| 文件 | 改动 |
| --- | --- |
| `data/repository/SettingsRepository.kt` | 新增 `ChatExperienceSettings`（含 5 个字段）+ `chatExperience` StateFlow + `updateChatExperience()` + 5 个 pref key + `CONTEXT_MESSAGES_MIN/MAX/DEFAULT` 常量；`init` 里读默认值。 |
| `data/local/Entities.kt` / `Models.kt` / `Mappers.kt` | `ConversationEntity` / `Conversation` 新增 4 个 token 累计字段（`Conversation` 另加派生的 `totalTokens` / `cacheHitRate`）。 |
| `data/local/ConversationDao.kt` | 新增 `addTokenUsage()`（累加式 UPDATE）。 |
| `data/local/AppDatabase.kt` | version 1 → 2，新增手写 `MIGRATION_1_2` 并 `addMigrations(...)`。 |
| `data/remote/SseEventParser.kt` | 新增 `TokenUsage` 与 `SsePayload.Usage`；解析 `usage`，choices 为空时也返回 Usage。 |
| `data/remote/DeepSeekClient.kt` | `buildPayload` 增加 `stream` 参数并在流式时带 `stream_options.include_usage`；`StreamChunk.Usage`；新增 `complete()` / `generateTitle()` / `CompletionResult`。 |
| `data/repository/ChatRepository.kt` | `ChatStreamEvent.Usage`；按设置带 context 条数、分支流式/非流式、落库 usage、AI 标题生成；**删除**旧的本地标题截断。 |
| `ui/chat/ChatViewModel.kt` | 追加用户消息后用单独协程生成标题；`when` 补 `Usage` 分支（空实现）。 |
| `ui/chat/ChatScreen.kt` | 输入框回车拦截 + IME action；输入框下方 token 小字；读 `chatExperience`。 |
| `ui/settings/SettingsViewModel.kt` / `SettingsScreen.kt` | 暴露 `chatExperience` 与 5 个 setter；新增「聊天体验」分组 5 张卡（含新私有 `SwitchSettingRow`，`ThemeChip` 改名 `ChoiceChip` 复用）。 |

### 增量 E 额外修改的文件

| 文件 | 改动 |
| --- | --- |
| `data/repository/PromptRepository.kt` | 新增 `BatchDeleteResult` 与 `deletePrompts(ids)`（逐条复用 `deletePrompt`）。 |
| `ui/prompts/PromptViewModel.kt` | 新增 `PromptSelection`；`PromptListUiState` 加 `selection` / `deleting` 与三个派生属性；VM 加 7 个方法；`combine` 2 路 → 4 路；单条删除成功后补一条提示并顺带移出选中集合。 |
| `ui/prompts/PromptListScreen.kt` | 顶部区按模式切换；`PromptCard` 支持选择态；卡片正文抽成 `PromptCardBody`；新增批量确认弹窗。 |
| `ui/Strings.kt` | 新增「批量选择 / 全选 / 取消全选 / 删除选中 / 已选 N 项 / 批量删除确认 / 删除结果」等文案与 3 个格式化函数。 |

### 仅插入、未改逻辑的文件

- `ui/chat/ChatScreen.kt`（加卡片 + 一个回调参数）
- `ui/prompts/PromptEditScreen.kt`（加卡片、字数行、确认弹窗）
- `ui/navigation/AppNavHost.kt`（加覆盖层，未动 NavHost 内部结构）

---

## 3. 新增接口（下一会话可复用）

```kotlin
// SettingsRepository
val apiGuideCollapsed: StateFlow<Boolean>
fun setApiGuideCollapsed(collapsed: Boolean)      // 传 false = 强制展开
val promptGuideCollapsed: StateFlow<Boolean>
fun setPromptGuideCollapsed(collapsed: Boolean)
val apiKeyLoaded: StateFlow<Boolean>              // 密钥是否已从加密存储读过一次
val onboardingDone: StateFlow<Boolean>
fun setOnboardingDone(done: Boolean)

// DeepSeekClient / ChatRepository
suspend fun DeepSeekClient.checkApiKey(apiKey: String): KeyCheckResult
suspend fun ChatRepository.testConnection(apiKey: String): KeyCheckResult

sealed interface KeyCheckResult {
    Success / InvalidKey / NoBalance / RateLimited / NetworkError
    data class ServerError(val code: Int)
    data class Failed(val message: String)
}

// SettingsViewModel
fun testConnection()
sealed interface ConnectionTestState { Idle / Testing / Success / Failed(message) }

// 复用型 Composable
OnboardingScreen(container, onFinish, onSkip)
CollapsibleGuideCard(title, expanded, onToggle, modifier, subtitle, content)
ApiKeyMissingCard(onClick, modifier)
ApiKeyGuideCard(expanded, onToggle, modifier)
PromptGuideCard(expanded, onToggle, onUseExample, modifier)

// ---- 增量 C ----
data class ChatExperienceSettings(
    sendOnEnter: Boolean = true,
    autoGenerateTitle: Boolean = true,
    contextMessageCount: Int = 20,      // 5..50，读时再 coerce
    streamingOutput: Boolean = true,
    showTokenUsage: Boolean = false,
)
val SettingsRepository.chatExperience: StateFlow<ChatExperienceSettings>
fun SettingsRepository.updateChatExperience(transform: (ChatExperienceSettings) -> ChatExperienceSettings)

data class TokenUsage(promptTokens, completionTokens, cacheHitTokens, cacheMissTokens)
suspend fun DeepSeekClient.complete(...): CompletionResult          // 非流式
suspend fun DeepSeekClient.generateTitle(apiKey, userMessage): String?   // ≤10 字，失败返回 null
suspend fun ChatRepository.autoGenerateTitleIfNeeded(conversationId)
suspend fun ConversationDao.addTokenUsage(id, prompt, completion, hit, miss)

sealed interface StreamChunk { Content / Usage / Done / Error }       // 新增 Usage
sealed interface ChatStreamEvent { Delta / Usage / Completed / Failed }
Conversation.totalTokens / Conversation.cacheHitRate                  // 派生属性

// ---- 增量 E ----
data class PromptSelection(active: Boolean = false, ids: Set<Long> = emptySet())
data class BatchDeleteResult(deleted: Int, blocked: Int, failed: Int)   // blocked = 被会话引用跳过
suspend fun PromptRepository.deletePrompts(ids: List<Long>): BatchDeleteResult

// PromptListUiState 新增
selection: PromptSelection
deleting: Boolean
selectedCount / allSelected / canDeleteSelected                        // 派生

// PromptListViewModel 新增
enterSelectionMode() / exitSelectionMode() / toggleSelected(id)
selectAll() / clearSelection() / toggleSelectAll() / deleteSelected()
```

---

## 4. 与已有代码的冲突点（保留原逻辑，未删除重写）

1. **密钥输入框语义**：原有实现是「输入新值 → 保存」（`keyInput` 为空串，保存后清空），任务假设「读取现有密钥 + 脱敏显示」。二者共存：脱敏展示沿用 `maskApiKey`（`sk-ab••••••wxyz` 风格，不是 `sk-****abcd`）。
2. **聊天页原本没有通往引导的入口**：为不改导航结构，给 `ChatScreen` 新增参数 `onRequestKeySetup`，`AppNavHost` 里把 `manualOnboarding` 置 true 拉起覆盖层。**没有新增 route，也没有改 startDestination。**
3. **ChatViewModel 的状态组合**：`uiState` 由三层 `combine` 拼成，为不动流式逻辑，缺不缺密钥的判断放在 Composable 里直接 collect `settingsRepository.apiKey`，**没有改 ChatViewModel**。
4. **文案改动（会与旧截图不一致）**：`API KEY→连接码`、`DeepSeek API Key→DeepSeek 密钥`、`System Prompt→角色设定`（任务明确要求）、`temperature→回答风格`、`top_p→多样性`、`systemPromptRequired→角色设定不能为空`；另外 `switchPromptSubtitle`、`emptyPromptsDesc`、`noPromptsDesc`、`apiKeyMissing` 里的 "system 指令" 也改成了口语。要回退只改 `ui/Strings.kt`。
5. **「清除密钥」行为变了**：原来点一下直接清，现在多一层确认；清除动作仍是 `viewModel.clearApiKey()`。
6. **全屏引导是覆盖层，不是 route**：所以它不参与返回栈——第 1 步按返回键不会退出引导（第 2/3 步会回上一步，靠 `BackHandler`）。要退出只能点「稍后再说」或「完成」。
7. **会话标题策略变了（增量 C-2）**：原来是「本地截断首条消息前 18 字」，现在是「AI 生成 ≤10 字」，且受开关控制、失败就保持「新会话」。列表里的旧会话标题不受影响。
8. **数据库升到 v2**：`conversations` 多了 4 个 token 列。已写 `MIGRATION_1_2`，但**只在无模拟器环境里编译验证过、没跑过真实升级**；若要改表结构，必须继续叠加 migration，别把版本号降回去。
9. **单条删除多了一条提示（增量 E 顺带改）**：以前删成功后静默，现在会和批量删除一样弹「已删除 1 个人设」；删除逻辑本身没变。

---

## 5. 下一会话开始前建议先读

```
ui/Strings.kt                        # 所有文案的唯一来源
ui/onboarding/OnboardingScreen.kt    # 全屏引导；第 3 步复用 SettingsViewModel
ui/navigation/AppNavHost.kt          # 引导覆盖层的判定逻辑在这里（apiKeyLoaded / onboardingDone）
ui/settings/SettingsScreen.kt        # 密钥卡结构（测试连接三态 + 内嵌引导卡）
ui/settings/SettingsViewModel.kt     # ConnectionTestState 状态机
ui/prompts/PromptEditScreen.kt       # 指引卡插入点 + 覆盖确认
data/remote/DeepSeekClient.kt        # checkApiKey 与 streamChat 是两条独立路径
data/repository/SettingsRepository.kt  # 3 个折叠/引导 key + 密钥；注意 secure / plain 两个 prefs 的区分
```

## 6. 禁止改动（本增量划定的边界）

- **不要**改 `DeepSeekClient.streamChat` 与 `SseEventParser` 的解析逻辑（流式是本项目最脆的部分）。
- **不要**改 `ChatViewModel` 的 combine 结构与 `sendMessage` / `stopGeneration`。
- **不要**替换 `PromptRepository` / `ConversationRepository` 的既有方法签名；要新能力就加方法。
- **不要**把密钥写进 `Strings.kt`、日志或任何 Compose 代码；只走 `SettingsRepository`。
- **不要**给引导加 nav route（当前是 Box 覆盖层，加 route 会和 startDestination 逻辑打架）。
- **不要**给引导卡引入**新的**外部图片资源；引导已固定使用 `res/drawable-nodpi/` 里的 3 张截图（产品已确认），要换图就替换同名的 3 个文件，别新建第四张。
- **不要**新增依赖、改 Gradle / AGP / Kotlin 版本，不要引 Hilt/Koin。
- **不要**把颜色写进 Composable；需要新颜色就加到 `ui/theme/Theme.kt`（本次未新增颜色：成功态复用 `secondary`，失败态复用 `error`）。
- **不要**在消息层再累加一次 usage（会翻倍）：token 统计只在 `ChatRepository.persistUsage` 一处写入。
- **不要**把 `AppDatabase` 的 version 降回去或删掉 `MIGRATION_1_2`。
- **不要**把「聊天体验」的设置缓存到 Composable 本地状态里读——必须读 `settingsRepository.chatExperience`，否则改完不即时生效。

## 7. 编译结果（本会话实测）

```
> Task :app:assembleDebug      BUILD SUCCESSFUL
> Task :app:assembleRelease    BUILD SUCCESSFUL
```

- 过程中失败过一次：`Strings.deepseekApiKeysUrl` 漏写（Unresolved reference），补齐后通过。
- 增量 C 一次编译通过（含 Room v2 + migration）。
- 未做真机/模拟器运行验证（本机无模拟器），UI 与迁移升级均只做了静态审查。

## 8. 已知未做（可下个会话继续）

- 测试连接没有缓存结果，每次点都真发请求。
- 截图是浏览器截图裁掉标签条后的版本（保留地址栏便于确认站点）；若后续要做「离线也好看」的官方插图，需要另外设计。
- 全屏引导不做「重新温习」入口：用户完成后再想看，只能去设置页看内嵌卡。
- 引导期间按返回键不能退出第 1 步（设计如此，但可能有人觉得困住）。
- 未加多语言：文案仍集中在 `Strings.kt`。
- **增量 C**：流式下的 token 统计依赖 `stream_options.include_usage`，若服务端不返回 usage，输入框下方会一直显示 0；没有做「按会话重置统计」的入口。
- **增量 C**：非流式模式下「停止生成」等于直接丢弃整段回复（没有半成品可留），未做特殊提示。
- **增量 C**：标题生成是纯 fire-and-forget，用户看不到"正在起标题"，也失败无提示（按需求如此）。
- **D 的残留风险**：如果服务端既不发 `[DONE]` 也不关连接（极罕见），仍要靠 `readTimeout`（120s）兜底收尾，期间转圈会一直在；此时用户可以点「停止」立即收尾。
- **增量 E**：选择模式是临时状态，退出页面（VM 销毁）即丢失；没有「撤销删除」；没有跨页面的批量操作。
