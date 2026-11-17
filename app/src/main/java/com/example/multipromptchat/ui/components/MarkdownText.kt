package com.example.multipromptchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multipromptchat.ui.Strings
import com.example.multipromptchat.ui.theme.CardShape
import com.example.multipromptchat.ui.theme.CodeComment
import com.example.multipromptchat.ui.theme.CodeKeyword
import com.example.multipromptchat.ui.theme.CodeNumber
import com.example.multipromptchat.ui.theme.CodeString
import kotlinx.coroutines.delay

/**
 * 轻量 Markdown 渲染（不引第三方库）。
 *
 * 为什么自己写：`compose-markdown` 在 JitPack、需要额外仓库；`multiplatform-markdown-renderer-m3`
 * 会拉 Compose Multiplatform 的 material3，和现有 androidx material3 有类冲突风险。
 *
 * 覆盖：标题、段落、粗体/斜体/删除线/行内代码、有序/无序列表、引用、分割线、表格、
 * 围栏代码块（**横向滚动 + 右上角复制 + 基础语法高亮**）。
 *
 * ⚠️ 流式输出时**不要**用它：半截 Markdown（``` 未闭合、表格没对齐）解析出来会乱。
 * 调用方在 isStreaming 期间继续用普通 Text，结束之后再切到这里。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) {
        runCatching { MarkdownParser.parse(markdown) }.getOrElse { emptyList() }
    }

    if (blocks.isEmpty()) {
        // 解析不出内容时退回纯文本，绝不白屏。
        Text(
            text = markdown,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> MdHeading(block)
                is MdBlock.Paragraph -> MdParagraph(block.text)
                is MdBlock.ListBlock -> MdList(block)
                is MdBlock.Quote -> MdQuote(block.text)
                is MdBlock.Code -> MdCodeBlock(block)
                is MdBlock.Table -> MdTable(block)
                MdBlock.Divider -> HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// ---- 各类块 --------------------------------------------------------------

@Composable
private fun MdHeading(block: MdBlock.Heading) {
    // 6 级标题各给一档字号，深层级靠字号与上间距收敛，不靠编号
    val style = when (block.level) {
        1 -> MaterialTheme.typography.titleLarge
        2 -> MaterialTheme.typography.titleMedium
        3 -> MaterialTheme.typography.titleSmall
        4 -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        5 -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        else -> MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
    }
    Text(
        text = inlineAnnotated(block.text, MaterialTheme.colorScheme),
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = if (block.level <= 2) 2.dp else 4.dp),
    )
}

@Composable
private fun MdParagraph(text: String) {
    Text(
        text = inlineAnnotated(text, MaterialTheme.colorScheme),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun MdList(block: MdBlock.ListBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (item.level * 16).dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = item.marker,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(if (item.ordered) 26.dp else 16.dp),
                )
                Text(
                    text = inlineAnnotated(item.text, MaterialTheme.colorScheme),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MdQuote(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = inlineAnnotated(text, MaterialTheme.colorScheme),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MdTable(block: MdBlock.Table) {
    // 列宽按等权分配、铺满可用宽度：竖屏不会被挤爆，横屏/平板能吃到多余横向空间。
    // 只有列数很多（> 4）时才退化成横向滚动，避免每列窄到只剩几个字。
    val columnCount = maxOf(block.header.size, block.rows.maxOfOrNull { it.size } ?: 0)
    val useWeights = columnCount in 1..4

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .then(if (useWeights) Modifier else Modifier.horizontalScroll(rememberScrollState()))
                .padding(10.dp),
        ) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                block.header.forEach { cell ->
                    MdTableCell(text = cell, bold = true, weight = if (useWeights) 1f else null)
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            block.rows.forEach { row ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    row.forEach { cell ->
                        MdTableCell(text = cell, bold = false, weight = if (useWeights) 1f else null)
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.MdTableCell(
    text: String,
    bold: Boolean,
    weight: Float?,
) {
    Text(
        text = inlineAnnotated(text, MaterialTheme.colorScheme),
        style = if (bold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = if (weight != null) {
            Modifier.weight(weight).padding(end = 12.dp)
        } else {
            Modifier.width(140.dp).padding(end = 12.dp)
        },
    )
}

/** 代码块：语法高亮 + 横向滚动 + 右上角复制 */
@Composable
private fun MdCodeBlock(block: MdBlock.Code) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(block.code) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = block.language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(block.code))
                        copied = true
                    },
                ) {
                    Text(
                        text = if (copied) Strings.copied else Strings.copyCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (copied) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp),
            ) {
                Text(
                    text = highlightCode(block.code),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                    ),
                    softWrap = false,
                )
            }
        }
    }
}

// ---- 行内样式 ------------------------------------------------------------

private fun inlineAnnotated(
    text: String,
    scheme: androidx.compose.material3.ColorScheme,
): AnnotatedString = buildAnnotatedString {
    var index = 0
    val buffer = StringBuilder()

    fun flush() {
        if (buffer.isEmpty()) return
        append(buffer.toString())
        buffer.clear()
    }

    while (index < text.length) {
        val ch = text[index]
        when {
            // 行内代码 `x`
            ch == '`' -> {
                val end = text.indexOf('`', index + 1)
                if (end > index) {
                    flush()
                    val start = length
                    append(text.substring(index + 1, end))
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = scheme.surfaceVariant,
                            color = scheme.onSurface,
                        ),
                        start,
                        length,
                    )
                    index = end + 1
                } else {
                    buffer.append(ch); index++
                }
            }

            // 粗体 **x**
            ch == '*' && index + 1 < text.length && text[index + 1] == '*' -> {
                val end = text.indexOf("**", index + 2)
                if (end > index) {
                    flush()
                    val start = length
                    append(text.substring(index + 2, end))
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                    index = end + 2
                } else {
                    buffer.append(ch); index++
                }
            }

            // 删除线 ~~x~~
            ch == '~' && index + 1 < text.length && text[index + 1] == '~' -> {
                val end = text.indexOf("~~", index + 2)
                if (end > index) {
                    flush()
                    val start = length
                    append(text.substring(index + 2, end))
                    addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, length)
                    index = end + 2
                } else {
                    buffer.append(ch); index++
                }
            }

            // 斜体 *x* / _x_
            ch == '*' || ch == '_' -> {
                val end = text.indexOf(ch, index + 1)
                if (end > index + 1) {
                    flush()
                    val start = length
                    append(text.substring(index + 1, end))
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                    index = end + 1
                } else {
                    buffer.append(ch); index++
                }
            }

            else -> {
                buffer.append(ch)
                index++
            }
        }
    }
    flush()
}

// ---- 代码高亮（轻量词法，够用不上头） -------------------------------------

private val KEYWORDS = setOf(
    "val", "var", "fun", "class", "object", "interface", "data", "enum", "sealed", "if", "else",
    "when", "for", "while", "do", "return", "break", "continue", "try", "catch", "finally", "throw",
    "import", "package", "private", "public", "internal", "protected", "override", "suspend",
    "true", "false", "null", "this", "super", "new", "def", "from", "as", "in", "is", "not", "and",
    "const", "let", "async", "await", "function", "export", "default", "struct", "fn", "impl", "use",
    "int", "long", "float", "double", "char", "boolean", "void", "static", "final", "String",
)

private val TOKEN_REGEX = Regex(
    "(//[^\\n]*|#[^\\n]*|/\\*[\\s\\S]*?\\*/)" +                // 1 注释
        "|(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')" +  // 2 字符串
        "|(\\b\\d+(?:\\.\\d+)?\\b)" +                          // 3 数字
        "|(\\b[A-Za-z_][A-Za-z0-9_]*\\b)"                      // 4 单词
)

private fun highlightCode(code: String): AnnotatedString = buildAnnotatedString {
    val commentStyle = SpanStyle(color = CodeComment, fontStyle = FontStyle.Italic)
    val stringStyle = SpanStyle(color = CodeString)
    val numberStyle = SpanStyle(color = CodeNumber)
    val keywordStyle = SpanStyle(color = CodeKeyword, fontWeight = FontWeight.Medium)

    var last = 0
    TOKEN_REGEX.findAll(code).forEach { match ->
        if (match.range.first > last) append(code.substring(last, match.range.first))
        val value = match.value
        val style = when {
            match.groups[1] != null -> commentStyle
            match.groups[2] != null -> stringStyle
            match.groups[3] != null -> numberStyle
            value in KEYWORDS -> keywordStyle
            else -> null
        }
        val start = length
        append(value)
        if (style != null) addStyle(style, start, length)
        last = match.range.last + 1
    }
    if (last < code.length) append(code.substring(last))
}

