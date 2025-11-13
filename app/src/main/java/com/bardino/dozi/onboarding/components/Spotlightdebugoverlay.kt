package com.bardino.dozi.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Spotlight koordinatlarını test etmek için debug overlay
 * Ekranı tıklayarak koordinatları görebilirsin
 */
@Composable
fun SpotlightDebugOverlay(
    enabled: Boolean = false,
    onRectSelected: (Rect) -> Unit = {}
) {
    if (!enabled) return

    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var selectedRect by remember { mutableStateOf<Rect?>(null) }
    var isSelecting by remember { mutableStateOf(false) }
    var startPos by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {} // Block touches
    ) {
        // Grid overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Yatay çizgiler
            for (i in 0..20) {
                val y = size.height * i / 20
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
            // Dikey çizgiler
            for (i in 0..20) {
                val x = size.width * i / 20
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }

            // Seçili rect
            selectedRect?.let { rect ->
                drawRect(
                    color = Color.Green.copy(alpha = 0.3f),
                    topLeft = Offset(rect.left, rect.top),
                    size = androidx.compose.ui.geometry.Size(rect.width, rect.height)
                )
            }
        }

        // Info panel
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "🎯 SPOTLIGHT DEBUG MODE",
                color = Color.Yellow,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(4.dp))
            touchPosition?.let { pos ->
                Text(
                    text = "X: ${pos.x.toInt()}, Y: ${pos.y.toInt()}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            selectedRect?.let { rect ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = """
                        Rect(
                          left = ${rect.left}f,
                          top = ${rect.top}f,
                          right = ${rect.right}f,
                          bottom = ${rect.bottom}f
                        )
                    """.trimIndent(),
                    color = Color.Green,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}