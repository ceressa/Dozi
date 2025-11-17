package com.bardino.dozi.onboarding.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.theme.*

@Composable
fun OnboardingNameScreen(
    onNext: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Adım göstergesi
        Text(
            text = "Adım 1/3",
            style = MaterialTheme.typography.labelLarge,
            color = DoziBlue,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        // Dozi karakteri
        Image(
            painter = painterResource(id = R.drawable.dozi_hosgeldin),
            contentDescription = "Dozi",
            modifier = Modifier.size(120.dp)
        )

        Spacer(Modifier.height(24.dp))

        // Mesaj
        Card(
            colors = CardDefaults.cardColors(
                containerColor = DoziTurquoise.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Seni nasıl çağırayım? 😊",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DoziCoralDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(20.dp)
            )
        }

        Spacer(Modifier.height(48.dp))

        // İsim girişi
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                showError = false
            },
            label = { Text("Adın") },
            placeholder = { Text("Örn: Ufuk") },
            singleLine = true,
            isError = showError,
            supportingText = {
                if (showError) {
                    Text(
                        text = "Lütfen adını gir",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DoziBlue,
                focusedLabelColor = DoziBlue,
                cursorColor = DoziBlue
            )
        )

        Spacer(Modifier.weight(1f))

        // Kaydet butonu - Daha belirgin
        Button(
            onClick = {
                if (name.isBlank()) {
                    showError = true
                } else {
                    onNext(name.trim())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DoziCoral
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Text(
                text = "Kaydet ve Devam Et",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}