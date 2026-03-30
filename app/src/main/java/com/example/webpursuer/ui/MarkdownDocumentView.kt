package com.murmli.webpursuer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownDocumentView(
    markdown: String,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += offsetChange
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) // Dark gray background outside the document
            .transformable(state = state),
        contentAlignment = Alignment.TopCenter
    ) {
        // The "Page"
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .widthIn(max = 800.dp) // Max width like a document
                .fillMaxWidth()
                .wrapContentHeight(),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                MarkdownText(
                    markdown = markdown,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black // Explicitly black text on white page
                )
                
                // Bottom padding to feel more like a page
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
