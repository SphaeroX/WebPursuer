package com.example.webpursuer.util

import org.junit.Test
import org.junit.Assert.*

class UrlUtilsTest {
    @Test
    fun extractUrlFromText_findsUrl() {
        val text = "Check this out https://example.com it is cool"
        val url = UrlUtils.extractUrlFromText(text)
        assertEquals("https://example.com", url)
    }

    @Test
    fun extractUrlFromText_findsHttpUrl() {
        val text = "http://google.com"
        val url = UrlUtils.extractUrlFromText(text)
        assertEquals("http://google.com", url)
    }

    @Test
    fun extractUrlFromText_returnsNullIfNoUrl() {
        val text = "Just some text"
        val url = UrlUtils.extractUrlFromText(text)
        assertNull(url)
    }
}
