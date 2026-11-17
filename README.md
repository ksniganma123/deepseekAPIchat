# deepseekAPIchat

> 一个把「人设」做成一等公民的 Android DeepSeek 聊天客户端：**每个会话独立绑定一个人设，聊天中随时换，人设攒多了还能批量管。**

> ⚠️ 本项目由 **AI 辅助编码（AI Coding）** 完成，功能稳定性不作保证；与 DeepSeek 开放平台及深度求索公司无任何隶属关系。详见 [声明](#声明)。

技术栈：Kotlin · Jetpack Compose + Material 3 · Room · OkHttp + okhttp-sse · EncryptedSharedPreferences。单模块 `app`，包名 `com.example.multipromptchat`。

**本文是使用手册**：按「你想做什么 → 怎么做 → 预期结果 → 注意什么」组织。需要改代码、看接口或自己编译，请直接跳到 [给开发者](#给开发者)。

如果你用的是更早的构建：1.0 没有图片对话、模型选择、Markdown 富文本与外观自定义；1.2 在横屏/平板上气泡宽度被写死、表格会被挤压。当前版本已修正。

---

## 这个 App 能做什么

大多数聊天客户端只有「一个全局人设」。如果你同时用几种角色——写代码时要严谨的工程师、改简历时要挑剔的顾问、翻译时要零废话的译者——那就得反复改全局提示词，或者开一堆 App。

这个 App 把这件事拆成两层：

- **人设（Prompt 模板）** 是可复用的资产：能建、能改、能批量删；
- **会话** 绑定一个人设，切换时把「保留历史 / 清空历史」交给你决定。

所以换角色只需要在聊天页点一下人设胶囊，不必重开会话、也不必改全局配置。除此之外：聊天支持流式输出与图片输入，回复按 Markdown 排版（代码块带高亮与复制），界面可以换背景图和主色调。

界面按 HarmonyOS 风格做成大圆角、柔和阴影、低饱和强调色与充足留白；**不使用华为商标、专有字体或专有图标**，图标全部来自 `androidx.compose.material:material-icons-core`。

### 素材与许可

- 代码以 **MIT License** 开源，见 [LICENSE](LICENSE)；App 内「设置 → 关于」也能直接读到协议文本。
- 启动图标使用社区同人「DeepSeek 鲸鱼娘」形象（素材文件署名 `vicineko`），版权归原作者所有；该同人形象与深度求索公司无任何关系，仅供个人构建使用，公开发布前请自行替换为自有素材或先取得原作者授权。

---

## 快速开始

五分钟走完，每步都写了预期结果。

**1. 安装**

- 用现成包：仓库的 Release 附件或 `dist/deepseekAPIchat-1.3.apk`（约 8.7 MB，`versionName 1.3` / `versionCode 3`）。拷进手机点击安装，需允许「未知来源」。
- 自己编译：见 [构建环境与命令](#构建环境与命令)。

**预期**：桌面出现「deepseekAPIchat」图标。

**2. 首次打开**

**预期**：出现全屏引导第一步，标题「欢迎使用 deepseekAPIchat」，能看到「多个人设，随时切换」「人设攒多了，还能批量管」两张卡。点「开始配置」进入第二步。

**3. 拿一把密钥**

点「打开平台」（或手动访问 `https://platform.deepseek.com/api_keys`），登录 → 创建 API Key → 复制 `sk-` 开头那串。

**预期**：浏览器打开开放平台；引导第二步里有三张截图分别对应「进 API keys 页面 → 创建 → 复制」。

**4. 粘贴并验证**

回到引导第三步粘贴 Key → 「保存」→ 「测试连接」。

**预期**：出现「连接正常，可以开始聊天了」。若提示「密钥无效，请检查是否复制完整」，重新复制一次即可。

**5. 新建会话并选人设**

「完成，开始聊天」进入会话页 → 右上角「新建」→ **必须**选一个人设 → 「创建」。

**预期**：直接进入聊天页，顶部有蓝色人设胶囊。首次启动预置了 3 个人设：通用助手 / 代码搭档 / 翻译润色。

**6. 发第一条消息**

输入问题 → 发送（或回车）。

**预期**：回答逐字出现；发完后会话标题会自动换成 ≤10 字的短标题，返回列表即可看到。

---

## 功能使用手册

### 人设（Prompt 模板）

**解决什么**：把「角色」从一次性提示词变成可复用资产，多个会话可以共用同一套设定。

**在哪用**：「Prompt」Tab。

**怎么用**

- 新建/编辑：填名称、角色设定（system 指令），再调两个可选项——「回答风格」（temperature，0~2，越高越发散）与「多样性」（top_p，0~1）。
- 写设定没头绪：编辑页顶部有可折叠的「角色设定写法指引」，含 4 条要点和两组 ❌/✅ 对比，点 ✅ 卡片的「用这个示例」可一键填入（输入框已有内容时会先确认）。
- 勾选「切换会话时重置上下文」：会话里切到该人设时，会问你要不要清空历史。
- **批量删除**：列表页右上「批量选择」→ 勾选或「全选」→ 顶部显示「已选 N 项」→「删除选中」→ 二次确认。没选中任何条目时按钮是禁用的；点「完成」可退出选择模式。

**注意**

- 人设被会话引用时**不能删**，会提示「被 N 个会话引用」——这是保护，避免历史会话丢失设定。
- 会话会记一份绑定当时的人设快照，所以模板后来被改被删，旧会话的行为仍然可解释。

### 会话

**怎么用**

- 新建：会话页右上「新建」→ 必选一个人设 → 可留空标题（发首条消息后自动生成 ≤10 字标题）。
- 改/删：卡片右上角「⋮」→ 重命名 / 删除（删除会连带清掉该会话全部消息，有二次确认）。
- 界面信息：卡片显示标题、最后一条消息预览、时间，以及绑定的人设胶囊标签。

**注意**：自动标题受「聊天体验 → 自动生成会话标题」开关控制；关掉后标题保持「新会话」，也可手动重命名。

### 聊天窗口

**怎么用**

- 发送：输入后点圆形发送键；输入中可按「停止」中断（已收到的部分仍会存下来）。
- 切换人设：点顶部蓝色人设胶囊 → 底部弹窗选人设 → 若新人设开了「切换时重置上下文」，会问「清空历史 / 保留历史」。
- 回复排版：助手回复按 Markdown 渲染——标题（6 级）、粗体/斜体/删除线、行内代码、有序与无序列表（支持嵌套缩进、按层级计号）、引用、分割线、表格。代码块带基础语法高亮、可横向滚动，右上角有「复制」。
- 流式输出中显示的是**纯文本**：半截 Markdown（``` 未闭合、表格没对齐）解析出来会乱，所以只在回复结束后做一次完整排版。你自己输入的消息也按纯文本显示。

**注意**

- 同一会话同时只能有一个进行中的请求。
- **横屏（窗口宽 ≥ 520dp 且宽大于高；平板、折叠屏、桌面窗口都算）切换为三栏**：最左是「会话 / Prompt / 设置」选项栏（96dp 固定），中栏是当前选项的列表（左 + 中合计约占总宽 1/3），右栏是详情。未选会话时右栏显示「请选择一个会话喵~」；点某个会话后右栏才加载聊天，聊天页的返回键只清空右栏。设置没有详情，直接占满中 + 右两栏。
- **窄窗口与竖屏（< 520dp 宽）保持原布局**：底部导航 + 整页。判断依据是「实测窗口尺寸」而非 `Configuration` 的屏幕尺寸，桌面自由窗口/分屏同样准确。
- 消息气泡宽度随窗口自适应（竖屏约 86%、横屏 66%、超大屏封顶 720dp），聊天内容列超过 960dp 时会居中；Markdown 表格按列等分宽度，不会再被挤成几列窄条。
- 失败会给出中文提示，不只是「出错了」：401 密钥无效、402 余额不足、403 无权限、429 太频繁、5xx 服务端错误、连接超时、DNS 失败。
- Markdown 不是完整 CommonMark：脚注、内嵌 HTML、任务列表不渲染。

### 发图片给 AI

**解决什么**：让模型看着图回答（截图报错、看表格、读手写笔记等）。

**怎么用**：输入框左侧 ⨁ → 系统图片选择器 → 选完在输入框上方出现缩略图与大小 → 可点 ✕ 移除 → 发送。也可以只发图不写字。

**限制**

| 项 | 规则 |
| --- | --- |
| 格式 | JPEG / PNG / WebP 明确支持；HEIC 等其它格式视系统能否解码，能则自动转 JPEG |
| 原图大小 | ≤ 8 MB，超过直接提示并拒绝 |
| 落地规格 | 自动压缩到最长边 1280、逐级降质直到 ≤ 1 MB 才发送与落库 |
| 数量 | 每条消息 1 张 |
| 前提 | 需要选**支持识图**的模型（如 `deepseek-flash`，见 [选择模型](#选择模型)） |

**注意**

- 图片存在 App 私有目录，消息只存路径；历史消息里的图会作为上下文一并重发，所以带图会话的 token 消耗会明显更高——可用「上下文消息条数」控制。
- 异常都有中文提示，不会静默丢图：超限、格式不支持、读取失败、解码失败；若是服务端拒绝图片，会原样显示接口返回的错误。

### 选择模型

**怎么用**：设置页「连接码」分组底部 → 保存密钥后点「获取可用模型」→ 应用用当前密钥请求 `GET /models`，列出你这账号实际可用的模型 → 点任意一行即切换（当前项打勾高亮），选择会持久化，聊天与自动标题都用它。

**注意**

- id 含 `flash` / `vl` / `vision` 的模型会打「可识图」标签；做图片对话请选这类。
- 若已保存的模型不在返回列表里，会自动选中第一项，避免用一个不存在的模型。
- 拉取失败（密钥错、网络不通、账号无模型）会在按钮下方给出原因。

### 外观

**在哪用**：设置页「外观」分组，三项都**改完立即生效，无需重启**：

1. **深浅色模式**：「跟随系统 / 浅色 / 深色」。跟随系统时会随系统深色设置自动切换。
2. **聊天背景图**：点「选择图片」→ 系统图片选择器（不需要存储权限）→ 选完自动压缩并铺满整屏，卡片与文字自动叠半透明蒙层保证可读；「移除背景」回到纯色。原图 ≤ 8 MB，自动缩到最长边 1600。
3. **主色调**：8 个预设色 + 色相滑杆，影响按钮、标签、选中态等强调色；「恢复默认」回到内置蓝（`#3A7AFE`）。自定义色会被收在低饱和区间（S 0.25~0.75），避免刺眼。

**注意**

- 背景图文件丢失或解码失败时**自动回落到纯色背景**，不会白屏。
- 自定义主色只影响强调色（primary 系）；中性色与错误色仍按主题固定值。

### 聊天体验设置

设置页「聊天体验」分组，全部即时生效、只影响后续请求：

| 设置 | 默认 | 作用 |
| --- | --- | --- |
| 发送快捷键 | 回车发送 | 回车发送模式：Enter 发送、Shift+Enter 换行；回车换行模式：Enter 换行、Ctrl+Enter 发送（Ctrl+Enter 需物理键盘或支持 Ctrl 的输入法） |
| 自动生成会话标题 | 开 | 首条消息后用一次极短请求生成 ≤10 字标题；请求失败时**自动退化为「首条消息截断」的本地标题**，不会一直停在「新会话」 |
| 上下文消息条数 | 20（5~50） | 只把最近 N 条历史发给模型，角色设定始终保留，用来控 token |
| 流式输出 | 开 | 关闭后改走非流式请求，一次性显示完整回复 |
| 显示 token 消耗与缓存命中率 | 关 | 开启后在输入框下方显示本会话累计 token 与缓存命中率 |

### 数据与隐私

- **密钥**：存在 `EncryptedSharedPreferences`（Android Keystore 的 AES-256 主密钥），界面只显示脱敏值（形如 `sk-ab••••••wxyz`）；源码、日志、git 里都不会出现明文。
- **会话与消息**：存在本机 Room 数据库 `deepseekapichat.db`；没有账号体系、没有云同步。
- `android:allowBackup="false"`：Key 不会被系统云备份带走，代价是卸载或换机后**数据不可恢复**。
- 想清空某段对话：会话列表卡片「⋮」→ 删除会话（连带删除消息）。

---

## 常见问题

**Q：为什么一定要先配密钥才能聊天？**
聊天能力由 DeepSeek 提供，App 本身不带模型。密钥只存在本机，用于直接向 `api.deepseek.com` 发请求。

**Q：提示「密钥无效，请检查是否复制完整」？**
多半是复制时截断了。回平台重新复制整串（`sk-` 开头），粘贴后「保存」再「测试连接」。

**Q：提示「账户余额不足」？**
密钥本身有效，但账户没余额。DeepSeek 按量计费，充值少量金额即可用很久；App 内不做支付跳转。

**Q：想发图片，但回复说看不懂图？**
先到设置页「获取可用模型」，选带「可识图」标签的模型（如 `deepseek-flash`）。文本模型会拒收图片，错误会原样显示。

**Q：人设删不掉，提示被会话引用？**
这是有意的保护。先去「会话」删掉或改绑那些会话，再回来删人设。

**Q：为什么有的会话显示「切人设清空」标签？**
说明该人设开了「切换时重置上下文」——切到它时会问你要不要清空历史。

**Q：输入框下方一直显示 Token 0？**
流式下的 token 统计依赖服务端在流末尾返回 `usage`；没返回就统计不到。不影响聊天。

**Q：回车怎么不是发送？**
看「设置 → 聊天体验 → 发送快捷键」当前模式。选「回车换行」时是 Enter 换行、**Ctrl+Enter** 发送。

**Q：卸载重装后数据没了？**
正常。数据全在本机且 `allowBackup=false`，没有云备份。

**Q：换手机怎么迁移？**
当前版本没有导出/导入功能，需要手动重建人设并重新配置密钥。

---

## 已知限制与边界

下面这些是**有意不做**或当前未做的部分，不算 bug：

- 只对接 DeepSeek 的 OpenAI 兼容接口，不做多供应商；Base URL 与默认模型是常量，可在源码里改。
- `reasoning_content`（模型的思考过程）只落库不展示。
- 消息没有分页，超长会话会一次性读入内存；同一会话同时只能有一个进行中的请求。
- 无云同步 / 账号体系 / 导出备份，卸载即清空。
- 没有单元测试与 UI 测试；数据库版本 3，升级靠手写 Migration。
- 文案集中在 `ui/Strings.kt`，尚未迁到 `strings.xml`，暂不支持多语言。
- Markdown 不是完整 CommonMark：脚注、内嵌 HTML、任务列表不支持。
- 图片对话每条消息 1 张；背景图在进入界面时于主线程解码（1600px 以内，通常 < 100ms）。
- 自定义主色调只影响强调色（primary 系）。
- 引导里的三张平台截图是浏览器截图裁掉标签条后的版本，不是官方插图。
- 项目未在真机上做完整回归，UI 细节以静态审查为主。

---

## 故障排查

面向自己编译/安装的情况；出现问题请带上机型、Android 版本、复现步骤与错误提示原文（**不要贴完整 API Key**）。

**构建报 KSP 版本不匹配 / Room 生成失败**
KSP 必须与 Kotlin 严格配对（当前 `2.2.21` ↔ `2.2.21-2.0.5`）。改用 kapt：

```kotlin
plugins { id("org.jetbrains.kotlin.kapt") }
dependencies { kapt("androidx.room:room-compiler:2.7.2") }
```

**gradle 找不到 Android SDK**
优先读 `local.properties` 的 `sdk.dir`，其次 `ANDROID_HOME` / `ANDROID_SDK_ROOT`。

**`gradlew` 报 `gradle-wrapper.jar is missing`**
说明 wrapper jar 被删（部分 `.gitignore` 模板会误伤 `*.jar`）。执行 `gradle wrapper --gradle-version 8.13 --rerun-tasks`，或直接用 `gradle assembleDebug`。

**新建会话聊完第一轮，标题还是「新会话」**
1.3 起标题生成有三段兜底：AI（system+采样参数的常规组合）→ AI 的无 system/无温度版本（适配 deepseek-reasoner 这类模型）→ 本地截断首条消息。若三条都没生效，先确认「设置 → 聊天体验 → 自动生成会话标题」是开的；仍然复现请反馈所用模型名。

**回复结束后「正在输入」不消失**
当前版本已在流式收尾处做了两道保险（收到 `[DONE]` 即关闭通道 + 收集端遇到结束标记即收尾），若仍复现请附上日志；期间可点「停止」强制收尾。

---

## 给开发者

### 项目结构

```
app/src/main/java/com/example/multipromptchat/
├── App.kt / MainActivity.kt / AppContainer.kt    # 入口与手写 DI 容器
├── domain/Models.kt                              # PromptTemplate / Conversation / Message + 常量
├── data/
│   ├── local/                                    # Room：AppDatabase(v3)、三个 DAO、实体、映射
│   ├── remote/                                   # DeepSeekClient（callbackFlow + okhttp-sse）、SseEventParser
│   ├── image/ImageStore.kt                       # 图片导入、压缩、data URL
│   └── repository/                               # Prompt / Conversation / Chat / Settings
└── ui/
    ├── theme/                                    # Color / Type / Shape / Theme / DynamicColor
    ├── components/                               # HarmonyCard、PillButton、引导卡、MarkdownText + Parser
    ├── onboarding/                               # 全屏三步引导
    ├── chat/ conversations/ prompts/ settings/    # 各页 Screen + ViewModel
    ├── navigation/                               # AppNavHost、BottomBar
    ├── Strings.kt                                # 全部文案常量
    └── util/TimeFormat.kt
```

### 接口与网络约定

- Base URL：`https://api.deepseek.com`；Endpoint：`POST /chat/completions`，模型列表用 `GET /models`。
- 默认模型：`deepseek-chat`（常量 `DEFAULT_MODEL`）；用户可在设置页切换。
- 请求体：

```json
{
  "model": "deepseek-chat",
  "stream": true,
  "stream_options": { "include_usage": true },
  "temperature": 0.7,
  "top_p": 1.0,
  "messages": [
    { "role": "system", "content": "<当前会话绑定的人设>" },
    { "role": "user", "content": "..." }
  ]
}
```

- 带图时该条消息的 `content` 变成数组：`[{"type":"text"...},{"type":"image_url","image_url":{"url":"data:image/jpeg;base64,..."}}]`。
- 流式解析：`choices[0].delta.content` 追加；`usage` 累加到会话；`[DONE]` 结束；空行 / 注释行 / 非法 JSON 一律忽略。
- 取消：Flow 被取消 → `callbackFlow` 的 `awaitClose` 关闭 `EventSource`，HTTP 请求立即中断。

### 构建环境与命令

需要 JDK 17 或 21、Android SDK Platform 36、Gradle 8.13+（Wrapper 已随仓库提供）。首次构建需联网下载依赖。

```bash
./gradlew assembleDebug        # Windows: .\gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` 里的 `sdk.dir` 指向本机 SDK（该文件已 gitignore）：

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

### Release 签名

签名信息从仓库根目录的 `keystore.properties` 读取（已 gitignore）：

```properties
storeFile=keystore/release.jks
storePassword=***
keyAlias=app
keyPassword=***
```

```bash
keytool -genkeypair -v -storetype PKCS12 -keystore keystore/release.jks \
  -alias app -keyalg RSA -keysize 2048 -validity 10000
./gradlew assembleRelease
```

- 没有 `keystore.properties` 时（新克隆、CI）release 照常成功，只是产出未签名的 `app-release-unsigned.apk`。
- **keystore 一定要备份**：换签名后已安装的 App 无法覆盖升级。
- release 默认 `isMinifyEnabled = false`；要开混淆请先确认 Room 生成的实现类与 OkHttp/okhttp-sse 未被裁掉。

### 改包名

1. `app/build.gradle.kts` 改 `namespace` 与 `applicationId`；
2. 重命名源码目录（Android Studio 的 Refactor → Rename 会同步 `package` 与 `import`）；
3. `AndroidManifest.xml` 里 `android:name=".App"` 这类相对名不用动。

### Markdown 渲染的取舍

没有引入第三方 Markdown 库，而是在 `ui/components/MarkdownText.kt` + `MarkdownParser.kt` 里自己实现：`compose-markdown` 托管在 JitPack（要多加一个仓库），`multiplatform-markdown-renderer-m3` 会拉 Compose Multiplatform 的 material3、与现有 androidx material3 有类冲突风险。

当前实现覆盖聊天需要的块级与行内语法（含嵌套列表按层计号、6 级标题），解析异常或空结果自动退回纯文本；需要完整 CommonMark 时可整体替换这一个组件。

### 贡献指南

欢迎 Issue 与 PR。提交前请跑通 `./gradlew assembleDebug`，并注意：

- 颜色只加在 `ui/theme/`，不在 Composable 里写死色值；新语义色扩 `Theme.kt`。
- 面向用户的文案集中在 `ui/Strings.kt`。
- 不要引入新依赖、不要改 Gradle / AGP / Kotlin 版本、不要引 Hilt / Koin（当前是 Application 级手动 `AppContainer`）。
- 不要替换 `PromptRepository` / `ConversationRepository` 现有方法签名，需要新能力就加方法。
- **改动流式相关代码时保留两道收尾保险**：`DeepSeekClient` 收到 `[DONE]` / `onClosed` 后 `close()`，以及 `ChatRepository` 上的 `.takeWhile { it !is StreamChunk.Done }`。删掉会出现「回复完了还在转圈」。
- 数据库当前 version = 3，改表结构请叠加 `Migration`，不要降版本号，也不要依赖破坏性迁移。
- 提交即表示同意你的贡献以 MIT 协议随本项目分发。

联系作者：Bilibili <https://space.bilibili.com/3546855692568823>

---

## 系统要求

### 手机

安装下限 **Android 8.0（API 26）**，来自 `minSdk = 26`，与实际 APK 清单一致（`minSdkVersion:'26'`）。

| 项目 | 最低 | 推荐 |
| --- | --- | --- |
| 系统 | **Android 8.0 / API 26** | Android 11+（API 30） |
| 内存 | 2 GB（能跑，但首次启动与长列表滚动会偏慢） | 3 GB 以上 |
| 存储 | 安装后约 25–35 MB，建议留 100 MB 余量 | — |
| CPU 架构 | 任意：arm64-v8a / armeabi-v7a / x86 / x86_64 均已打包 | — |
| 网络 | 可访问 `api.deepseek.com`（HTTPS，聊天必需） | 稳定的移动数据 / Wi-Fi |
| 其他 | 系统 Keystore（API 26 全都有）、一个浏览器（引导里「打开平台」用）、软键盘；要用 Ctrl+Enter 发送需物理键盘或支持 Ctrl 的输入法 | — |
| 屏幕 | 手机竖屏（约 360dp 宽）为设计基准 | — |

**为什么下限是 API 26**：① `ui/util/TimeFormat.kt` 直接用 `java.time.*`，这些 API 从 26 才进系统（降到 24 需开 core library desugaring）；② 密钥存储用 `EncryptedSharedPreferences`，需要可用的 Android Keystore，API 26 起 TEE 已是标配。Compose / OkHttp / Room 自身只要求 API 21+。

### 电脑（自己构建）

| 项目 | 要求 |
| --- | --- |
| JDK | 17 或 21（`JAVA_HOME` 指向 JDK，不是 JRE） |
| Android SDK | Platform 36、Build-Tools 36.x |
| Gradle | 8.13 及以上（AGP 8.13.2 要求 ≥ 8.13） |
| Kotlin / KSP | 2.2.21 / 2.2.21-2.0.5 |

### 实测依据

```
minSdkVersion:'26'   targetSdkVersion:'36'   compileSdkVersion:'36'
APK 8.69 MB（解压后约 24.7 MB：dex 约 23.2 MB / res 0.33 MB / native 36 KB）
dex 2 个；native 只有 libandroidx.graphics.path.so（4 个 ABI 各一份）
权限只有 INTERNET + ACCESS_NETWORK_STATE
```

dex 占了 23 MB 是 Compose 带来的体积，所以安装后占用明显大于 APK 本身。以上为静态分析结论，**尚未在真机上验证最低配置的实际流畅度**；面向 Android 8 低端机发布前建议实测首屏与长会话滚动。

---

## 声明

1. **本项目是 AI Coding 项目。** 代码主要由 AI 辅助生成并迭代，作者只做了构建与静态校验（`assembleDebug` / `assembleRelease` 通过、清单与依赖经核对），**不对功能稳定性、正确性、可用性作任何保证**，也没有在大量机型上做过真机验证。请自行评估风险后使用，重要数据请自行备份。
2. **本项目与 DeepSeek / 深度求索公司无任何关系。** 这是一个第三方客户端，仅调用 DeepSeek 开放平台公开的 HTTP 接口；与 DeepSeek 开放平台、杭州深度求索人工智能基础技术研究有限公司及其关联方**不存在隶属、合作、授权或背书关系**。"DeepSeek" 等名称与商标归其各自权利人所有。
3. **图标是社区同人形象。** 启动图标使用社区同人「DeepSeek 鲸鱼娘」形象（素材文件署名 `vicineko`），版权归原作者所有；该同人形象同样**与深度求索公司无任何关系**，未经授权请勿用于商业用途。
4. **作者主页。** 本项目由个人维护，作者 Bilibili 主页：<https://space.bilibili.com/3546855692568823>。问题反馈与使用交流可到那里联系。

使用或分发本项目，即表示你已理解并接受以上说明；若不接受，请勿使用。
