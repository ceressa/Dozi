package com.bardino.dozi.core.ui.screens.badi

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.bardino.dozi.core.ui.components.base.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadiInfoBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DoziColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DoziSpacing.lg)
        ) {
            Text(
                text = "Badi Sistemi Nedir?",
                style = DoziTypography.h2,
                color = DoziColors.OnSurface
            )

            Spacer(modifier = Modifier.height(DoziSpacing.lg))

            // Özellik açıklamaları
            FeatureExplanation(
                icon = Icons.Default.People,
                title = "İlaç Takip Ortağı",
                description = "Güvendiğiniz kişileri Badi olarak ekleyin. İlacınızı kaçırdığınızda onlara bildirim gider."
            )

            Spacer(modifier = Modifier.height(DoziSpacing.md))

            FeatureExplanation(
                icon = Icons.Default.Notifications,
                title = "Kritik İlaç Uyarısı",
                description = "Kritik ilaçlar için 3 kademe yükseltme sistemi. 60 dakika içinde alınmazsa Badi'leriniz bilgilendirilir."
            )

            Spacer(modifier = Modifier.height(DoziSpacing.md))

            FeatureExplanation(
                icon = Icons.Default.Lock,
                title = "Gizlilik Kontrolü",
                description = "Hangi ilaçlarınızı paylaşacağınızı siz seçin. Badi'ler sadece izin verdiğiniz ilaçları görür."
            )

            Spacer(modifier = Modifier.height(DoziSpacing.xl))

            // Nasıl çalışır?
            DoziCard(
                containerColor = DoziColors.PrimaryLight.copy(alpha = 0.3f)
            ) {
                Column {
                    Text(
                        text = "Nasıl Çalışır?",
                        style = DoziTypography.subtitle1,
                        color = DoziColors.Primary
                    )

                    Spacer(modifier = Modifier.height(DoziSpacing.sm))

                    HowItWorksStep(
                        number = "1",
                        text = "Badi kodunuzu paylaşın veya başkasının kodunu girin"
                    )
                    HowItWorksStep(
                        number = "2",
                        text = "Karşı taraf isteği kabul etsin"
                    )
                    HowItWorksStep(
                        number = "3",
                        text = "Paylaşmak istediğiniz ilaçları seçin"
                    )
                }
            }

            Spacer(modifier = Modifier.height(DoziSpacing.lg))

            // Anladım butonu
            DoziButton(
                text = "Anladım",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(DoziSpacing.md))
        }
    }
}

@Composable
private fun FeatureExplanation(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DoziColors.Primary,
            modifier = Modifier.size(DoziSpacing.lg)
        )

        Spacer(modifier = Modifier.width(DoziSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DoziTypography.subtitle1,
                color = DoziColors.OnSurface
            )

            Spacer(modifier = Modifier.height(DoziSpacing.xxs))

            Text(
                text = description,
                style = DoziTypography.body2,
                color = DoziColors.OnSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HowItWorksStep(
    number: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DoziSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = number,
            style = DoziTypography.subtitle1,
            color = DoziColors.Primary
        )
        Spacer(modifier = Modifier.width(DoziSpacing.sm))
        Text(
            text = text,
            style = DoziTypography.body2,
            color = DoziColors.OnSurface
        )
    }
}
