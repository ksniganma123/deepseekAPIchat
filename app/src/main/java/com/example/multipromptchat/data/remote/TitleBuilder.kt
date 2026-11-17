package com.example.multipromptchat.data.remote

/**
 * 标题清洗与本地兜底。
 *
 * 为什么单独抽出来：模型返回的"标题"经常带前缀（`标题：`）、引号、Markdown 粗体、
 * 句末标点，甚至整段解释；而生成失败时又不能让会话一直叫「新会话」。
 * 这两个函数是纯逻辑，可脱离 Android 单测。
 */
object TitleBuilder {

    const val MAX_CHARS = 10

    private val LEADING_NOISE = listOf(
        "标题：", "标题:", "对话标题：", "对话标题:", "会话标题：", "会话标题:",
        "title:", "Title:", "Title：", "**", "__", "「", "《", "\"", "'", "“", "”", "‘", "’",
        "#", "- ", "* ", "> ",
    )

    private val TRAILING_NOISE = charArrayOf(
        '。', '．', '.', '！', '!', '？', '?', '，', ',', '、', '：', ':', '；', ';', '~', '～',
    )

    private val WRAPPER_NOISE = charArrayOf(
        '“', '”', '‘', '’', '"', '\'', '「', '」', '《', '》', '*', '_', '#', ' ',
    )

    private val PLACEHOLDERS = setOf("无", "标题", "暂无", "新会话", "空", "none", "null", "n/a")

    /** 把模型输出清成一个干净的短标题；清不出有效内容时返回 null。 */
    fun sanitize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        var text = raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            // 只取第一行有效内容，避免模型附带解释
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: return null

        text = text.replace('\n', ' ').replace('\t', ' ').trim()

        // 反复剥前缀，处理「标题：**xxx**」这种叠加噪声
        var changed = true
        while (changed) {
            changed = false
            for (prefix in LEADING_NOISE) {
                if (text.startsWith(prefix)) {
                    text = text.removePrefix(prefix).trim()
                    changed = true
                }
            }
        }

        // 依次剥句末标点与包裹符号，直到稳定（应对「xxx」。这类叠加）
        repeat(3) {
            var end = text.length
            while (end > 0 && text[end - 1] in TRAILING_NOISE) end--
            text = text.take(end).trim().trim(*WRAPPER_NOISE)
        }

        // 折叠多余空白
        text = text.replace(Regex("\\s{2,}"), " ").trim()

        if (text.isEmpty()) return null
        if (PLACEHOLDERS.contains(text.lowercase())) return null

        return if (text.length <= MAX_CHARS) text else text.take(MAX_CHARS).trim().ifEmpty { null }
    }

    /**
     * 完全离线的兜底标题：取首条用户消息的第一行，去掉 Markdown/列表前缀后截断。
     * 只要消息里有非空文本就一定会有结果；调用方负责在返回 null 时保持默认标题。
     */
    fun localFallback(userMessage: String?, maxChars: Int = MAX_CHARS): String? {
        val raw = userMessage?.trim().orEmpty()
        if (raw.isEmpty()) return null

        var text = raw
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: return null

        // 去掉常见 Markdown/列表前缀（可能叠加多个 #）
        text = Regex("^#+\\s*").replace(text, "")
        text = text.removePrefix(">").trim()
        text = Regex("^[-*+]\\s+").replace(text, "")
        text = Regex("^\\d+[.)]\\s+").replace(text, "").trim()
        text = text.replace(Regex("\\s{2,}"), " ").trim()

        if (text.isEmpty()) return null
        val firstSentence = text
            .split('。', '！', '？', '；', '\n', '，')
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            .orEmpty()
        val candidate = if (firstSentence.isNotEmpty()) firstSentence else text
        return if (candidate.length <= maxChars) candidate else candidate.take(maxChars).trim().ifEmpty { null }
    }
}
