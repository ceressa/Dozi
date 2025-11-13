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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.data.repository.UserRepository
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
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
                backgroundColor = Color.White
            )
        },
        containerColor = BackgroundLight
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
                // Tema Ayarı
                SettingsSection(title = "Görünüm") {
                    var selectedTheme by remember { mutableStateOf(userData?.theme ?: "light") }

                    SettingsDropdown(
                        label = "Tema",
                        icon = Icons.Default.Palette,
                        options = listOf("light" to "Açık", "dark" to "Koyu", "system" to "Sistem"),
                        selectedValue = selectedTheme,
                        onValueChange = { newTheme ->
                            selectedTheme = newTheme
                            scope.launch {
                                try {
                                    userRepository.updateUserField("theme", newTheme)
                                    Toast.makeText(context, "Tema güncellendi", Toast.LENGTH_SHORT).show()
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                color = TextPrimary,
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
                    color = TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
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
