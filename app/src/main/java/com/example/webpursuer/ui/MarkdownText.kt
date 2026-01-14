package com.murmli.webpursuer.ui

import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
        markdown: String,
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified
) {
    val styledText = remember(markdown) { parseMarkdown(markdown) }
    Text(
            text = styledText,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = color
    )
}

fun parseMarkdown(text: String): AnnotatedString {
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

                val fontSize =
                        when (level) {
                            1 -> 24.sp
                            2 -> 20.sp
                            3 -> 18.sp
                            else -> 16.sp
                        }
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
            // Code Blocks (Basic single line detection for indent/backticks at start)
            else if (currentLine.startsWith("```") || currentLine.startsWith("    ")) {
                currentLine = currentLine.removePrefix("```").removeSuffix("```").trim()
                withStyle(
                        SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color.LightGray.copy(alpha = 0.3f)
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

// Helper to handle inline bold/italic within a line
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
    TEXT,
    BOLD,
    ITALIC,
    CODE,
    LINK
}

data class MarkdownPart(val type: MarkdownType, val text: String)

fun splitByDelimiters(text: String): List<MarkdownPart> {
    val parts = mutableListOf<MarkdownPart>()
    var currentIndex = 0
    // Regex matches: **bold**, *italic*, `code`, [link](url)
    val regex = Regex("(\\*\\*.*?\\*\\*)|(\\*.*?\\*)|(`.*?`)|(\\[.*?\\]\\(.*?\\))")

    val matches = regex.findAll(text)

    for (match in matches) {
        if (match.range.first > currentIndex) {
            parts.add(
                    MarkdownPart(MarkdownType.TEXT, text.substring(currentIndex, match.range.first))
            )
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
