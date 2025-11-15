package com.bardino.dozi.core.ui.screens.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedNotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "Gelişmiş Bildirim Ayarları",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                backgroundColor = DoziTurquoise
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DoziTurquoise)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 🔕 DND (Do Not Disturb) Settings
                DndSettingsCard(
                    enabled = uiState.dndEnabled,
                    startHour = uiState.dndStartHour,
                    startMinute = uiState.dndStartMinute,
                    endHour = uiState.dndEndHour,
                    endMinute = uiState.dndEndMinute,
                    onEnabledChange = { viewModel.updateDndEnabled(it) },
                    onStartTimeClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                viewModel.updateDndStartTime(hour, minute)
                            },
                            uiState.dndStartHour,
                            uiState.dndStartMinute,
                            true
                        ).show()
                    },
                    onEndTimeClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                viewModel.updateDndEndTime(hour, minute)
                            },
                            uiState.dndEndHour,
                            uiState.dndEndMinute,
                            true
                        ).show()
                    }
                )

                // ⏰ Adaptive Timing Settings
                AdaptiveTimingCard(
                    enabled = uiState.adaptiveTimingEnabled,
                    morningHour = uiState.preferredMorningHour,
                    eveningHour = uiState.preferredEveningHour,
                    onEnabledChange = { viewModel.updateAdaptiveTimingEnabled(it) },
                    onMorningHourChange = { viewModel.updatePreferredMorningHour(it) },
                    onEveningHourChange = { viewModel.updatePreferredEveningHour(it) }
                )

                // 🧠 Smart Reminder Settings
                SmartReminderCard(
                    enabled = uiState.smartReminderEnabled,
                    onEnabledChange = { viewModel.updateSmartReminderEnabled(it) }
                )

                // Bilgilendirme
                InfoCard()
            }
        }

        // Error Snackbar
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // TODO: Show snackbar
                viewModel.clearError()
            }
        }
    }
}

@Composable
private fun DndSettingsCard(
    enabled: Boolean,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    onEnabledChange: (Boolean) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(DoziPurple.copy(alpha = 0.2f), DoziBlue.copy(alpha = 0.1f))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DoNotDisturb,
                            contentDescription = null,
                            tint = DoziPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "🔕 Rahatsız Etme Modu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Belirli saatlerde sessiz bildirimler",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DoziPurple
                    )
                )
            }

            if (enabled) {
                Divider()

                // Start Time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onStartTimeClick)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = DoziPurple
                        )
                        Text(
                            text = "Başlangıç Saati",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(
                        text = String.format("%02d:%02d", startHour, startMinute),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DoziPurple
                    )
                }

                // End Time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEndTimeClick)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = WarningOrange
                        )
                        Text(
                            text = "Bitiş Saati",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(
                        text = String.format("%02d:%02d", endHour, endMinute),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange
                    )
                }

                // Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            DoziPurple.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💡 Bu saatler arasında bildirimler sessiz gösterilecek. Kritik ilaçlar etkilenmez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DoziPurple
                    )
                }
            }
        }
    }
}

@Composable
private fun AdaptiveTimingCard(
    enabled: Boolean,
    morningHour: Int,
    eveningHour: Int,
    onEnabledChange: (Boolean) -> Unit,
    onMorningHourChange: (Int) -> Unit,
    onEveningHourChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(DoziTurquoise.copy(alpha = 0.2f), DoziBlue.copy(alpha = 0.1f))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = DoziTurquoise,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "⏰ Uyarlanabilir Zamanlama",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tercih ettiğiniz saatlere göre ayarla",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DoziTurquoise
                    )
                )
            }

            if (enabled) {
                Divider()

                // Morning Hour
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Sabah Tercihi: ${String.format("%02d:00", morningHour)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Slider(
                        value = morningHour.toFloat(),
                        onValueChange = { onMorningHourChange(it.toInt()) },
                        valueRange = 6f..11f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = WarningOrange,
                            activeTrackColor = WarningOrange
                        )
                    )
                }

                // Evening Hour
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = DoziPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Akşam Tercihi: ${String.format("%02d:00", eveningHour)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Slider(
                        value = eveningHour.toFloat(),
                        onValueChange = { onEveningHourChange(it.toInt()) },
                        valueRange = 18f..22f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = DoziPurple,
                            activeTrackColor = DoziPurple
                        )
                    )
                }

                // Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            DoziTurquoise.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💡 Sabah ve akşam ilaç hatırlatmaları tercih ettiğiniz saatlere kaydırılır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DoziTurquoise
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartReminderCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(DoziBlue.copy(alpha = 0.2f), DoziTurquoise.copy(alpha = 0.1f))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = DoziBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "🧠 Akıllı Hatırlatma Önerileri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Alışkanlıklarınıza göre öneriler",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DoziBlue
                    )
                )
            }

            // Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        DoziBlue.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Aktif olduğunda:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DoziBlue
                    )
                    Text(
                        text = "• En çok kullandığınız erteleme süreleri ⭐ ile işaretlenir",
                        style = MaterialTheme.typography.bodySmall,
                        color = DoziBlue
                    )
                    Text(
                        text = "• Geçen hatırlatmalarınız analiz edilerek size özel öneriler sunulur",
                        style = MaterialTheme.typography.bodySmall,
                        color = DoziBlue
                    )
                    Text(
                        text = "• \"Hep 30 dk geç alıyorsunuz, zamanı değiştirmek ister misiniz?\" gibi akıllı bildirimler alırsınız",
                        style = MaterialTheme.typography.bodySmall,
                        color = DoziBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            SuccessGreen.copy(alpha = 0.08f),
                            DoziTurquoise.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(SuccessGreen, DoziTurquoise)
                                ),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Bilgi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Bu ayarlar tüm cihazlarınızda senkronize edilir ve ilaç hatırlatmalarınızı kişiselleştirir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
