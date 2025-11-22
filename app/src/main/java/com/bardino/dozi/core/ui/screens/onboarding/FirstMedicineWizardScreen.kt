package com.bardino.dozi.core.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.bardino.dozi.core.ui.components.base.*

@Composable
fun FirstMedicineWizardScreen(
    onAddMedicine: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DoziSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(DoziSpacing.xxl))

        // Başlık
        Text(
            text = "İlk İlacınızı Ekleyin",
            style = DoziTypography.h1,
            color = DoziColors.OnSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(DoziSpacing.md))

        Text(
            text = "Dozi'nin gücünü görmek için hemen bir ilaç ekleyin",
            style = DoziTypography.body1,
            color = DoziColors.OnSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bilgi kartları
        DoziCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Neden ilaç eklemeliyim?",
                    style = DoziTypography.subtitle1,
                    color = DoziColors.Primary
                )

                Spacer(modifier = Modifier.height(DoziSpacing.sm))

                WizardInfoItem(
                    number = "1",
                    text = "Zamanında hatırlatma alın"
                )
                WizardInfoItem(
                    number = "2",
                    text = "Dozlarınızı takip edin"
                )
                WizardInfoItem(
                    number = "3",
                    text = "Stok uyarıları alın"
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Ana buton
        DoziButton(
            text = "İlaç Ekle",
            onClick = onAddMedicine,
            size = DoziButtonSize.Large,
            icon = Icons.Default.Add,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(DoziSpacing.sm))

        // Atla butonu
        DoziButton(
            text = "Sonra eklerim",
            onClick = onSkip,
            variant = DoziButtonVariant.Text,
            size = DoziButtonSize.Medium
        )

        Spacer(modifier = Modifier.height(DoziSpacing.lg))
    }
}

@Composable
private fun WizardInfoItem(
    number: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DoziSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = number,
            style = DoziTypography.subtitle1,
            color = DoziColors.Primary
        )
        Spacer(modifier = Modifier.width(DoziSpacing.md))
        Text(
            text = text,
            style = DoziTypography.body2,
            color = DoziColors.OnSurface
        )
    }
}
