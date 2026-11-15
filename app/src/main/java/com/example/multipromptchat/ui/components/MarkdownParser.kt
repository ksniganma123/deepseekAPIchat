package com.example.multipromptchat.ui.components

/** Markdown 块级元素（只覆盖聊天里会出现的那些）。 */
/** 列表项：带真实层级与算好的序号（嵌套时由解析器按层计数） */
data class MdListItem(
    val text: String,
    val level: Int,
    val marker: String,
    val ordered: Boolean,
)

sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class ListBlock(val items: List<MdListItem>) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val language: String, val code: String) : MdBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
    data object Divider : MdBlock
}

/**
 * 行级 Markdown 解析器。只做块级切分，行内样式交给渲染层。
 *
 * 原则：任何不认识的行都当普通段落，绝不抛异常（上游还有 try/catch 兜底）。
 */
object MarkdownParser {

    private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
    private val LIST_ITEM = Regex("^(\\s*)([-*+]|\\d+[.)])\\s+(.*)$")
    private val DIVIDER = Regex("^\\s*(-{3,}|\\*{3,}|_{3,})\\s*$")
    private const val MAX_LIST_LEVEL = 6

    private val TABLE_SEPARATOR = Regex("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")

    fun parse(source: String): List<MdBlock> {
        if (source.isBlank()) return emptyList()
        val lines = source.replace("\r\n", "\n").split('\n')
        val blocks = mutableListOf<MdBlock>()

        var index = 0
        val paragraph = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks.add(MdBlock.Paragraph(paragraph.toString().trim()))
                paragraph.clear()
            }
        }

        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()

            // 围栏代码块（可能没闭合：流式场景下调用方不会走到这里，但仍要容错）
            if (trimmed.startsWith("```")) {
                flushParagraph()
                val language = trimmed.removePrefix("```").trim()
                val code = StringBuilder()
                index++
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    code.append(lines[index]).append('\n')
                    index++
                }
                if (index < lines.size) index++ // 跳过收尾 ```
                blocks.add(MdBlock.Code(language = language, code = code.toString().trimEnd('\n')))
                continue
            }

            // 空行 = 段落分隔
            if (trimmed.isEmpty()) {
                flushParagraph()
                index++
                continue
            }

            // 标题
            val headingMatch = HEADING.find(trimmed)
            if (headingMatch != null) {
                flushParagraph()
                blocks.add(
                    MdBlock.Heading(
                        level = headingMatch.groupValues[1].length,
                        text = headingMatch.groupValues[2].trim(),
                    )
                )
                index++
                continue
            }

            // 分割线
            if (DIVIDER.matches(trimmed)) {
                flushParagraph()
                blocks.add(MdBlock.Divider)
                index++
                continue
            }

            // 引用（连续 > 行合并成一段）
            if (trimmed.startsWith(">")) {
                flushParagraph()
                val quote = StringBuilder()
                while (index < lines.size && lines[index].trim().startsWith(">")) {
                    val content = lines[index].trim().removePrefix(">").trimStart()
                    if (quote.isNotEmpty()) quote.append('\n')
                    quote.append(content)
                    index++
                }
                blocks.add(MdBlock.Quote(quote.toString()))
                continue
            }

            // 表格：当前行有 | 且下一行是分隔行
            if (trimmed.contains('|') && index + 1 < lines.size &&
                TABLE_SEPARATOR.matches(lines[index + 1].trim())
            ) {
                flushParagraph()
                val header = splitRow(trimmed)
                index += 2
                val rows = mutableListOf<List<String>>()
                while (index < lines.size && lines[index].contains('|') && lines[index].isNotBlank()) {
                    rows.add(splitRow(lines[index].trim()))
                    index++
                }
                blocks.add(MdBlock.Table(header = header, rows = rows))
                continue
            }

            // 列表（有序 / 无序 / 嵌套 / 松散列表统一处理）
            if (LIST_ITEM.containsMatchIn(line)) {
                flushParagraph()
                val items = mutableListOf<MdListItem>()
                // 每个层级一个计数器：回到浅层时把更深的计数清零，保证「按层递增」
                val counters = mutableListOf<Int>()
                var cursor = index
                while (cursor < lines.size) {
                    val match = LIST_ITEM.find(lines[cursor])
                    if (match == null) {
                        // 松散列表：允许单个空行后继续同一列表（这就是「全是 1.」的元凶）
                        val next = cursor + 1
                        if (lines[cursor].isBlank() && next < lines.size && LIST_ITEM.containsMatchIn(lines[next])) {
                            cursor++
                            continue
                        }
                        break
                    }
                    val indentText = match.groupValues[1].replace("\t", "    ")
                    val level = (indentText.length / 2).coerceIn(0, MAX_LIST_LEVEL)
                    val marker = match.groupValues[2]
                    val ordered = marker.firstOrNull()?.isDigit() == true
                    val text = match.groupValues[3].trim()

                    while (counters.size <= level) counters.add(0)
                    for (deeper in level + 1 until counters.size) counters[deeper] = 0

                    val displayMarker = if (ordered) {
                        counters[level] += 1
                        "${counters[level]}."
                    } else {
                        counters[level] = 0
                        if (level == 0) "•" else "◦"
                    }
                    items.add(MdListItem(text = text, level = level, marker = displayMarker, ordered = ordered))
                    cursor++
                }
                blocks.add(MdBlock.ListBlock(items = items))
                index = cursor
                continue
            }

            // 普通段落（多行拼接）
            if (paragraph.isNotEmpty()) paragraph.append('\n')
            paragraph.append(trimmed)
            index++
        }

        flushParagraph()
        return blocks
    }

    private fun splitRow(line: String): List<String> {
        val cleaned = line.trim().removePrefix("|").removeSuffix("|")
        return cleaned.split('|').map { it.trim() }
    }
}
