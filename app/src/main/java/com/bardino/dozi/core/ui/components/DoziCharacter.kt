package com.bardino.dozi.core.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.theme.*
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DoziCharacter(
    modifier: Modifier = Modifier,
    showMessage: Boolean = true,
    hasUpcomingMedicine: Boolean = false,
    customMessage: String? = null
) {
    // Animasyon için state
    val infiniteTransition = rememberInfiniteTransition(label = "dozi_float")
    val floatAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    val hour = LocalTime.now().hour
    val message = customMessage ?: when {
        hasUpcomingMedicine -> "Yaklaşan ilaç saatin var ⏰"
        hour in 5..11 -> "Günaydın! ☀️"
        hour in 12..17 -> "İyi günler! 👋"
        hour in 18..21 -> "İyi akşamlar! 🌆"
        else -> "İyi geceler! 🌙"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Dozi karakteri (float animasyonu ile)
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(y = floatAnimation.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.dozi),
                contentDescription = "Dozi Maskot",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(16.dp))

        // Mesaj
        AnimatedVisibility(
            visible = showMessage,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = DoziTurquoise.copy(alpha = 0.1f)
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Text(
                    text = message,
                    color = DoziTurquoise,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

// 🎭 DOZI EXPRESSIONS (gelecekte farklı duygular için)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DoziHappy(modifier: Modifier = Modifier) {
    DoziCharacter(
        modifier = modifier,
        customMessage = "Harika gidiyorsun! 🎉"
    )
}

@Composable
fun DoziThinking(modifier: Modifier = Modifier) {
    DoziCharacter(
        modifier = modifier,
        customMessage = "Hmm... 🤔"
    )
}

@Composable
fun DoziReminder(modifier: Modifier = Modifier) {
    DoziCharacter(
        modifier = modifier,
        hasUpcomingMedicine = true
    )
}