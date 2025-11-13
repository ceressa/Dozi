package com.bardino.dozi.onboarding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.ui.theme.*

data class SpotlightTarget(
    val rect: Rect,
    val message: String,
    val cornerRadius: Dp = 16.dp
)

@Composable
fun SpotlightOverlay(
    target: SpotlightTarget?,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    showDoziPointer: Boolean = true
) {
    if (target == null) return

    // Glow animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onNext() }
    ) {
        // Blur arka plan
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(2.dp)
        ) {
            val path = Path().apply {
                addRect(
                    Rect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height
                    )
                )

                // Highlight alanını çıkar
                addRoundRect(
                    RoundRect(
                        rect = target.rect,
                        cornerRadius = CornerRadius(
                            target.cornerRadius.toPx(),
                            target.cornerRadius.toPx()
                        )
                    )
                )
            }

            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.7f),
                style = Fill
            )
        }

        // Highlight glow
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawRoundRect(
                color = DoziTurquoise.copy(alpha = glowAlpha * 0.3f),
                topLeft = Offset(
                    target.rect.left - 4.dp.toPx(),
                    target.rect.top - 4.dp.toPx()
                ),
                size = Size(
                    target.rect.width + 8.dp.toPx(),
                    target.rect.height + 8.dp.toPx()
                ),
                cornerRadius = CornerRadius(
                    (target.cornerRadius + 4.dp).toPx(),
                    (target.cornerRadius + 4.dp).toPx()
                )
            )
        }

        // Mesaj balonu (üstte veya altta)
        val isTop = target.rect.top > 200f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(if (isTop) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(
                    top = if (isTop) (target.rect.top - 150f).dp else 0.dp,
                    bottom = if (!isTop) 100.dp else 0.dp
                )
                .padding(horizontal = 24.dp)
        ) {
            if (showDoziPointer) {
                DoziTutorialPointer(
                    message = target.message,
                    pointingDown = !isTop
                )
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = target.message,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Devam etmek için dokun",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}