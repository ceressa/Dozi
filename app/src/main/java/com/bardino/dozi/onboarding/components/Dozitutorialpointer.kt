package com.bardino.dozi.onboarding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.theme.*

@Composable
fun DoziTutorialPointer(
    message: String,
    pointingDown: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Bounce animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Point animasyonu (işaret parmağı)
    val pointScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "point"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (pointingDown) {
            Arrangement.Top
        } else {
            Arrangement.Bottom
        }
    ) {
        if (!pointingDown) {
            // Pointer (aşağıdan yukarı gösteriyorsa)
            Text(
                text = "👇",
                fontSize = MaterialTheme.typography.displayMedium.fontSize,
                modifier = Modifier
                    .offset(y = (-bounceOffset).dp)
                    .rotate(180f)
            )
            Spacer(Modifier.height(8.dp))
        }

        // Dozi + Mesaj
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dozi karakteri
            Image(
                painter = painterResource(id = R.drawable.dozi_teach1),
                contentDescription = "Dozi",
                modifier = Modifier
                    .size(60.dp)
                    .offset(y = bounceOffset.dp)
            )

            // Konuşma balonu
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (pointingDown) {
            // Pointer (yukarıdan aşağı gösteriyorsa)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "👇",
                fontSize = MaterialTheme.typography.displayMedium.fontSize,
                modifier = Modifier.offset(y = bounceOffset.dp)
            )
        }
    }
}