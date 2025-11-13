package com.bardino.dozi.core.ui.voice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bardino.dozi.core.data.IlacJsonRepository
import com.bardino.dozi.core.ui.theme.DoziCoral
import com.bardino.dozi.core.ui.theme.DoziTurquoise
import com.bardino.dozi.core.voice.ParsedMedicineCommand
import com.bardino.dozi.core.voice.parseVoiceCommand
import com.bardino.dozi.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Uygulama-içi sesli asistan:
 * - İkonla açılır
 * - Dinleme sırasında animasyonlu dalga
 * - Konuşma bitince "Dozi düşünüyor..."
 * - Ardından popup ile onay / düzenle / iptal
 */
@Composable
fun DoziVoiceAssistantOverlay(
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf(VoiceUiState.Listening) }
    var lastHeardText by remember { mutableStateOf<String?>(null) }
    var parsed by remember { mutableStateOf<ParsedMedicineCommand?>(null) }

    // ... diğer kodlar aynı kalacak ...


    // Mikrofon izni
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Mikrofon izni gerekli.", Toast.LENGTH_LONG).show()
            onDismiss()
        }
    }

    // Speech launcher
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            lastHeardText = spoken
            uiState = VoiceUiState.Thinking
        } else {
            Toast.makeText(context, "Konuşma algılanamadı.", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    // İlk açılışta izin ve dinleme
    LaunchedEffect(Unit) {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        delay(150)
        startSpeech(speechLauncher)
    }

    // Thinking -> Parse ve popup gösterimi
    LaunchedEffect(uiState) {
        if (uiState == VoiceUiState.Thinking) {
            delay(400)
            parsed = lastHeardText?.let { parseVoiceCommand(it) }
            uiState = VoiceUiState.Result
        }
    }

    // Arka plan overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 22.dp, vertical = 20.dp)
                    .size(width = 340.dp, height = 420.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Üst bar
                Row(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "Dozi Sesli Asistan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Kapat")
                    }
                }

                // İçerik
                when (uiState) {
                    VoiceUiState.Listening -> {
                        Text("🎧 Dozi seni dinliyor…", color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        VoiceWaveAnimation(
                            barCount = 5,
                            barWidth = 10.dp,
                            barMaxHeight = 80.dp,
                            color = DoziTurquoise
                        )
                        Spacer(Modifier.height(24.dp))
                        IconButton(
                            onClick = { /* Dinleme aktif */ },
                            modifier = Modifier
                                .size(56.dp)
                                .background(DoziCoral.copy(alpha = 0.15f), shape = CircleShape),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = DoziCoral)
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null)
                        }
                    }

                    VoiceUiState.Thinking -> {
                        Text("💭 Dozi düşünüyor…", color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        ThinkingDots()
                        Spacer(Modifier.height(24.dp))
                        Text(lastHeardText ?: "", fontWeight = FontWeight.Medium)
                    }

                    // ✅ Düzenlenen kısım burası
                    VoiceUiState.Result -> {
                        val p = parsed
                        if (p == null) {
                            Text("Komutu anlayamadım.", color = Color.Red)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = {
                                    startSpeech(speechLauncher)
                                    uiState = VoiceUiState.Listening
                                }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Tekrar Dinle")
                                }
                                TextButton(onClick = onDismiss) { Text("İptal") }
                            }
                        } else {
                            // 🔍 Komut başarıyla çözümlendi
                            // Örnek: "Akşam 5 Lustral" -> name=Lustral, hour=17
                            val matches = IlacJsonRepository.search(context, p.name)

                            if (matches.isNotEmpty()) {
                                val ilac = matches.first().item
                                // İlacı buldu → AddReminderScreen’a yönlendir
                                navController.currentBackStackEntry?.savedStateHandle?.set("voice_medicine", ilac.Product_Name)
                                navController.currentBackStackEntry?.savedStateHandle?.set("voice_hour", p.hour)
                                navController.currentBackStackEntry?.savedStateHandle?.set("voice_minute", p.minute)
                                navController.currentBackStackEntry?.savedStateHandle?.set("voice_flow", "known")

                                navController.navigate(Screen.AddReminder.route)
                            } else {
                                // İlacı bulamadı → MedicineLookupScreen’a yönlendir
                                navController.currentBackStackEntry?.savedStateHandle?.set("voice_query", p.name)
                                navController.currentBackStackEntry?.savedStateHandle?.set("voice_hour", p.hour)
                                navController.currentBackStackEntry?.savedStateHandle?.set("voice_minute", p.minute)
                                navController.currentBackStackEntry?.savedStateHandle?.set("voice_flow", "unknown")

                                navController.navigate(Screen.MedicineLookup.route)
                            }

                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

private enum class VoiceUiState { Listening, Thinking, Result }

private fun startSpeech(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "İlacını söyle. Örn: 'Sabah 9'da Parol 500 mg'")
    }
    launcher.launch(intent)
}

@Composable
private fun VoiceWaveAnimation(
    barCount: Int,
    barWidth: Dp,
    barMaxHeight: Dp,
    color: Color
) {
    val infinite = rememberInfiniteTransition(label = "wave")
    val anims = (0 until barCount).map { i ->
        val delay = i * 120
        infinite.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = 900, delayMillis = delay, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "bar-$i"
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        anims.forEach { anim ->
            val h = barMaxHeight * anim.value
            Canvas(Modifier.size(width = barWidth, height = barMaxHeight)) {
                val top = size.height - h.toPx()
                drawLine(
                    color = color,
                    start = Offset(x = size.width / 2f, y = size.height),
                    end = Offset(x = size.width / 2f, y = top),
                    strokeWidth = barWidth.toPx()
                )
            }
        }
    }
}

@Composable
private fun ThinkingDots() {
    val infinite = rememberInfiniteTransition(label = "dots")
    val a by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "dots-anim"
    )
    val dots = when {
        a < 0.33f -> "."
        a < 0.66f -> ".."
        else -> "..."
    }
    Text("Analiz ediliyor$dots", color = Color.Gray)
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label:", color = Color.Gray)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
