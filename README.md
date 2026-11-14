# deepseekAPIchat

> 一个把「人设」做成一等公民的 Android DeepSeek 聊天客户端：**每个会话独立绑定一个人设，聊天中随时换，人设攒多了还能批量管。**

> ⚠️ 本项目由 **AI 辅助编码（AI Coding）** 完成，功能稳定性不作保证；与 DeepSeek 开放平台及深度求索公司无任何隶属关系。详见 [声明](#声明)。

技术栈：Kotlin · Jetpack Compose + Material 3 · Room · OkHttp + okhttp-sse · EncryptedSharedPreferences。单模块 `app`，包名 `com.example.multipromptchat`（可改，见 [改包名](#改包名)）。

---

## 项目简介

市面上的聊天客户端大多只有「一个全局人设」。如果你习惯同时用几种角色——写代码时要严谨的工程师、改简历时要挑剔的顾问、翻译时要零废话的译者——那就要么反复改全局提示词，要么开一堆 App。

deepseekAPIchat 把这件事拆成两层：

- **人设（Prompt 模板）** 是可复用的资产，能建、能改、能批量删；
- **会话** 绑定一个人设，并在切换时把「保留历史 / 清空历史」交给你决定。

于是切换角色只需要在聊天页点一下人设胶囊，而不是重开一个会话或改全局配置。

界面按 HarmonyOS 风格做成大圆角、柔和阴影、低饱和强调色与充足留白；**不使用华为商标、专有字体或专有图标**，图标全部来自 `androidx.compose.material:material-icons-core`。

### 素材与许可

- 代码以 **MIT License** 开源，见 [LICENSE](LICENSE)；App 内「设置 → 关于」也能直接读到协议文本。
- 启动图标使用社区同人「DeepSeek 鲸鱼娘」形象（素材文件署名 `vicineko`），版权归原作者所有；该同人形象与深度求索公司无任何关系，仅供个人构建使用，公开发布前请自行替换为自有素材或先取得原作者授权。

---

## 亮点特性

### 人设：会话级而不是全局级

- **每个会话绑定一个人设**，还有一份 `promptSnapshot`（绑定当时的人设内容），所以模板后来被改被删，历史会话的行为依然可解释。
- **会话内随时切换**：底部弹窗选人设，切完只影响后续请求的 system 指令。
- **切换时问你一句**：若该模板开了「切换时重置上下文」，会弹「保留历史 / 清空历史」；关掉则静默保留。
- **人设批量管理**：列表页「批量选择」→ 勾选 / 全选 → 一次删除，带二次确认；被会话占用的人设会被跳过，不会误删。

### 聊天

- **流式输出**逐字上屏，带闪烁光标；也可在设置里关掉，改成一次性返回完整回复。
- **随时叫停**：发送中按钮变「停止」，取消即中断 HTTP 连接，已收到的部分仍会存库。
- **失败有话说**：401 / 402 / 403 / 429 / 5xx、连接超时、DNS 失败都翻译成中文提示。
- **自动起标题**：首条消息后用一个极短请求生成 ≤10 字标题；失败就保留「新会话」，不打扰。
- **上下文节流**：只发送最近 N 条历史（5~50 可调），system 指令始终保留。
- **用量可见**：可选在输入框下方显示本会话累计 token 与缓存命中率。

### 隐私与本地化

- 会话、消息、人设全部存在本机 Room 数据库，**没有云同步、没有账号体系**。
- API Key 走 `EncryptedSharedPreferences`（Android Keystore 的 AES-256 主密钥），界面只显示脱敏形式（形如 `sk-ab••••••wxyz`）。
- `android:allowBackup="false"`，Key 不会被系统云备份带走；源码与日志里都不会出现明文 Key。

### 上手成本

- **首次启动全屏三步引导**：欢迎与核心亮点 → 看截图去开放平台拿密钥 → 当场粘贴、保存、测试连接。可「稍后再说」跳过。
- **测试连接**一键探活，直接告诉你「密钥无效」「余额不足」「网络不通」还是「服务暂时不可用」。
- 三 Tab 底部导航：会话 / Prompt / 设置；空态、错态、加载态齐备；支持深色模式（跟随系统 / 浅色 / 深色）。

---

## 安装与前置要求

### 前置要求

| 项目 | 要求 |
| --- | --- |
| 手机 | Android 8.0（API 26）及以上，详见 [系统要求](#系统要求) |
| DeepSeek 账号 | 需要可用的 API Key，且账户有少量余额（按量计费） |
| 安装方式 A | 直接把 APK 拷进手机安装（需允许「未知来源」） |
| 安装方式 B | 电脑 + `adb`：`adb install -r app-debug.apk` |

### 方式 A：用现成的 APK

仓库 `dist/` 下有一份 release 签名包：

```
dist/deepseekAPIchat-1.0.0.apk     # 约 8.6 MB，versionName 1.0.0
```

拷到手机 → 点击安装 → 桌面出现「deepseekAPIchat」图标。

### 方式 B：自己构建

```bash
git clone <your-repo-url> deepseekAPIchat
cd deepseekAPIchat

# Windows
.\gradlew.bat assembleDebug
# macOS / Linux
./gradlew assembleDebug
```

**预期结果**：`app/build/outputs/apk/debug/app-debug.apk` 生成，末尾打印 `BUILD SUCCESSFUL`。

安装到已连接设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

构建环境（JDK / SDK / Gradle 版本）见 [系统要求](#系统要求)。Gradle Wrapper 已随仓库提供，首次构建需要联网下载依赖。

---

## 快速上手

按顺序做完这 7 步即可开始聊天，每步都写了预期结果。

**1. 首次打开 App**

预期：出现全屏引导第一步，标题「欢迎使用 deepseekAPIchat」，能看到「多个人设，随时切换」「人设攒多了，还能批量管」两张卡。

**2. 点亮「开始配置」**

预期：进入第二步「三步拿到密钥」，里面有三张平台截图：登录后在左侧点 `API keys` → 点「创建 API key」并起名 → 创建成功后点「复制」。

**3. 拿一把密钥**

点「打开平台」按钮（或手动访问 `https://platform.deepseek.com/api_keys`），登录后创建 Key 并复制那串 `sk-` 开头的字符。
预期：浏览器打开开放平台；复制成功的 Key 类似 `sk-xxxxxxxx...`。

**4. 粘贴、保存、测试**

回到 App 的引导第三步，粘贴 Key → 点「保存」→ 点「测试连接」。
预期：出现「连接正常，可以开始聊天了」。若提示「密钥无效，请检查是否复制完整」，重新复制一次。

**5. 完成引导**

点「完成，开始聊天」。
预期：进入「会话」页，显示空态「还没有会话」。

**6. 新建会话并选人设**

点右上角「新建」→ 在弹窗里**必须**选一个人设 → 点「创建」。
预期：直接进入聊天页，顶部有个蓝色人设胶囊标签（首次启动预置了 3 个人设：通用助手 / 代码搭档 / 翻译润色）。

**7. 发第一条消息**

输入问题 → 点发送（或按回车）。
预期：回答逐字出现；发完后会话标题会被自动改成一个不超过 10 字的短标题，返回列表即可看到。

---

## 常用用法与配置说明

### 人设（Prompt 模板）怎么用

「Prompt」Tab 里可以新建、编辑、删除人设。每个模板包含：

| 字段 | 说明 | 界面文案 |
| --- | --- | --- |
| `name` | 模板名，会显示在会话列表的人设胶囊上 | 名称 |
| `systemPrompt` | 角色的 system 指令 | 角色设定 |
| `temperature` | 采样温度，0~2 | 回答风格 |
| `topP` | 核采样，0~1 | 多样性 |
| `resetContextOnSwitch` | 切到该模板时是否询问清空历史 | 切换会话时重置上下文 |

编辑页顶部有一张可折叠的「角色设定写法指引」，含 4 条要点和两组 ❌/✅ 对比；点 ✅ 卡片的「用这个示例」可一键填入（已有内容时会先确认）。

### 在会话里换人设

1. 聊天页顶部的蓝色人设胶囊 → 点一下。
2. 底部弹窗选另一个模板（当前项带 ✓）。
3. 如果新模板开了 `resetContextOnSwitch`，会问「要清空历史吗？」：
   - **清空历史**：消息全部删除，从这个角色重新开始；
   - **保留历史**：消息留着，只有后续请求的 system 指令换成新角色。

预期结果：顶部的胶囊标签立刻变成新人设；如果选了清空，消息列表清空。

### 批量删除人设

1. 「Prompt」Tab → 右上角「批量选择」。
2. 勾选条目（点整张卡或勾选框都行），或用「全选 / 取消全选」。
3. 顶部会显示「已选 N 项」；没选中任何条目时「删除选中」是禁用的。
4. 点「删除选中」→ 二次确认 → 确认。
5. 点「完成」可随时退出选择模式。

预期结果：删除成功的条目立即消失，计数同步更新；**仍被会话引用的人设会被跳过**，顶部提示会写明「已删除 X 个；Y 个被会话引用，已跳过」。

### 「聊天体验」设置（设置页 → 聊天体验）

| 设置 | 默认 | 作用 |
| --- | --- | --- |
| 发送快捷键 | 回车发送 | 回车发送模式：Enter 发送、Shift+Enter 换行；回车换行模式：Enter 换行、Ctrl+Enter 发送（Ctrl+Enter 需物理键盘或支持 Ctrl 的输入法） |
| 自动生成会话标题 | 开 | 首条消息后用一次极短请求生成 ≤10 字标题；失败保留默认标题 |
| 上下文消息条数 | 20（5~50） | 只把最近 N 条历史发给模型，system 指令始终保留，用来控 token |
| 流式输出 | 开 | 关闭后走非流式请求，一次性显示完整回复 |
| 显示 token 消耗与缓存命中率 | 关 | 开启后在输入框下方显示本会话累计 token 与缓存命中率 |

所有设置**改完立即生效，只影响后续请求**，不会改动已有会话数据。

### API 密钥管理

- 位置：设置页「连接码」分组。
- 粘贴新 Key → 「保存」；随时可「测试连接」；「清除」需要二次确认。
- 界面上只显示脱敏值，例如 `sk-ab••••••wxyz`；完整 Key 不会出现在日志或源码中。

### 主题

设置页「外观」：跟随系统 / 浅色 / 深色。所有颜色集中在 `ui/theme/`，页面里没有硬编码色值，切换后整屏即时生效。

### 数据在哪、怎么清

- 全部在本机：Room 数据库 `deepseekapichat.db`（会话、消息、人设）+ 加密偏好 `multipromptchat_secure`（密钥）。
- 卸载 App 会一并删除，且**没有云备份**（`allowBackup=false`），换机需要自己重新配密钥。
- 只想清空某段对话：会话列表卡片右上角菜单 → 删除会话（会连带删除其消息）。

### 网络与接口（想改的人看这里）

- Base URL：`https://api.deepseek.com`，Endpoint：`POST /chat/completions`
- 模型：`deepseek-chat`（常量在 `domain/Models.kt` 的 `DEFAULT_MODEL`）
- 请求体结构：

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

- 流式解析：`choices[0].delta.content` 追加；`reasoning_content` 入库但不展示；`[DONE]` 结束；空行 / 注释行 / 非法 JSON 一律忽略。
- 取消：Flow 被取消 → `callbackFlow` 的 `awaitClose` 关闭 `EventSource`，HTTP 请求立即中断。

### 改包名

1. `app/build.gradle.kts` 改 `namespace` 与 `applicationId`；
2. 重命名源码目录 `app/src/main/java/com/example/multipromptchat/`（Android Studio 的 Refactor → Rename 会同步 `package` 与 `import`）；
3. `AndroidManifest.xml` 里 `android:name=".App"` 这类相对名不用动。

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
./gradlew assembleRelease     # → app/build/outputs/apk/release/app-release.apk
```

- 没有 `keystore.properties` 时（新克隆、CI）release 任务照常成功，只是产出未签名的 `app-release-unsigned.apk`。
- **keystore 一定要备份**：换签名后已安装的 App 无法覆盖升级。
- release 默认 `isMinifyEnabled = false`；要开混淆请先确认 Room 生成的实现类与 OkHttp/okhttp-sse 未被裁掉。

---

## 常见问题

**Q：为什么一定要先配密钥才能聊天？**
聊天能力由 DeepSeek 提供，App 本身不带模型。密钥只存在本机，用于直接向 `api.deepseek.com` 发请求。

**Q：提示「密钥无效，请检查是否复制完整」怎么办？**
多半是复制时截断了。去开放平台重新复制（`sk-` 开头那整串），粘贴后点「保存」再「测试连接」。

**Q：提示「账户余额不足」？**
密钥本身有效，但账户没余额。DeepSeek 按量计费，充值少量金额即可用很久；App 内不做支付跳转。

**Q：人设删不掉，提示被会话引用？**
这是有意为之的保护：会话绑着这个模板，直接删会让历史会话的人设信息丢失。先去「会话」删掉或改绑那些会话，再回来删人设。

**Q：为什么有的会话能看到「切人设清空」标签？**
说明该模板开了 `resetContextOnSwitch`——切到它时会问你要不要清空历史。

**Q：输入框下方一直显示 Token 0？**
流式的 token 统计依赖服务端在流末尾返回 `usage`。若没返回，就统计不到；这不是记账错误，也不影响聊天。

**Q：回车怎么不是发送？**
在「设置 → 聊天体验 → 发送快捷键」里看当前模式。选「回车换行」时是 Enter 换行、**Ctrl+Enter** 发送（需要物理键盘或支持 Ctrl 的输入法）。

**Q：卸载重装后数据没了？**
是的。数据全在本机且 `allowBackup=false`，没有云同步。

**Q：构建报 KSP 版本不匹配 / Room 生成失败？**
KSP 必须与 Kotlin 严格配对（当前 `2.2.21` ↔ `2.2.21-2.0.5`）。若改用 kapt：

```kotlin
plugins { id("org.jetbrains.kotlin.kapt") }
dependencies { kapt("androidx.room:room-compiler:2.7.2") }
```

**Q：gradle 找不到 Android SDK？**
优先读 `local.properties` 的 `sdk.dir`，其次 `ANDROID_HOME` / `ANDROID_SDK_ROOT`。

**Q：`gradlew` 报 `gradle-wrapper.jar is missing`？**
说明 wrapper jar 被删了（部分 `.gitignore` 模板会误伤 `*.jar`）。执行一次 `gradle wrapper --gradle-version 8.13 --rerun-tasks`，或直接用 `gradle assembleDebug`。

### 已知限制

下面这些是**有意不做**的部分，不算 bug；遇到其它问题欢迎开 Issue。

- 只对接 DeepSeek 的 OpenAI 兼容接口，模型名固定为 `deepseek-chat`；没有多模型切换。
- `reasoning_content` 只落库、不渲染；没有 Markdown / 代码高亮，纯文本显示。
- 消息没有分页，超长会话会一次性读入内存；同一会话同时只能有一个进行中的请求。
- 无云同步 / 账号体系 / 导出备份，卸载即清空。
- 没有单元测试与 UI 测试；数据库版本 2，升级靠手写 Migration。
- 文案集中在 `ui/Strings.kt`，尚未迁到 `strings.xml`，暂不支持多语言。
- 引导里的三张平台截图是浏览器截图裁掉标签条后的版本，不是官方插图。

---

## 贡献指南

欢迎 Issue 与 PR。为了让改动好合并，麻烦守住下面几条：

**提交前**

1. 跑通 `./gradlew assembleDebug`（有签名配置的再跑 `assembleRelease`）。
2. 自查是否碰到了 [已知限制](#已知限制) 里明确说不做的范围。

**代码约定**

- 颜色只加在 `ui/theme/`，不在 Composable 里写死色值；需要新语义色就扩 `Theme.kt`。
- 面向用户的文案集中在 `ui/Strings.kt`（后续要 i18n 时整体搬进 `strings.xml`）。
- 不要引入新依赖、不要改 Gradle / AGP / Kotlin 版本、不要引 Hilt / Koin（当前是 Application 级手动 `AppContainer`）。
- 不要替换 `PromptRepository` / `ConversationRepository` 现有方法签名，需要新能力就加方法。
- **改动流式相关代码时必须保留两道收尾保险**：`DeepSeekClient` 在收到 `[DONE]` / `onClosed` 后 `close()`，以及 `ChatRepository` 上的 `.takeWhile { it !is StreamChunk.Done }`。删掉它们会出现「回复完了还在转圈」。
- 数据库当前 version = 2，改表结构请叠加 `Migration`，不要降版本号、也不要依赖破坏性迁移。

**Issue 请附上**

机型与 Android 版本、复现步骤、预期与实际结果；涉及请求失败的请说明错误提示原文（**不要贴完整 API Key**）。

**联系作者**

Bilibili 主页：<https://space.bilibili.com/3546855692568823>

**许可**

提交即表示同意你的贡献以 MIT 协议随本项目分发。

---

## 系统要求

### 运行要求（手机）

App 的安装下限是 **Android 8.0（API 26）**，来自 `app/build.gradle.kts` 的 `minSdk = 26`，并与实际 APK 清单一致（`minSdkVersion:'26'`）。

| 项目 | 最低 | 推荐 |
| --- | --- | --- |
| 系统 | **Android 8.0 / API 26** | Android 11+（API 30） |
| 内存 | 2 GB（能跑，但首次启动与长列表滚动会偏慢） | 3 GB 以上 |
| 存储 | 安装后约 25–35 MB，建议留 100 MB 余量 | — |
| CPU 架构 | 任意：arm64-v8a / armeabi-v7a / x86 / x86_64 均已打包 | — |
| 网络 | 可访问 `api.deepseek.com`（HTTPS，聊天必需） | 稳定的移动数据 / Wi-Fi |
| 其他 | 系统 Keystore（API 26 全都有）、一个浏览器（引导里「打开平台」用）、软键盘；要用 Ctrl+Enter 发送需物理键盘或支持 Ctrl 的输入法 | — |
| 屏幕 | 手机竖屏（约 360dp 宽）为设计基准 | — |

**为什么下限是 API 26**——代码里有两处硬依赖：

1. `ui/util/TimeFormat.kt` 直接使用 `java.time.*`（`Instant` / `ZoneId` / `DateTimeFormatter`），这些 API 从 API 26 才进系统；若要降到 API 24，需开启 core library desugaring。
2. 密钥存储用 `EncryptedSharedPreferences`，要求系统有可用的 Android Keystore；API 26 起 TEE 已是标配（无 TEE 的设备会退回软件实现，仍可用）。

Compose / OkHttp / Room 自身只要求 API 21+，限制不来自它们。

### 构建环境（电脑）

| 项目 | 要求 |
| --- | --- |
| JDK | 17 或 21（`JAVA_HOME` 指向 JDK，不是 JRE） |
| Android SDK | Platform 36、Build-Tools 36.x |
| Gradle | 8.13 及以上（AGP 8.13.2 要求 Gradle ≥ 8.13；Wrapper 已随仓库提供） |
| Kotlin / KSP | 2.2.21 / 2.2.21-2.0.5 |
| 其他 | 首次构建需联网下载依赖 |

`local.properties`（gitignore，不随仓库分发）：

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

### 实测依据与说明

```
minSdkVersion:'26'   targetSdkVersion:'36'   compileSdkVersion:'36'
APK 9.06 MB（解压后 24.7 MB：dex 23.2 MB / res 0.33 MB / native 36 KB）
dex 2 个；native 只有 libandroidx.graphics.path.so（4 个 ABI 各一份）
权限只有 INTERNET + ACCESS_NETWORK_STATE
```

dex 占了 23 MB，是 Compose 带来的体积，所以安装后占用明显大于 APK 本身。

> 以上运行要求为静态分析结论（清单 / APK 构成 / 依赖的 API 要求）；项目**尚未在真机上验证最低配置的实际流畅度**，也没有单元测试。若你打算面向 Android 8 低端机发布，建议先实测首屏与长会话滚动。

---

## 声明

1. **本项目是 AI Coding 项目。** 代码主要由 AI 辅助生成并迭代，作者只做了构建与静态校验（`assembleDebug` / `assembleRelease` 通过、清单与依赖经核对），**不对功能稳定性、正确性、可用性作任何保证**，也没有在大量机型上做过真机验证。请自行评估风险后使用，重要数据请自行备份。
2. **本项目与 DeepSeek / 深度求索公司无任何关系。** 这是一个第三方客户端，仅调用 DeepSeek 开放平台公开的 HTTP 接口；与 DeepSeek 开放平台、杭州深度求索人工智能基础技术研究有限公司及其关联方**不存在隶属、合作、授权或背书关系**。"DeepSeek" 等名称与商标归其各自权利人所有。
3. **图标是社区同人形象。** 启动图标使用社区同人「DeepSeek 鲸鱼娘」形象（素材文件署名 `vicineko`），版权归原作者所有；该同人形象同样**与深度求索公司无任何关系**，未经授权请勿用于商业用途。
4. **作者主页。** 本项目由个人维护，作者 Bilibili 主页：<https://space.bilibili.com/3546855692568823>。问题反馈与使用交流可到那里联系。

使用或分发本项目，即表示你已理解并接受以上说明；若不接受，请勿使用。
