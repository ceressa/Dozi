package com.bardino.dozi.onboarding.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.bardino.dozi.core.ui.screens.reminder.AddReminderScreen
import com.bardino.dozi.onboarding.components.SpotlightOverlay
import com.bardino.dozi.onboarding.components.SpotlightTarget
import kotlinx.coroutines.delay

/**
 * Hatırlatma kurma eğitimi ekranı
 * AddReminderScreen'i spotlight mode ile açar
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OnboardingReminderScreen(
    navController: NavController,
    onNext: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var target by remember { mutableStateOf<SpotlightTarget?>(null) }

    // Spotlight hedefleri (örnek ölçüler)
    var timeRect by remember { mutableStateOf<Rect?>(null) }
    var frequencyRect by remember { mutableStateOf<Rect?>(null) }
    var saveRect by remember { mutableStateOf<Rect?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        // AddReminderScreen'i normal şekilde çağır
        AddReminderScreen(
            navController = navController as NavHostController,
            onNavigateBack = onNext,
            modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                val width = layoutCoordinates.size.width.toFloat()
                val height = layoutCoordinates.size.height.toFloat()

                // Basit sabit konumlar — dinamik ölçüm gerekirse ilgili alanlara onGloballyPositioned eklenebilir
                frequencyRect = Rect(
                    left = width * 0.05f,
                    top = height * 0.25f,
                    right = width * 0.95f,
                    bottom = height * 0.45f
                )

                timeRect = Rect(
                    left = width * 0.05f,
                    top = height * 0.55f,
                    right = width * 0.95f,
                    bottom = height * 0.70f
                )

                saveRect = Rect(
                    left = width * 0.05f,
                    top = height * 0.88f,
                    right = width * 0.95f,
                    bottom = height * 0.97f
                )
            }
        )

        // Spotlight overlay
        SpotlightOverlay(
            target = target,
            onNext = {
                step++
                when (step) {
                    1 -> frequencyRect?.let {
                        target = SpotlightTarget(
                            rect = it,
                            message = "🔁 İlaç alma sıklığını buradan belirleyebilirsin."
                        )
                    }
                    2 -> timeRect?.let {
                        target = SpotlightTarget(
                            rect = it,
                            message = "⏰ İlaç saatini seçmek için buraya dokun."
                        )
                    }
                    3 -> saveRect?.let {
                        target = SpotlightTarget(
                            rect = it,
                            message = "✅ Her şey hazır! Kaydet butonuna basarak hatırlatmayı oluştur."
                        )
                    }
                    else -> onNext()
                }
            }
        )
    }

    // Tur başlangıcı
    LaunchedEffect(Unit) {
        delay(1200)
        frequencyRect?.let {
            target = SpotlightTarget(
                rect = it,
                message = "💡 Hatırlatma kurma turuna hoş geldin!"
            )
        }
    }
}
