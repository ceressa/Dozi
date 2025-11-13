package com.bardino.dozi.onboarding.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.navigation.NavController
import com.bardino.dozi.core.ui.screens.home.HomeScreen
import com.bardino.dozi.onboarding.components.SpotlightOverlay
import com.bardino.dozi.onboarding.components.SpotlightTarget
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OnboardingHomeTourScreen(
    navController: NavController,
    onNext: () -> Unit,
    onNavigateToMedicines: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var target by remember { mutableStateOf<SpotlightTarget?>(null) }

    // Ana ekran
    Box(Modifier.fillMaxSize()) {
        HomeScreen(
            navController = navController,
            contentPadding = PaddingValues(),
            onNavigateToMedicines = onNavigateToMedicines,
            onNavigateToReminders = onNavigateToReminders,
            onNavigateToProfile = onNavigateToProfile
        )

        SpotlightOverlay(
            target = target,
            onNext = {
                step++
                when (step) {
                    1 -> target = SpotlightTarget(
                        rect = Rect(80f, 600f, 800f, 800f),
                        message = "Zaman çizelgesi burada! İlaç geçmişini görüntüleyebilirsin."
                    )
                    2 -> target = SpotlightTarget(
                        rect = Rect(100f, 1800f, 900f, 2000f),
                        message = "Alt menüden diğer bölümlere geçebilirsin."
                    )
                    else -> onNext()
                }
            }
        )
    }

    // İlk spotlight
    LaunchedEffect(Unit) {
        delay(800)
        target = SpotlightTarget(
            rect = Rect(80f, 350f, 800f, 500f),
            message = "Burası ana ekran. İlaç hatırlatmalarını burada görebilirsin."
        )
    }
}
