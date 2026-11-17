package com.example.multipromptchat.ui

/**
 * All user-facing copy lives here so the app can be localised later without
 * touching layout code (move these values into strings.xml when i18n starts).
 */
object Strings {

    // Tabs
    const val tabConversations = "会话"
    const val tabPrompts = "Prompt"
    const val tabSettings = "设置"

    // 1.3：横屏三栏的详情占位
    const val detailPickConversation = "请选择一个会话喵~"
    const val detailPickPrompt = "选择或新建一个人设喵~"

    // Common
    const val cancel = "取消"
    const val save = "保存"
    const val delete = "删除"
    const val rename = "重命名"
    const val edit = "编辑"
    const val confirm = "确定"
    const val back = "返回"
    const val create = "新建"
    const val unknownError = "出错了，请稍后再试"
    const val more = "更多操作"

    // Conversation list
    const val conversationsTitle = "会话"
    const val emptyConversationsTitle = "还没有会话"
    const val emptyConversationsDesc = "点右上角「新建」，选一个人设就能开始聊。"
    const val newConversationTitle = "新建会话"
    const val conversationTitleLabel = "标题（可选）"
    const val conversationTitlePlaceholder = "留空则自动命名"
    const val promptRequired = "请选择一个人设 Prompt"
    const val noMessagesPreview = "暂无消息"
    const val promptDeleted = "Prompt 已删除"
    const val renameConversationTitle = "重命名会话"
    const val deleteConversationTitle = "删除会话"
    const val deleteConversationMessage = "会话与全部消息都会被删除，且不可恢复。"
    const val noPromptsTitle = "还没有 Prompt 模板"
    const val noPromptsDesc = "先建一个角色设定，会话才能绑上人设。"
    const val goCreatePrompt = "去创建 Prompt"

    // Chat
    const val inputHint = "发消息…"
    const val send = "发送"
    const val stop = "停止生成"
    const val thinking = "思考中…"
    const val switchPromptHint = "点标签切换人设"
    const val switchPromptTitle = "切换人设"
    const val switchPromptSubtitle = "只影响之后的对话风格"
    const val keepHistory = "保留历史"
    const val clearHistory = "清空历史"
    const val switchPromptResetTitle = "要清空历史吗？"
    const val currentPrompt = "当前人设"
    const val noPrompt = "未绑定"
    const val emptyChatTitle = "开始聊天"
    const val emptyChatDesc = "输入问题并发送，回复会逐字流式出现。"
    const val dismiss = "知道了"
    const val apiKeyMissing = "还没有填写密钥，先去设置页填一下吧"
    const val loading = "加载中…"

    // Prompt management
    const val promptsTitle = "Prompt"
    const val newPromptTitle = "新建 Prompt"
    const val editPromptTitle = "编辑 Prompt"
    const val promptNameLabel = "名称"
    const val promptNamePlaceholder = "例如：代码搭档"
    const val systemPromptLabel = "角色设定"
    const val systemPromptPlaceholder = "你是一名……"
    const val temperatureLabel = "回答风格"
    const val topPLabel = "多样性"
    const val resetSwitchLabel = "切换会话时重置上下文"
    const val resetSwitchDesc = "开启后，会话内切到该模板会询问是否清空历史"
    const val deletePromptTitle = "删除 Prompt"
    const val nameRequired = "名称不能为空"
    const val systemPromptRequired = "角色设定不能为空"
    const val emptyPromptsTitle = "还没有 Prompt 模板"
    const val emptyPromptsDesc = "每个模板是一段角色设定，会话内可随时切换。"

    // Settings
    const val settingsTitle = "设置"
    const val apiKeySection = "连接码"
    const val apiKeyLabel = "DeepSeek 密钥"
    const val apiKeyPlaceholder = "sk-…"
    const val apiKeySaved = "已加密保存"
    const val apiKeyCleared = "已清除 API Key"
    const val apiKeyEmpty = "尚未设置 API Key"
    const val apiKeyStored = "已保存："
    const val saveKey = "保存"
    const val clearKey = "清除"
    const val apiKeyPrivacy = "Key 只在本机 Keystore 加密保存，仅用于请求接口，不会出现在日志里。"
    const val themeSection = "外观"
    const val themeSystem = "跟随系统"
    const val themeLight = "浅色"
    const val themeDark = "深色"
    const val aboutSection = "关于"
    const val appName = "deepseekAPIchat"
    const val appTagline = "多 Prompt 人设 · 流式对话"
    const val appVersion = "版本 1.4"
    const val copyrightHolder = "© 2026 K5NGM_JiYuxi"
    const val licenseSummary = "本项目以 MIT 协议开源：可自由使用、修改、分发，请保留版权声明与许可声明。"
    const val viewLicense = "查看许可证文本"
    const val licenseTitle = "MIT License 摘要"

    val licenseBody: String = """
        MIT License

        Copyright (c) 2026 K5NGM_JiYuxi

        Permission is hereby granted, free of charge, to any person obtaining a copy
        of this software and associated documentation files (the "Software"), to deal
        in the Software without restriction, including without limitation the rights
        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        copies of the Software, and to permit persons to whom the Software is
        furnished to do so, subject to the following conditions:

        The above copyright notice and this permission notice shall be included in all
        copies or substantial portions of the Software.

        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
        SOFTWARE.
    """.trimIndent()

    // ---- 增量 A：密钥配置引导 / 测试连接 -------------------------------
    const val testConnection = "测试连接"
    const val testingConnection = "正在测试连接…"
    const val connectionOk = "连接正常，可以开始聊天了"
    const val apiKeyInvalid = "密钥无效，请检查是否复制完整"
    const val apiKeyNoBalance = "密钥能用，但账户余额不足"
    const val apiKeyRateLimited = "请求太频繁了，稍后再试"
    const val networkUnavailable = "网络不通，稍后再试"

    const val apiGuideTitle = "三步拿到 DeepSeek 密钥"
    const val apiGuideSubtitle = "第一次用？照着点就行"
    const val apiGuideStep1Title = "打开 DeepSeek 开放平台"
    const val apiGuideStep1Desc = "点下面的按钮，用浏览器打开官方页面并登录"
    const val apiGuideOpenPlatform = "打开平台"
    const val deepseekApiKeysUrl = "https://platform.deepseek.com/api_keys"
    const val apiGuideStep2Title = "创建 API key"
    const val apiGuideStep2Desc =
        "在左侧点「API keys」，再点右上角「创建 API key」，起个名字后创建"
    const val apiGuideStep3Title = "复制并粘贴回来"
    const val apiGuideStep3Desc =
        "弹窗里那串 sk- 开头的字符只显示一次，点「复制」，回到本页粘贴保存"
    const val apiGuideImageCaption = "示意图位"
    const val apiGuideBillingNote = "DeepSeek 按量计费，充值少量金额即可使用很久。"

    // 真实截图的小字说明
    const val shotCaptionPlatform = "① 登录后在左侧找到「API keys」"
    const val shotCaptionCreate = "② 点「创建 API key」，随便起个名字"
    const val shotCaptionCopy = "③ 创建成功后立刻点「复制」，关掉就看不到了"

    const val apiKeyMissingTitle = "还差一步就能开始聊天"
    const val apiKeyMissingDesc = "先填入 DeepSeek 密钥，就能开始对话。"

    // ---- 增量 A2（大屏版）：初次使用的全屏三步引导 ----------------------
    const val onboardingSkip = "稍后再说"
    const val onboardingNext = "下一步"
    const val onboardingBack = "上一步"
    const val onboardingFinish = "完成，开始聊天"
    const val onboardingStart = "开始配置"

    // 第 1 步：欢迎 + 这是什么软件
    const val onboardingWelcomeTitle = "欢迎使用 deepseekAPIchat"
    const val onboardingWelcomeLead = "一个人设不够用？同一个会话里，随时换。"
    const val welcomeCoreLabel = "两个核心能力"
    const val welcomeFeatureSwitchTitle = "多个人设，随时切换"
    const val welcomeFeatureSwitchBody =
        "每个会话绑一个人设；聊到一半换个角色，历史可留可清，只影响之后的回答。"
    const val welcomeFeatureBatchTitle = "人设攒多了，还能批量管"
    const val welcomeFeatureBatchBody =
        "勾选多个一次删除，支持全选；被会话占用的人设自动保护，不会误删。"
    const val onboardingWelcomePoint2 = "回复逐字出现，不满意可以中途叫停"
    const val onboardingWelcomePoint3 = "会话和密钥只存在这台手机上"
    const val onboardingWelcomeFootTitle = "开始前先配好 DeepSeek 密钥"
    const val onboardingWelcomeFoot =
        "聊天能力由 DeepSeek 提供，密钥相当于你的通行证；它只保存在本机，不会上传到别处，也不会写进日志。"

    const val onboardingStepKeyTitle = "三步拿到密钥"
    const val onboardingStepKeyDesc = "照着做就行，两分钟搞定。"
    const val onboardingStepPasteTitle = "粘贴并验证"
    const val onboardingStepPasteDesc = "把刚复制的那串字符粘进来，保存后点一下「测试连接」。"

    const val clearKeyConfirmTitle = "清除密钥？"
    const val clearKeyConfirmMessage = "清除后要重新粘贴密钥才能继续聊天。"

    // ---- 增量 B：角色设定写法指引 ---------------------------------------
    const val promptGuideTitle = "怎么写出好用的角色设定"
    const val promptGuideSubtitle = "四句话讲清楚就够"
    const val promptGuideExampleTitle = "看看对比"
    const val useExample = "用这个示例"
    const val overwriteConfirmTitle = "覆盖现有设定？"
    const val overwriteConfirmMessage = "输入框里已经有内容了，用示例会把它覆盖掉。"
    const val overwriteConfirmOk = "覆盖"
    const val promptHelper = "写得越具体，回答越稳定。"

    val promptGuidePoints: List<String> = listOf(
        "说清楚它是谁：什么身份、什么经验。",
        "说清楚它怎么说话：语气、长短、要不要举例。",
        "说清楚它不该做什么：别做的事直接点名。",
        "给一个具体场景，比抽象形容有效得多。",
    )
    const val promptGuideBad1 = "你是一个助手。"
    const val promptGuideGood1 =
        "你是一名耐心的高中数学老师，用提问引导我解题，不直接给答案，每次只讲一个步骤。"
    const val promptGuideBad2 = "你很专业。"
    const val promptGuideGood2 =
        "你是一名有十年经验的简历顾问，先问我目标岗位，再逐条指出简历问题，每条给出修改示例。"

    // ---- 1.2：外观（背景图 + 主色调） -----------------------------------
    const val modelSectionTitle = "模型"
    const val modelFetch = "获取可用模型"
    const val modelFetching = "正在获取…"
    const val modelHint = "图片对话需要支持识图的模型（例如 deepseek-flash）"
    const val modelNeedKey = "先保存密钥，再获取模型列表"
    const val modelVisionTag = "可识图"

    const val appearanceSectionDesc = "深浅色、背景图与主色调，改完立即生效"
    const val themeModeTitle = "深浅色模式"
    const val themeModeDesc = "「跟随系统」会随系统深色设置自动切换"
    const val appearanceBackgroundTitle = "聊天背景图"
    const val appearanceBackgroundDesc = "选一张本地图片铺在界面最底层，卡片与文字会自动加蒙层保证可读"
    const val appearanceBackgroundPick = "选择图片"
    const val appearanceBackgroundRemove = "移除背景"
    const val appearanceBackgroundEmpty = "当前使用纯色背景"
    const val appearanceBackgroundBusy = "正在处理图片…"
    const val appearanceAccentTitle = "主色调"
    const val appearanceAccentDesc = "影响按钮、标签、选中态等强调色，改动立即生效"
    const val appearanceAccentReset = "恢复默认"
    const val appearanceAccentDefault = "默认蓝"

    // 图片相关的通用上限说明（与 ImageStore 常量保持一致）
    const val imageLimitsHint = "支持 JPEG / PNG / WebP，单张不超过 8 MB"
    const val imageProcessing = "正在压缩图片…"

    // 聊天里的图片附件
    const val attachImage = "添加图片"
    const val copyCode = "复制"
    const val copied = "已复制"
    const val removeImage = "移除图片"
    const val imageOnlyMessage = "（图片）"

    // ---- 增量 C：聊天体验设置 -------------------------------------------
    const val chatExperienceSection = "聊天体验"
    const val sendShortcutTitle = "发送快捷键"
    const val sendShortcutDesc = "决定回车键是直接发送，还是换行"
    const val sendShortcutEnter = "回车发送"
    const val sendShortcutNewline = "回车换行"
    const val sendShortcutNewlineHint = "该模式下 Ctrl+Enter 发送"
    const val autoTitleTitle = "自动生成会话标题"
    const val autoTitleDesc = "发首条消息后自动起一个 10 字以内的标题；失败时保留默认标题"
    const val contextCountTitle = "上下文消息条数"
    const val contextCountDesc = "只把最近 N 条历史发给模型，角色设定始终保留，用来控制消耗"
    const val streamingTitle = "流式输出"
    const val streamingDesc = "关闭后一次性显示完整回复，不再逐字出现"
    const val tokenUsageTitle = "显示 token 消耗与缓存命中率"
    const val tokenUsageDesc = "开启后在输入框下方显示本会话的累计用量"

    // ---- 增量 E：人设批量删除 -------------------------------------------
    const val promptSelectMode = "批量选择"
    const val promptSelectFinish = "完成"
    const val promptSelectAll = "全选"
    const val promptClearAll = "取消全选"
    const val promptBatchDelete = "删除选中"
    const val promptSelectHint = "点条目勾选，可全选后一次删除"
    const val promptBatchDeleteTitle = "确认删除这些人设？"
    const val promptBatchDeleteMessage = "删除后不可恢复；仍被会话引用的人设会被跳过，不会强行删除。"
    const val promptDeleting = "正在删除…"

    // Formatted
    fun conversationCount(count: Int): String = "共 $count 个会话"
    fun promptCount(count: Int): String = "共 $count 个模板"
    fun usageCount(count: Int): String = "被 $count 个会话使用"
    fun deletePromptInUse(count: Int): String = "被 $count 个会话引用，先解绑或删除这些会话。"
    fun switchPromptResetMessage(templateName: String): String =
        "「$templateName」默认在切换时重置上下文。要清空本会话的历史消息吗？"

    fun serverUnavailable(code: Int): String = "服务暂时不可用（$code），稍后再试"

    fun charCount(count: Int): String = "已写 $count 字"

    fun contextCountValue(count: Int): String = "最近 $count 条"

    fun modelFetched(count: Int): String = "已获取 $count 个模型，点选即生效"

    // ---- 1.2：图片附件的错误提示（都用人话） ----------------------------
    fun imageAttached(sizeKb: Long): String = "已附加 1 张图片（约 ${sizeKb} KB）"

    fun imageTooLarge(maxMb: Int): String = "图片太大了，请换一张小于 ${maxMb} MB 的"

    fun imageUnsupported(): String = "这个格式暂时不支持，请换成 JPEG / PNG / WebP"

    fun imageDecodeFailed(): String = "图片读不出来，换一张试试"

    fun imageReadFailed(): String = "读取图片失败，请重试"

    fun imagePickFailed(): String = "没能打开相册，请检查系统权限后重试"

    fun promptSelectedCount(count: Int): String = "已选 $count 项"

    fun promptsDeleted(count: Int): String = "已删除 $count 个人设"

    fun promptsDeletedPartial(deleted: Int, blocked: Int, failed: Int): String {
        val parts = mutableListOf("已删除 $deleted 个")
        if (blocked > 0) parts.add("$blocked 个被会话引用，已跳过")
        if (failed > 0) parts.add("$failed 个删除失败")
        return parts.joinToString("；")
    }

    fun promptsNotDeleted(blocked: Int, failed: Int): String {
        val parts = mutableListOf<String>()
        if (blocked > 0) parts.add("$blocked 个被会话引用，已跳过")
        if (failed > 0) parts.add("$failed 个删除失败")
        return parts.joinToString("；").ifEmpty { unknownError }
    }

    fun tokenUsage(totalTokens: Long, cacheHitRate: Float?): String {
        val total = formatThousands(totalTokens)
        if (cacheHitRate == null) return "本会话 Token $total"
        val percent = (cacheHitRate * 100f).toInt()
        return "本会话 Token $total · 缓存命中 $percent%"
    }

    private fun formatThousands(value: Long): String {
        val raw = value.toString()
        if (raw.length <= 3) return raw
        val builder = StringBuilder()
        var counter = 0
        for (index in raw.length - 1 downTo 0) {
            builder.append(raw[index])
            counter++
            if (counter % 3 == 0 && index != 0) builder.append(',')
        }
        return builder.reverse().toString()
    }
}
