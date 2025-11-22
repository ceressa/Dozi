package com.bardino.dozi.core.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.ui.components.DoziCharacter
import com.bardino.dozi.core.ui.components.base.*

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DoziColors.Primary.copy(alpha = 0.1f),
                        Color.White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DoziSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Dozi maskotu
            DoziCharacter(
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(DoziSpacing.xl))

            // Başlık
            Text(
                text = "Dozi'ye Hoş Geldiniz!",
                style = DoziTypography.h1,
                color = DoziColors.OnSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(DoziSpacing.sm))

            // Alt başlık
            Text(
                text = "İlaçlarınızı asla unutmayın.\nSağlığınızı kontrol altında tutun.",
                style = DoziTypography.body1,
                color = DoziColors.OnSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // Özellik listesi
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DoziSpacing.sm)
            ) {
                FeatureItem(
                    emoji = "⏰",
                    text = "Akıllı hatırlatmalar"
                )
                FeatureItem(
                    emoji = "📊",
                    text = "Detaylı istatistikler"
                )
                FeatureItem(
                    emoji = "👨‍👩‍👧‍👦",
                    text = "Aile takip sistemi (Badi)"
                )
            }

            Spacer(modifier = Modifier.height(DoziSpacing.xl))

            // Ana buton
            DoziButton(
                text = "Başlayalım",
                onClick = onContinue,
                size = DoziButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(DoziSpacing.sm))

            // Atla butonu
            DoziButton(
                text = "Şimdilik geç",
                onClick = onSkip,
                variant = DoziButtonVariant.Text,
                size = DoziButtonSize.Medium
            )

            Spacer(modifier = Modifier.height(DoziSpacing.lg))
        }
    }
}

@Composable
private fun FeatureItem(
    emoji: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = emoji,
            style = DoziTypography.h3
        )
        Spacer(modifier = Modifier.width(DoziSpacing.md))
        Text(
            text = text,
            style = DoziTypography.body1,
            color = DoziColors.OnSurface
        )
    }
}
