package com.example.webpursuer.util

import java.util.regex.Pattern

object UrlUtils {
    fun extractUrlFromText(text: String): String? {
        // Simple regex to find URL in text
        val urlRegex = "(https?://\\S+)"
        val pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
}
