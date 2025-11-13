package com.bardino.dozi.core.ui.screens.reminder

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.components.EmptyReminderList
import com.bardino.dozi.core.ui.theme.*

data class ReminderItem(
    val id: String,
    val reminderTitle: String?,
    val medicineName: String,
    val dosage: String, // ✅ String olarak (1, 0.5, 0.25, 2, 3, custom value)
    val hour: Int,
    val minute: Int,
    val frequency: String,
    val xValue: Int,
    val selectedDates: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddReminder: () -> Unit
) {
    val context = LocalContext.current
    var reminders by remember { mutableStateOf(loadReminders(context)) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { isVisible = true }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "Hatırlatmalarım",
                canNavigateBack = false, // ✅ Ana sayfalardan biri olduğu için false
                backgroundColor = Color.White
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddReminder,
                containerColor = DoziTurquoise,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.shadow(10.dp, RoundedCornerShape(18.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Hatırlatma")
            }
        },
        containerColor = BackgroundLight
    ) { padding ->

        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyReminderList(onAddReminder = onNavigateToAddReminder)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Üst Bilgi Alanı
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(DoziBlue, DoziTurquoise)
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "💡 Toplam ${reminders.size} hatırlatma",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "İlaçlarınızı zamanında almanız için hatırlatmalar burada.",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Hatırlatma Listesi
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
                        ) {
                            ReminderCard(
                                reminder = reminder,
                                onDelete = {
                                    deleteReminder(context, reminder.id)
                                    reminders = loadReminders(context)
                                }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderItem,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Başlık satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.reminderTitle?.takeIf { it.isNotBlank() }
                            ?: reminder.medicineName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DoziTurquoise
                    )
                    if (reminder.reminderTitle?.isNotBlank() == true) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = reminder.medicineName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = ErrorRed
                    )
                }
            }

            Divider(color = Gray200, thickness = 1.dp)

            // Bilgi satırları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dozaj
                InfoTag(
                    icon = Icons.Default.Medication,
                    text = formatDosage(reminder.dosage),
                    color = DoziCoral
                )

                // Saat
                InfoTag(
                    icon = Icons.Default.Schedule,
                    text = "%02d:%02d".format(reminder.hour, reminder.minute),
                    color = DoziBlue
                )
            }

            // Sıklık bilgisi
            InfoTag(
                icon = Icons.Default.CalendarMonth,
                text = formatFrequency(reminder.frequency, reminder.xValue, reminder.selectedDates),
                color = SuccessGreen,
                fullWidth = true
            )
        }
    }

    // Silme onay dialogu
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Hatırlatmayı Sil?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "${reminder.reminderTitle?.takeIf { it.isNotBlank() } ?: reminder.medicineName} hatırlatmasını silmek istediğinize emin misiniz?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    border = BorderStroke(1.dp, Gray200)
                ) {
                    Text("İptal", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
private fun InfoTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    fullWidth: Boolean = false
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ✅ Dozaj formatı
private fun formatDosage(dosage: String): String {
    return when (dosage) {
        "0.5" -> "Yarım adet"
        "0.25" -> "Çeyrek adet"
        "1" -> "1 adet"
        "2" -> "2 adet"
        "3" -> "3 adet"
        else -> "$dosage adet"
    }
}

// ✅ Sıklık formatı
private fun formatFrequency(frequency: String, xValue: Int, dates: List<String>): String {
    return when (frequency) {
        "Her X günde bir" -> "Her $xValue günde bir"
        "İstediğim tarihlerde" -> "${dates.size} tarih seçildi"
        else -> frequency
    }
}

// 🧠 SharedPreferences yükleme
private fun loadReminders(context: Context): List<ReminderItem> {
    val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    return prefs.all.mapNotNull { (key, value) ->
        try {
            val parts = value.toString().split("|").associate {
                val (k, v) = it.split("=", limit = 2)
                k to v
            }
            ReminderItem(
                id = key,
                reminderTitle = parts["reminderTitle"]?.takeIf { it.isNotBlank() },
                medicineName = parts["medicineName"] ?: "",
                dosage = parts["dosage"] ?: "1", // ✅ String olarak
                hour = parts["hour"]?.toIntOrNull() ?: 0,
                minute = parts["minute"]?.toIntOrNull() ?: 0,
                frequency = parts["frequency"] ?: "Her gün",
                xValue = parts["xValue"]?.toIntOrNull() ?: 2,
                selectedDates = parts["dates"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }.sortedByDescending { it.id }
}

private fun deleteReminder(context: Context, id: String) {
    val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    prefs.edit().remove(id).apply()
}