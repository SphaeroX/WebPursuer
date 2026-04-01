package com.murmli.webpursuer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownBlock {
    data class Text(val lines: List<String>) : MarkdownBlock()
    data class Code(val content: String) : MarkdownBlock()
    data class Table(val rows: List<List<String>>) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
}

fun parseBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    var currentTextLines = mutableListOf<String>()

    fun flushText() {
        if (currentTextLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Text(currentTextLines.toList()))
            currentTextLines.clear()
        }
    }

    val lines = markdown.split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // Horizontal Rule
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            flushText()
            blocks.add(MarkdownBlock.HorizontalRule)
            i++
            continue
        }

        // Code block
        if (trimmed.startsWith("```")) {
            flushText()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.Code(codeLines.joinToString("\n")))
            i++ // Skip ending ```
            continue
        }

        // Table
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            flushText()
            val tableRows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                val rowTrimmed = lines[i].trim()
                val cells = rowTrimmed.split("|").map { it.trim() }.drop(1).dropLast(1)
                
                // Skip separator rows (e.g., |---|---|)
                if (cells.all { it.matches(Regex("^[\\s:-]+$")) }) {
                    // Just skip
                } else {
                    tableRows.add(cells)
                }
                i++
            }
            if (tableRows.isNotEmpty()) {
                blocks.add(MarkdownBlock.Table(tableRows))
            }
            continue
        }

        // Normal text line
        currentTextLines.add(line)
        i++
    }
    flushText()
    return blocks
}

@Composable
fun MarkdownText(
        markdown: String,
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified,
        fontSizeMultiplier: Float = 1f
) {
    val blocks = remember(markdown) { parseBlocks(markdown) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy((8 * fontSizeMultiplier).dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Text -> {
                    val styledText = remember(block.lines, fontSizeMultiplier) { 
                        parseMarkdownText(block.lines.joinToString("\n"), fontSizeMultiplier) 
                    }
                    Text(
                            text = styledText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontSizeMultiplier,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * fontSizeMultiplier
                            ),
                            color = color
                    )
                }
                is MarkdownBlock.Code -> {
                    Surface(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                                text = block.content,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding((8 * fontSizeMultiplier).dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize * fontSizeMultiplier,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * fontSizeMultiplier
                                ),
                                color = color
                        )
                    }
                }
                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                            modifier = Modifier.padding(vertical = (8 * fontSizeMultiplier).dp),
                            color = Color.Gray.copy(alpha = 0.5f)
                    )
                }
                is MarkdownBlock.Table -> {
                    Column(
                            modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                    ) {
                        block.rows.forEachIndexed { rowIndex, row ->
                            Row(
                                    modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                            .background(if (rowIndex == 0) Color.LightGray.copy(alpha = 0.2f) else Color.Transparent)
                            ) {
                                row.forEachIndexed { cellIndex, cell ->
                                    val cellText = remember(cell, fontSizeMultiplier) { 
                                        parseMarkdownText(cell, fontSizeMultiplier) 
                                    }
                                    Box(
                                            modifier = Modifier
                                                    .weight(1f)
                                                    .padding((8 * fontSizeMultiplier).dp)
                                    ) {
                                        Text(
                                                text = cellText,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontSizeMultiplier,
                                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * fontSizeMultiplier
                                                ),
                                                fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                                color = color
                                        )
                                    }
                                    if (cellIndex < row.size - 1) {
                                        Box(
                                                modifier = Modifier
                                                        .width(1.dp)
                                                        .fillMaxHeight()
                                                        .background(Color.Gray.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                            if (rowIndex < block.rows.size - 1) {
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun parseMarkdownText(text: String, fontSizeMultiplier: Float = 1f): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line.trim()
            if (currentLine.isEmpty()) {
                if (index < lines.size - 1) append("\n")
                return@forEachIndexed
            }

            // Headers
            if (currentLine.startsWith("#")) {
                val level = currentLine.takeWhile { it == '#' }.length
                currentLine = currentLine.removePrefix("#".repeat(level)).trim()

                val fontSize = when (level) {
                    1 -> 24.sp
                    2 -> 20.sp
                    3 -> 18.sp
                    else -> 16.sp
                } * fontSizeMultiplier
                val fontWeight = FontWeight.Bold

                withStyle(SpanStyle(fontSize = fontSize, fontWeight = fontWeight)) {
                    appendMarkdownLine(currentLine)
                }
            }
            // List Items
            else if (currentLine.startsWith("* ") || currentLine.startsWith("- ")) {
                currentLine = currentLine.substring(2)
                append("• ")
                appendMarkdownLine(currentLine)
            }
            // Code Blocks
            else if (currentLine.startsWith("```") || currentLine.startsWith("    ")) {
                currentLine = currentLine.removePrefix("```").removeSuffix("```").trim()
                withStyle(
                        SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color.LightGray.copy(alpha = 0.3f),
                                fontSize = 14.sp * fontSizeMultiplier
                        )
                ) { append(currentLine) }
            }
            // Normal Text
            else {
                appendMarkdownLine(currentLine)
            }

            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

fun AnnotatedString.Builder.appendMarkdownLine(line: String) {
    val parts = splitByDelimiters(line)
    parts.forEach { part ->
        when (part.type) {
            MarkdownType.BOLD ->
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part.text) }
            MarkdownType.ITALIC ->
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(part.text) }
            MarkdownType.CODE ->
                    withStyle(
                            SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = Color.LightGray.copy(alpha = 0.3f)
                            )
                    ) { append(part.text) }
            MarkdownType.LINK -> withStyle(SpanStyle(color = Color.Blue)) { append(part.text) }
            MarkdownType.TEXT -> append(part.text)
        }
    }
}

enum class MarkdownType {
    TEXT, BOLD, ITALIC, CODE, LINK
}

data class MarkdownPart(val type: MarkdownType, val text: String)

fun splitByDelimiters(text: String): List<MarkdownPart> {
    val parts = mutableListOf<MarkdownPart>()
    var currentIndex = 0
    val regex = Regex("(\\*\\*.*?\\*\\*)|(\\*.*?\\*)|(`.*?`)|(\\[.*?\\]\\(.*?\\))")

    val matches = regex.findAll(text)

    for (match in matches) {
        if (match.range.first > currentIndex) {
            parts.add(MarkdownPart(MarkdownType.TEXT, text.substring(currentIndex, match.range.first)))
        }

        val value = match.value
        when {
            value.startsWith("**") ->
                    parts.add(MarkdownPart(MarkdownType.BOLD, value.removeSurrounding("**")))
            value.startsWith("*") ->
                    parts.add(MarkdownPart(MarkdownType.ITALIC, value.removeSurrounding("*")))
            value.startsWith("`") ->
                    parts.add(MarkdownPart(MarkdownType.CODE, value.removeSurrounding("`")))
            value.startsWith("[") -> {
                val label = value.substringAfter("[").substringBefore("]")
                parts.add(MarkdownPart(MarkdownType.LINK, label))
            }
        }
        currentIndex = match.range.last + 1
    }

    if (currentIndex < text.length) {
        parts.add(MarkdownPart(MarkdownType.TEXT, text.substring(currentIndex)))
    }

    return parts
}
