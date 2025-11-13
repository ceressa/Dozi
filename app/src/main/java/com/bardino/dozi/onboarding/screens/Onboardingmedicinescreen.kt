package com.bardino.dozi.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bardino.dozi.core.ui.screens.medicine.MedicineLookupScreen
import com.bardino.dozi.onboarding.components.SpotlightOverlay
import com.bardino.dozi.onboarding.components.SpotlightTarget
import kotlinx.coroutines.delay

@Composable
fun OnboardingMedicineScreen(
    navController: NavController,
    onNext: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var target by remember { mutableStateOf<SpotlightTarget?>(null) }

    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val screenHeight = config.screenHeightDp.dp

    // Ekran boyutuna göre dinamik hesaplama
    val searchRect = remember {
        Rect(
            left = 40f,
            top = 280f,
            right = screenWidth.value * 3f - 40f,
            bottom = 380f
        )
    }

    val barcodeRect = remember {
        Rect(
            left = screenWidth.value * 3f - 120f,
            top = 200f,
            right = screenWidth.value * 3f - 40f,
            bottom = 260f
        )
    }

    val voiceRect = remember {
        Rect(
            left = screenWidth.value * 3f - 200f,
            top = 200f,
            right = screenWidth.value * 3f - 130f,
            bottom = 260f
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MedicineLookupScreen(
            navController = navController,
            onNavigateBack = {
                // İlaç eklendikten sonra devam
                if (step >= 2) {
                    onNext()
                }
            }
        )

        SpotlightOverlay(
            target = target,
            onNext = {
                step++
                when (step) {
                    1 -> target = SpotlightTarget(
                        rect = voiceRect,
                        message = "🎤 Buraya tıklayarak sesle ilaç arayabilirsin"
                    )
                    2 -> target = SpotlightTarget(
                        rect = barcodeRect,
                        message = "📷 Ya da barkod okutarak ilacını hızlıca ekleyebilirsin"
                    )
                    else -> target = null
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        delay(1000)
        target = SpotlightTarget(
            rect = searchRect,
            message = "🔍 İlacını buraya yazarak arayabilirsin"
        )
    }
}