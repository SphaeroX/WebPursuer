package com.example.webpursuer.util

import java.lang.StringBuilder
import kotlin.math.max
import kotlin.math.min

object DiffUtils {

    /**
     * Vergleicht zwei Texte und gibt nur die Änderungen mit Kontext zurück.
     * @param oldText Der ursprüngliche Text
     * @param newText Der neue Text
     * @param contextLines Anzahl der unveränderten Zeilen um eine Änderung herum (Standard: 2)
     * @return Ein String, der nur die geänderten Stellen enthält (Markdown-Format)
     */
    fun getDiff(oldText: String?, newText: String?, contextLines: Int = 2): String {
        if (oldText == null) return "New Content: \n${truncate(newText ?: "")}"
        if (newText == null) return "Content was removed."
        if (oldText == newText) return "No visible text changes."

        val oldLines = oldText.lines()
        val newLines = newText.lines()
        
        // Einfache zeilenbasierte Diff-Logik
        val sb = StringBuilder()
        val changes = mutableListOf<Int>() // Indizes der geänderten Zeilen in newLines

        // Wir suchen nach Zeilen, die in newLines sind, aber nicht identisch in oldLines an derselben Position
        // (Für eine mobile App reicht dieser "Simple Diff" oft aus, um 90% der Daten zu sparen)
        val maxLines = max(oldLines.size, newLines.size)
        for (i in 0 until maxLines) {
            val oldLine = oldLines.getOrNull(i)
            val newLine = newLines.getOrNull(i)
            if (oldLine != newLine) {
                changes.add(i)
            }
        }

        if (changes.isEmpty()) return "No changes detected in text blocks."

        // Gruppierung der Änderungen mit Kontext
        var lastAddedLine = -1
        for (changeIdx in changes) {
            val start = max(0, changeIdx - contextLines)
            val end = min(newLines.size - 1, changeIdx + contextLines)

            if (start > lastAddedLine + 1 && lastAddedLine != -1) {
                sb.append("\n[...] \n\n")
            }

            for (i in max(start, lastAddedLine + 1)..end) {
                val line = newLines[i]
                if (changes.contains(i)) {
                    val oldLine = oldLines.getOrNull(i)
                    if (oldLine != null) {
                        sb.append("- OLD: $oldLine\n")
                        sb.append("+ NEW: $line\n")
                    } else {
                        sb.append("+ ADDED: $line\n")
                    }
                } else {
                    sb.append("  $line\n")
                }
                lastAddedLine = i
            }
        }

        return truncate(sb.toString())
    }

    /**
     * Begrenzt den Text auf eine maximale Länge, um API-Limits nicht zu sprengen.
     */
    fun truncate(text: String, maxLength: Int = 8000): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength) + "\n\n[... Text truncated for API efficiency ...]"
        } else {
            text
        }
    }
}
