package com.bardino.dozi.core.ui.screens.settings

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.data.repository.UserRepository
import com.bardino.dozi.core.data.ThemePreferences
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import com.bardino.dozi.notifications.NotificationHelper
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfileManagement: () -> Unit = {},
    onNavigateToProfileDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()

    var userData by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Firestore'dan kullanıcı verilerini çek
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                userData = userRepository.getUserData()
                isLoading = false
            } catch (e: Exception) {
                Toast.makeText(context, "Ayarlar yüklenemedi", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "Ayarlar",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                backgroundColor = MaterialTheme.colorScheme.surface
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                // Profil Ayarları
                SettingsSection(title = "Profil") {
                    ProfileSettingsSection(
                        onNavigateToProfileManagement = onNavigateToProfileManagement,
                        onNavigateToProfileDashboard = onNavigateToProfileDashboard
                    )
                }

                // Tema Ayarı
                SettingsSection(title = "Görünüm") {
                    var selectedTheme by remember { mutableStateOf(userData?.theme ?: "system") }

                    SettingsDropdown(
                        label = "Tema",
                        icon = Icons.Default.Palette,
                        options = listOf("light" to "Açık", "dark" to "Koyu", "system" to "Sistem"),
                        selectedValue = selectedTheme,
                        onValueChange = { newTheme ->
                            selectedTheme = newTheme
                            scope.launch {
                                try {
                                    // 🎨 DataStore'a kaydet (gerçek zamanlı için)
                                    ThemePreferences.saveTheme(context, newTheme)

                                    // 📦 Firestore'a kaydet (senkronizasyon için)
                                    userRepository.updateUserField("theme", newTheme)

                                    val themeText = when (newTheme) {
                                        "dark" -> "Koyu tema"
                                        "light" -> "Açık tema"
                                        else -> "Sistem teması"
                                    }
                                    Toast.makeText(context, "$themeText etkinleştirildi", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                // Dil Ayarı
                SettingsSection(title = "Bölge") {
                    var selectedLanguage by remember { mutableStateOf(userData?.language ?: "tr") }

                    SettingsDropdown(
                        label = "Dil",
                        icon = Icons.Default.Language,
                        options = listOf("tr" to "Türkçe", "en" to "English"),
                        selectedValue = selectedLanguage,
                        onValueChange = { newLang ->
                            selectedLanguage = newLang
                            scope.launch {
                                try {
                                    userRepository.updateUserField("language", newLang)
                                    Toast.makeText(context, "Dil güncellendi", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    var selectedTimezone by remember { mutableStateOf(userData?.timezone ?: "Europe/Istanbul") }

                    SettingsDropdown(
                        label = "Saat Dilimi",
                        icon = Icons.Default.AccessTime,
                        options = listOf(
                            "Europe/Istanbul" to "İstanbul (GMT+3)",
                            "Europe/London" to "Londra (GMT+0)",
                            "America/New_York" to "New York (GMT-5)"
                        ),
                        selectedValue = selectedTimezone,
                        onValueChange = { newTimezone ->
                            selectedTimezone = newTimezone
                            scope.launch {
                                try {
                                    userRepository.updateUserField("timezone", newTimezone)
                                    Toast.makeText(context, "Saat dilimi güncellendi", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                // Bildirim Ayarları
                SettingsSection(title = "Bildirimler") {
                    var vibrationEnabled by remember { mutableStateOf(userData?.vibration ?: true) }

                    SettingsSwitch(
                        label = "Titreşim",
                        description = "Bildirimler için titreşim",
                        icon = Icons.Default.Vibration,
                        checked = vibrationEnabled,
                        onCheckedChange = { isEnabled ->
                            vibrationEnabled = isEnabled
                            scope.launch {
                                try {
                                    userRepository.updateUserField("vibration", isEnabled)
                                    Toast.makeText(
                                        context,
                                        if (isEnabled) "Titreşim açık" else "Titreşim kapalı",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 🔔 Test Bildirimi Butonu
                    TestNotificationButton()
                }

                // Ses Ayarları
                SettingsSection(title = "Sesli Asistan") {
                    var selectedVoiceGender by remember { mutableStateOf(userData?.voiceGender ?: "erkek") }

                    SettingsDropdown(
                        label = "Ses Seçimi",
                        icon = Icons.Default.RecordVoiceOver,
                        options = listOf(
                            "erkek" to "🎙️ Ozan (Erkek Ses)",
                            "kadin" to "🎙️ Efsun (Kadın Ses)"
                        ),
                        selectedValue = selectedVoiceGender,
                        onValueChange = { newVoice ->
                            selectedVoiceGender = newVoice
                            scope.launch {
                                try {
                                    userRepository.updateUserField("voiceGender", newVoice)
                                    Toast.makeText(
                                        context,
                                        "Ses değiştirildi: ${if (newVoice == "erkek") "Ozan" else "Efsun"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Örnek Ses Dinleme Butonları
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                com.bardino.dozi.core.utils.SoundHelper.playSampleSound(context, "erkek")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedVoiceGender == "erkek") DoziTurquoise.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Ozan'ı Dinle")
                        }

                        OutlinedButton(
                            onClick = {
                                com.bardino.dozi.core.utils.SoundHelper.playSampleSound(context, "kadin")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedVoiceGender == "kadin") DoziTurquoise.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Efsun'u Dinle")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdown(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = options.find { it.first == selectedValue }?.second ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(icon, contentDescription = null, tint = DoziTurquoise)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DoziTurquoise,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, displayText) ->
                DropdownMenuItem(
                    text = { Text(displayText) },
                    onClick = {
                        onValueChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = DoziTurquoise,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DoziTurquoise,
                checkedTrackColor = DoziTurquoise.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun TestNotificationButton() {
    val context = LocalContext.current

    Button(
        onClick = {
            // Bildirim izni kontrolü
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Android 13 altında izin gerekmiyor
            }

            if (hasPermission) {
                // Test bildirimi gönder
                try {
                    NotificationHelper.showMedicationNotification(
                        context = context,
                        medicineName = "Lustral",
                        dosage = "100mg",
                        time = "12:00"
                    )
                    Toast.makeText(context, "✅ Test bildirimi gönderildi!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "❌ Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(
                    context,
                    "⚠️ Bildirim izni verilmemiş. Lütfen uygulama ayarlarından izin verin.",
                    Toast.LENGTH_LONG
                ).show()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = DoziPurple
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Test Bildirimi Gönder",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
