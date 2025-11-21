package com.bardino.dozi.core.ui.screens.medicine

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bardino.dozi.core.data.LocalMedicine
import com.bardino.dozi.core.data.MedicineLookupRepository
import com.bardino.dozi.core.data.model.MedicineColor
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

// Form tipleri
private val formTypes = listOf(
    "tablet" to "Tablet",
    "kapsül" to "Kapsül",
    "şurup" to "Şurup",
    "damla" to "Damla",
    "krem" to "Krem/Merhem",
    "enjeksiyon" to "Enjeksiyon",
    "sprey" to "Sprey",
    "takviye" to "Takviye",
    "diğer" to "Diğer"
)

// Emoji ikonları
private val medicineEmojis = listOf(
    "💊", "💉", "🩹", "🧴", "🌿", "🍃", "✨", "❤️", "🧬", "🩺"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomMedicineAddScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReminder: ((String) -> Unit)? = null,
    initialName: String = ""
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Form state
    var name by remember { mutableStateOf(initialName) }
    var stock by remember { mutableStateOf("") }
    var selectedForm by remember { mutableStateOf("tablet") }
    var selectedEmoji by remember { mutableStateOf("💊") }
    var selectedColor by remember { mutableStateOf(MedicineColor.GREEN) } // Takviyeler için yeşil default

    // Error states
    var nameError by remember { mutableStateOf(false) }
    var stockError by remember { mutableStateOf(false) }

    // Dialog states
    var showReminderDialog by remember { mutableStateOf(false) }
    var savedMedicineId by remember { mutableStateOf<String?>(null) }

    // Animasyon
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "Özel İlaç Ekle",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                backgroundColor = MaterialTheme.colorScheme.surface
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Başlık ve açıklama
                AnimatedVisibility(visible = isVisible, enter = fadeIn()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = DoziTurquoise.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = selectedEmoji,
                                        fontSize = 24.sp
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Özel İlaç Ekle",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Takviye, vitamin veya listede olmayan ilaçlar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // İlaç Adı
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "İlaç/Takviye Adı",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it; nameError = it.isBlank() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9F9FB), RoundedCornerShape(12.dp)),
                                placeholder = {
                                    Text(
                                        "Örn: Omega-3, D Vitamini, Probiyotik...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, null, tint = DoziTurquoise)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DoziTurquoise,
                                    unfocusedBorderColor = VeryLightGray,
                                    cursorColor = DoziTurquoise,
                                    focusedContainerColor = Color(0xFFF9F9FB),
                                    unfocusedContainerColor = Color(0xFFF9F9FB)
                                ),
                                singleLine = true,
                                isError = nameError
                            )
                            if (nameError) {
                                Text(
                                    text = "İlaç adı boş bırakılamaz",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Stok Sayısı
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Evdeki Stok Miktarı",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = stock,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        val num = newValue.toIntOrNull()
                                        if (num == null || num in 1..999) {
                                            stock = newValue
                                            stockError = newValue.isEmpty()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9F9FB), RoundedCornerShape(12.dp)),
                                placeholder = {
                                    Text(
                                        "Kaç adet/tablet/kapsül var?",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Inventory, null, tint = DoziCoral)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DoziTurquoise,
                                    unfocusedBorderColor = VeryLightGray,
                                    cursorColor = DoziTurquoise,
                                    focusedContainerColor = Color(0xFFF9F9FB),
                                    unfocusedContainerColor = Color(0xFFF9F9FB)
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = stockError
                            )
                            if (stockError) {
                                Text(
                                    text = "Stok miktarı giriniz (1-999)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Form Tipi Seçimi
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Form Tipi",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(formTypes) { (value, label) ->
                                    FormTypeChip(
                                        label = label,
                                        selected = selectedForm == value,
                                        onClick = { selectedForm = value }
                                    )
                                }
                            }
                        }

                        // Emoji Seçimi
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "İkon Seç",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(medicineEmojis) { emoji ->
                                    EmojiChip(
                                        emoji = emoji,
                                        selected = selectedEmoji == emoji,
                                        onClick = { selectedEmoji = emoji }
                                    )
                                }
                            }
                        }

                        // Renk Seçimi
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Renk Kategorisi",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(MedicineColor.entries.toList()) { color ->
                                    ColorChip(
                                        color = color,
                                        selected = selectedColor == color,
                                        onClick = { selectedColor = color }
                                    )
                                }
                            }
                        }

                        // Bilgi kartı
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = DoziTurquoise.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = DoziTurquoise
                                )
                                Text(
                                    text = "Özel ilaçlar resmi veritabanında olmayan ilaçlar için kullanılır. İlaç eklendikten sonra hatırlatma kurabilirsiniz.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Kaydet Butonu - Altta sabit
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = {
                            nameError = name.isBlank()
                            stockError = stock.isEmpty() || stock.toIntOrNull() == null

                            if (!nameError && !stockError) {
                                val newId = UUID.randomUUID().toString()
                                val localMedicine = LocalMedicine(
                                    id = newId,
                                    name = name.trim(),
                                    dosage = "",
                                    stock = stock.toInt()
                                )

                                // Local'e kaydet
                                MedicineLookupRepository.saveLocalMedicine(context, localMedicine)

                                // Firestore'a kaydet (isCustom = true)
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    try {
                                        val firestoreRepo = com.bardino.dozi.core.data.repository.MedicineRepository()
                                        val firestoreMedicine = com.bardino.dozi.core.data.model.Medicine(
                                            id = "",
                                            name = name.trim(),
                                            stockCount = stock.toInt(),
                                            boxSize = stock.toInt(),
                                            form = selectedForm,
                                            icon = selectedEmoji,
                                            color = selectedColor,
                                            reminderEnabled = false,
                                            isCustom = true  // Özel ilaç olarak işaretle
                                        )
                                        val saved = firestoreRepo.addMedicine(firestoreMedicine, context)

                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            if (saved != null) {
                                                Toast.makeText(
                                                    context,
                                                    "✅ ${name.trim()} eklendi!",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                // Hatırlatma dialog'unu göster - Firestore ID kullan
                                                if (onNavigateToReminder != null) {
                                                    savedMedicineId = saved.id  // Firestore'dan dönen ID'yi kullan
                                                    showReminderDialog = true
                                                } else {
                                                    onNavigateBack()
                                                }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "⚠️ İlaç yerel olarak kaydedildi",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                // Firestore başarısız olsa da local ID ile devam et
                                                if (onNavigateToReminder != null) {
                                                    savedMedicineId = newId
                                                    showReminderDialog = true
                                                } else {
                                                    onNavigateBack()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("CustomMedicineAdd", "Firestore kayıt hatası", e)
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "⚠️ İlaç yerel olarak kaydedildi",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // Hata durumunda da local ID ile devam et
                                            if (onNavigateToReminder != null) {
                                                savedMedicineId = newId
                                                showReminderDialog = true
                                            } else {
                                                onNavigateBack()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoziTurquoise)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "İlaç Ekle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Hatırlatma ekleme dialog'u
    if (showReminderDialog && savedMedicineId != null) {
        AlertDialog(
            onDismissRequest = {
                showReminderDialog = false
                onNavigateBack()
            },
            icon = {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = DoziTurquoise,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Hatırlatma Ekle",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "$name için bir hatırlatma kurmak ister misin?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hatırlatma kurarak ilaçlarını düzenli alabilirsin!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReminderDialog = false
                        onNavigateToReminder?.invoke(savedMedicineId!!)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DoziTurquoise)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Evet, Ekle", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReminderDialog = false
                    onNavigateBack()
                }) {
                    Text("Şimdi Değil", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun FormTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) DoziTurquoise else Color.Transparent,
        border = if (!selected) androidx.compose.foundation.BorderStroke(1.dp, VeryLightGray) else null
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmojiChip(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = if (selected) DoziTurquoise.copy(alpha = 0.15f) else Color(0xFFF5F5F5),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, DoziTurquoise) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = 20.sp)
        }
    }
}

@Composable
private fun ColorChip(
    color: MedicineColor,
    selected: Boolean,
    onClick: () -> Unit
) {
    val chipColor = Color(android.graphics.Color.parseColor(color.hexColor))

    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = chipColor,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, VeryLightGray)
        }
    ) {
        if (selected) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
