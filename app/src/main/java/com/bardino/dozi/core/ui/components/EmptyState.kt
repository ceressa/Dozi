package com.bardino.dozi.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.ui.theme.*

@Composable
fun EmptyState(
    icon: Int,
    title: String,
    description: String,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(bottom = 32.dp) // 📏 Bottombar’dan kurtulmak için alt boşluk
            .navigationBarsPadding(), // 📱 Sistem alt çubuğuna göre otomatik boşluk
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // 🎯 dikeyde tam ortalama
    ) {
        // 🖼️ Görsel
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(170.dp) // 📏 biraz küçültüp tam orantı
        )

        Spacer(Modifier.height(28.dp))

        // 🧩 Başlık
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimaryLight,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        // 🧠 Açıklama
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondaryLight,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.1f,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        if (actionButton != null) {
            Spacer(Modifier.height(36.dp))
            actionButton()
        }
    }
}


@Composable
fun EmptyMedicineList(
    onAddMedicine: () -> Unit
) {
    EmptyState(
        icon = com.bardino.dozi.R.drawable.dozi_idea,
        title = "Henüz ilaç eklemediniz",
        description = "İlaçlarınızı ekleyerek takibini kolaylaştırın",
        actionButton = {
            Button(
                onClick = onAddMedicine,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DoziCoral
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("İlk İlacı Ekle")
            }
        }
    )
}


@Composable
fun EmptyReminderList(
    onAddReminder: () -> Unit
) {
    EmptyState(
        icon = com.bardino.dozi.R.drawable.dozi_time,
        title = "Henüz hatırlatma yok",
        description = "İlaç hatırlatmaları kurarak hiçbir dozu kaçırmayın",
        actionButton = {
            Button(
                onClick = onAddReminder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DoziCoral
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Hatırlatma Ekle")
            }
        }
    )
}

@Composable
fun EmptySearchResults() {
    EmptyState(
        icon = com.bardino.dozi.R.drawable.dozi_unhappy,
        title = "Sonuç bulunamadı",
        description = "Arama kriterlerinizi değiştirip tekrar deneyin"
    )
}