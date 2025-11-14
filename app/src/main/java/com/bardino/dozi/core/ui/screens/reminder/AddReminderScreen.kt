package com.bardino.dozi.core.ui.screens.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.bardino.dozi.R
import com.bardino.dozi.core.data.MedicineRepository
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.repository.MedicineRepository as FirestoreMedicineRepository
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.screens.profile.addGeofence
import com.bardino.dozi.core.ui.theme.*
import com.bardino.dozi.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Medicine entry data class
data class MedicineEntry(
    val id: Int,
    var name: String = "",
    var dosageType: String = "1",
    var customDosage: String = ""
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    onNavigateBack: () -> Unit,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    medicineId: String? = null  // Edit mode için medicine ID
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var selectedPlace by remember { mutableStateOf<String?>(null) }

    // Edit mode kontrolü
    val isEditMode = medicineId != null
    var isLoading by remember { mutableStateOf(isEditMode) }

    // State'ler
    var step by remember { mutableStateOf(1) }
    var medicines by remember { mutableStateOf(listOf(MedicineEntry(id = 0))) }
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    var frequency by remember { mutableStateOf("Her gün") }
    var xValue by remember { mutableStateOf(2) }
    var selectedDates by remember { mutableStateOf<List<String>>(emptyList()) }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) } // Başlangıç tarihi
    var showError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    val storedMedicines = remember {
        MedicineRepository
            .loadMedicines(context).map { it.name }
    }
    var selectedMedicineIndex by remember { mutableStateOf(-1) }

    // Ses kontrolü
    var soundEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
                .getBoolean("sound_enabled", true)
        )
    }

    // Edit mode: Mevcut medicine verisini yükle
    LaunchedEffect(medicineId) {
        if (isEditMode && medicineId != null) {
            try {
                val repository = FirestoreMedicineRepository()
                val medicine = repository.getMedicineById(medicineId)

                if (medicine != null) {
                    // Medicine verisini state'lere doldur
                    medicines = listOf(MedicineEntry(
                        id = 0,
                        name = medicine.name,
                        dosageType = medicine.dosage.split(" ").firstOrNull() ?: "1",
                        customDosage = medicine.dosage
                    ))

                    // İlk saati al
                    if (medicine.times.isNotEmpty()) {
                        val firstTime = medicine.times.first().split(":")
                        hour = firstTime.getOrNull(0)?.toIntOrNull() ?: 8
                        minute = firstTime.getOrNull(1)?.toIntOrNull() ?: 0
                    }

                    frequency = medicine.frequency
                    xValue = medicine.frequencyValue
                    selectedDates = medicine.days
                    startDate = medicine.startDate
                }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(context, "Hatırlatma yüklenirken hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // MedicineListScreen'den dönen ilaç ismini al
    LaunchedEffect(navController.currentBackStackEntry) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.get<String>("selectedMedicine")
            ?.let { selectedName ->
                if (selectedMedicineIndex >= 0 && selectedMedicineIndex < medicines.size) {
                    medicines = medicines.toMutableList().also {
                        it[selectedMedicineIndex] = it[selectedMedicineIndex].copy(name = selectedName)
                    }
                }
                selectedMedicineIndex = -1
                // Kullanıldıktan sonra temizle
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<String>("selectedMedicine")
            }
    }

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(step, soundEnabled) {
        if (soundEnabled && step <= 4) {
            playStepSound(context, step, soundEnabled)
        } else if (!soundEnabled) {
            currentMediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            currentMediaPlayer = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentMediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            currentMediaPlayer = null
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DoziTopBar(
                title = if (isEditMode) "Hatırlatma Düzenle" else "Hatırlatma Asistanı",
                canNavigateBack = true,
                onNavigateBack = {
                    if (step > 1) step-- else onNavigateBack()
                },
                actions = {
                    IconButton(
                        onClick = {
                            soundEnabled = !soundEnabled
                            context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("sound_enabled", soundEnabled)
                                .apply()

                            if (!soundEnabled) {
                                currentMediaPlayer?.apply {
                                    if (isPlaying) stop()
                                    release()
                                }
                                currentMediaPlayer = null
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (soundEnabled) "Sesi Kapat" else "Sesi Aç",
                            tint = if (soundEnabled) DoziTurquoise else TextSecondaryLight
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { focusManager.clearFocus() }
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + expandVertically()
                ) {
                    StepProgressIndicator(currentStep = step, totalSteps = 4)
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()
                ) {
                    DoziSpeechBubble(step = step, soundEnabled = soundEnabled)
                }

                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { if (targetState > initialState) 300 else -300 }
                        ) + fadeIn() togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { if (targetState > initialState) -300 else 300 }
                                ) + fadeOut()
                    },
                    label = "step_transition"
                ) { currentStep ->
                    when (currentStep) {
                        1 -> MultipleMedicinesStep(
                            medicines = medicines,
                            onMedicinesChange = { medicines = it },
                            storedMedicines = storedMedicines,
                            onAddNewMedicine = { index ->
                                selectedMedicineIndex = index
                                navController.navigate(Screen.MedicineLookup.route)
                            },
                            showError = showError
                        )

                        2 -> FrequencyStep(
                            selected = frequency,
                            xValue = xValue,
                            selectedDates = selectedDates,
                            startDate = startDate,
                            onSelect = { frequency = it },
                            onXChange = { xValue = it },
                            onDatesChange = { selectedDates = it },
                            onStartDateChange = { startDate = it },
                            context = context
                        )

                        3 -> TimeStep(
                            hour = hour,
                            minute = minute,
                            onTimeChange = { h, m -> hour = h; minute = m },
                            context = context
                        )

                        4 -> SummaryStep(
                            medicines = medicines,
                            hour = hour,
                            minute = minute,
                            frequency = frequency,
                            xValue = xValue,
                            selectedDates = selectedDates
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()
            ) {
                NavigationButtons(
                    step = step,
                    medicineName = medicines.firstOrNull()?.name ?: "",
                    onBack = { step-- },
                    onNext = {
                        when (step) {
                            1 -> {
                                if (medicines.any { it.name.isBlank() }) {
                                    showError = true
                                } else {
                                    showError = false
                                    step++
                                }
                            }
                            2 -> step++
                            3 -> step++
                            4 -> {
                                // Tüm ilaçları kaydet
                                saveMedicinesToFirestore(
                                    medicines = medicines,
                                    hour = hour,
                                    minute = minute,
                                    frequency = frequency,
                                    xValue = xValue,
                                    selectedDates = selectedDates,
                                    startDate = startDate,
                                    onSuccess = {
                                        if (soundEnabled) playSuccessSound(context)
                                        showSuccess = true
                                    },
                                    onError = {
                                        Toast.makeText(
                                            context,
                                            "❌ Hatırlatmalar kaydedilemedi",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    if (showSuccess) {
        ReminderSuccessDialog(
            onAddAnother = {
                // Başka ilaç ekle - her şeyi sıfırla
                showSuccess = false
                step = 1
                medicines = listOf(MedicineEntry(id = 0))
                frequency = "Her gün"
                selectedDates = emptyList()
                startDate = System.currentTimeMillis()
                hour = 8
                minute = 0
            },
            onFinish = {
                showSuccess = false
                onNavigateBack()
            }
        )
    }
}

private var currentMediaPlayer: MediaPlayer? = null

private fun playStepSound(context: Context, step: Int, soundEnabled: Boolean) {
    if (!soundEnabled) return

    try {
        currentMediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }

        val soundResId = when (step) {
            1 -> R.raw.step1
            2 -> R.raw.step2
            3 -> R.raw.step3
            4 -> R.raw.step4
            else -> return
        }

        currentMediaPlayer = MediaPlayer.create(context, soundResId)?.apply {
            setOnCompletionListener {
                release()
                currentMediaPlayer = null
            }
            start()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun playSuccessSound(context: Context) {
    try {
        MediaPlayer.create(context, R.raw.success)?.apply {
            setOnCompletionListener { release() }
            start()
        }
    } catch (e: Exception) {
        // Ses dosyası yoksa sessizce devam et
    }
}

// UI COMPONENTS
@Composable
private fun StepProgressIndicator(currentStep: Int, totalSteps: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = currentStep / totalSteps.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(MaterialTheme.shapes.small),
            color = DoziCoral,
            trackColor = Gray200
        )
        Text(
            text = "Adım $currentStep / $totalSteps",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun AnimatedDoziIcon(step: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dozi")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val doziImage = when (step) {
        1 -> R.drawable.dozi_noted
        2 -> R.drawable.dozi_noted2
        3 -> R.drawable.dozi_ok
        4 -> R.drawable.dozi
        else -> R.drawable.dozi
    }

    Image(
        painter = painterResource(id = doziImage),
        contentDescription = "Dozi",
        modifier = Modifier.size(48.dp).scale(scale)
    )
}

@Composable
private fun DoziSpeechBubble(step: Int, soundEnabled: Boolean) {
    val text = when (step) {
        1 -> "💊 Hangi ilaçları eklemek istersin? Birden fazla ilaç ekleyebilirsin!"
        2 -> "🔁 Ne kadar sıklıkla alıyorsun?"
        3 -> "⏰ Harika! Şimdi saati belirleyelim."
        4 -> "✅ Neredeyse bitti! Gözden geçirip kaydedelim."
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(GradientCoral),
                    shape = MaterialTheme.shapes.large
                )
                .padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedDoziIcon(step)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ADIM 1 - ÇOKLU İLAÇ EKLEME
@Composable
private fun MultipleMedicinesStep(
    medicines: List<MedicineEntry>,
    onMedicinesChange: (List<MedicineEntry>) -> Unit,
    storedMedicines: List<String>,
    onAddNewMedicine: (Int) -> Unit,
    showError: Boolean
) {
    var showPickerForIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        medicines.forEachIndexed { index, medicine ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = MaterialTheme.shapes.medium,
                border = if (showError && medicine.name.isBlank()) {
                    BorderStroke(2.dp, ErrorRed)
                } else {
                    BorderStroke(1.dp, Gray200)
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "İlaç ${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = DoziTurquoise
                        )

                        if (medicines.size > 1) {
                            IconButton(
                                onClick = {
                                    onMedicinesChange(medicines.filterIndexed { i, _ -> i != index })
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Kaldır",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // İlaç seçici
                    MedicinePickerRow(
                        selectedName = medicine.name,
                        onSelect = {
                            if (storedMedicines.isNotEmpty()) {
                                showPickerForIndex = index
                            } else {
                                onAddNewMedicine(index)
                            }
                        },
                        onAddNew = { onAddNewMedicine(index) }
                    )

                    // Dozaj seçici
                    Text(
                        "Kaç Adet?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DosageChip(
                            label = "1",
                            isSelected = medicine.dosageType == "1",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(dosageType = "1")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DosageChip(
                            label = "Yarım",
                            isSelected = medicine.dosageType == "0.5",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(dosageType = "0.5")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DosageChip(
                            label = "2",
                            isSelected = medicine.dosageType == "2",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(dosageType = "2")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DosageChip(
                            label = "Çeyrek",
                            isSelected = medicine.dosageType == "0.25",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(dosageType = "0.25")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DosageChip(
                            label = "3",
                            isSelected = medicine.dosageType == "3",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(dosageType = "3")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DosageChip(
                            label = "Diğer",
                            isSelected = medicine.dosageType == "custom",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(dosageType = "custom")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Custom dozaj girişi
                    AnimatedVisibility(
                        visible = medicine.dosageType == "custom",
                        enter = fadeIn() + expandVertically()
                    ) {
                        OutlinedTextField(
                            value = medicine.customDosage,
                            onValueChange = { newDosage ->
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(customDosage = newDosage)
                                })
                            },
                            label = { Text("Adet Girin", color = TextSecondary) },
                            placeholder = { Text("Örn: 1.5, 4, vb.") },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = DoziTurquoise) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DoziTurquoise,
                                focusedLabelColor = DoziTurquoise,
                                cursorColor = DoziTurquoise,
                                unfocusedBorderColor = Gray200,
                                focusedContainerColor = DoziTurquoise.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // + İlaç Ekle butonu
        OutlinedButton(
            onClick = {
                val newId = (medicines.maxOfOrNull { it.id } ?: 0) + 1
                onMedicinesChange(medicines + MedicineEntry(id = newId))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(2.dp, DoziTurquoise),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DoziTurquoise),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("İlaç Ekle", fontWeight = FontWeight.Bold)
        }

        // Hata mesajı
        AnimatedVisibility(
            visible = showError && medicines.any { it.name.isBlank() },
            enter = fadeIn() + expandVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = ErrorRed.copy(alpha = 0.1f)
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Lütfen tüm ilaçları seçin",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if (showPickerForIndex >= 0) {
        MedicineBottomSheet(
            items = storedMedicines,
            onPick = { selectedMedicine ->
                onMedicinesChange(medicines.toMutableList().also {
                    it[showPickerForIndex] = it[showPickerForIndex].copy(name = selectedMedicine)
                })
                showPickerForIndex = -1
            },
            onDismiss = { showPickerForIndex = -1 }
        )
    }
}

@Composable
private fun DosageChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DoziTurquoise else Color.White
        ),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) DoziTurquoise else Gray200
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else TextPrimary
            )
        }
    }
}

@Composable
private fun MedicinePickerRow(
    selectedName: String,
    onSelect: () -> Unit,
    onAddNew: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            label = { Text("İlaç Seç", color = TextSecondary) },
            placeholder = { Text("Kayıtlı ilaçlardan seç", color = TextSecondaryLight) },
            leadingIcon = {
                Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = DoziCoral)
            },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = DoziCoral)
            },
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect
                ),
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = TextPrimary,
                disabledBorderColor = Gray200,
                disabledLeadingIconColor = DoziCoral,
                disabledTrailingIconColor = DoziCoral,
                disabledLabelColor = TextSecondary,
                disabledPlaceholderColor = TextSecondaryLight,
                disabledContainerColor = DoziCoral.copy(alpha = 0.05f) // ✅ YENİ
            ),
            shape = MaterialTheme.shapes.medium
        )

        FilledTonalIconButton(
            onClick = onAddNew,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = DoziTurquoise.copy(alpha = 0.15f),
                contentColor = DoziTurquoise
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Yeni ilaç ekle"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicineBottomSheet(
    items: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (items.isEmpty()) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Kayıtlı İlaçlar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Divider(color = Gray200, thickness = 1.dp)

            items.forEach { medicine ->
                ListItem(
                    headlineContent = {
                        Text(
                            medicine,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Medication,
                            contentDescription = null,
                            tint = DoziTurquoise
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            onPick(medicine)
                        }
                        .padding(vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ADIM 2 - SIKLIK (Önceki adım 3'tü, şimdi adım 2)
@Composable
private fun FrequencyStep(
    selected: String,
    xValue: Int,
    selectedDates: List<String>,
    startDate: Long,
    onSelect: (String) -> Unit,
    onXChange: (Int) -> Unit,
    onDatesChange: (List<String>) -> Unit,
    onStartDateChange: (Long) -> Unit,
    context: Context
) {
    val options = listOf(
        "Her gün",
        "Gün aşırı",
        "Haftada bir",
        "15 günde bir",
        "Ayda bir",
        "Her X günde bir",
        "İstediğim tarihlerde"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                FrequencyOptionCard(
                    option = option,
                    isSelected = selected == option,
                    xValue = xValue,
                    selectedDates = selectedDates,
                    startDate = startDate,
                    onSelect = { onSelect(option) },
                    onXChange = onXChange,
                    onDatesChange = onDatesChange,
                    onStartDateChange = onStartDateChange,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun FrequencyOptionCard(
    option: String,
    isSelected: Boolean,
    xValue: Int,
    selectedDates: List<String>,
    startDate: Long,
    onSelect: () -> Unit,
    onXChange: (Int) -> Unit,
    onDatesChange: (List<String>) -> Unit,
    onStartDateChange: (Long) -> Unit,
    context: Context
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DoziTurquoise.copy(alpha = 0.1f) else Color.White
        ),
        shape = MaterialTheme.shapes.medium,
        border = if (isSelected) BorderStroke(2.dp, DoziTurquoise) else BorderStroke(1.dp, Gray200)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onSelect,
                        colors = RadioButtonDefaults.colors(selectedColor = DoziTurquoise)
                    )
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) DoziTurquoise else TextPrimary
                    )
                }
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = DoziTurquoise, modifier = Modifier.size(20.dp))
                }
            }

            if (option == "Her X günde bir" && isSelected) {
                Spacer(Modifier.height(12.dp))
                Divider(color = Gray200)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (xValue > 1) onXChange(xValue - 1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Azalt", tint = DoziTurquoise)
                    }
                    Surface(
                        color = DoziTurquoise.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "$xValue gün",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DoziTurquoise,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                    IconButton(onClick = { onXChange(xValue + 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "Artır", tint = DoziTurquoise)
                    }
                }
            }

            // Başlangıç tarihi seçici (İstediğim tarihlerde hariç tüm seçenekler için)
            if (isSelected && option != "İstediğim tarihlerde") {
                Spacer(Modifier.height(12.dp))
                Divider(color = Gray200)
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "📅 Başlangıç Tarihi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = DoziTurquoise
                )
                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        showStartDatePicker(context, startDate) { newStartDate ->
                            onStartDateChange(newStartDate)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(2.dp, DoziTurquoise),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DoziTurquoise)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = DoziTurquoise)
                    Spacer(Modifier.width(8.dp))
                    val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
                    Text(
                        formatter.format(Date(startDate)),
                        fontWeight = FontWeight.Bold,
                        color = DoziTurquoise
                    )
                }
            }

            if (option == "İstediğim tarihlerde" && isSelected) {
                Spacer(Modifier.height(12.dp))
                Divider(color = Gray200)
                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        showDatePicker(context) { dateString ->
                            if (!selectedDates.contains(dateString)) {
                                onDatesChange(selectedDates + dateString)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(2.dp, DoziTurquoise),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DoziTurquoise)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tarih Ekle", fontWeight = FontWeight.Medium)
                }

                if (selectedDates.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedDates) { date ->
                            DateChip(
                                date = date,
                                onRemove = {
                                    onDatesChange(selectedDates - date)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateChip(date: String, onRemove: () -> Unit) {
    Surface(
        color = DoziTurquoise.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelMedium,
                color = DoziTurquoise,
                fontWeight = FontWeight.Medium
            )
            Icon(
                Icons.Default.Close,
                contentDescription = "Kaldır",
                tint = DoziTurquoise,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

private fun showDatePicker(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, day)
            }
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            onDateSelected(formatter.format(selectedDate.time))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showStartDatePicker(context: Context, currentStartDate: Long, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = currentStartDate
    }
    val today = Calendar.getInstance()

    val picker = DatePickerDialog(
        context,
        { _, year, month, day ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(selectedDate.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Bugünden önceki tarihleri seçilemez yap
    picker.datePicker.minDate = today.timeInMillis
    picker.show()
}

// ADIM 3 - SAAT (Önceki adım 2'ydi, şimdi adım 3)
@Composable
private fun TimeStep(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    context: Context
) {
    Card(
        onClick = {
            TimePickerDialog(context, { _, h, m -> onTimeChange(h, m) }, hour, minute, true).show()
        },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(DoziBlue, DoziBlue.copy(alpha = 0.8f))
                    ),
                    shape = MaterialTheme.shapes.large
                )
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "%02d:%02d".format(hour, minute),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Dokunarak değiştir",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// ADIM 4 - ÖZET
@Composable
private fun SummaryStep(
    medicines: List<MedicineEntry>,
    hour: Int,
    minute: Int,
    frequency: String,
    xValue: Int,
    selectedDates: List<String>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "📋 Özet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Divider(color = Gray200)

                // İlaçlar
                Text(
                    text = "💊 İlaçlar (${medicines.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DoziTurquoise
                )

                medicines.forEachIndexed { index, medicine ->
                    val dosageText = when (medicine.dosageType) {
                        "0.5" -> "Yarım adet"
                        "0.25" -> "Çeyrek adet"
                        "custom" -> "${medicine.customDosage} adet"
                        else -> "${medicine.dosageType} adet"
                    }

                    Surface(
                        color = DoziTurquoise.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${index + 1}. ${medicine.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                dosageText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DoziTurquoise,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Divider(color = Gray200)

                SummaryRow(
                    Icons.Default.CalendarMonth,
                    "Sıklık",
                    when {
                        frequency == "Her X günde bir" -> "Her $xValue günde bir"
                        frequency == "İstediğim tarihlerde" -> "${selectedDates.size} tarih seçildi"
                        else -> frequency
                    },
                    DoziTurquoise
                )
                SummaryRow(
                    Icons.Default.Schedule,
                    "Saat",
                    "%02d:%02d".format(hour, minute),
                    WarningOrange
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = DoziBlue.copy(alpha = 0.1f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = DoziBlue)
                Text(
                    text = "Her şey hazır! ${medicines.size} ilaç aynı saatte kaydedilecek.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// NAVİGASYON BUTONLARI
@Composable
private fun NavigationButtons(
    step: Int,
    medicineName: String,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (step > 1) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(2.dp, DoziCoral),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DoziCoral)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Geri", fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onNext as () -> Unit,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = DoziCoral)
            ) {
                Icon(
                    imageVector = if (step < 4) Icons.Default.ArrowForward else Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (step < 4) "Sonraki" else "Kaydet",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// BAŞARI DİYALOĞU
@Composable
private fun ReminderSuccessDialog(
    onAddAnother: () -> Unit,
    onFinish: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(28.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = SuccessGreen.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
                Text(
                    text = "Tamamlandı!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
                Text(
                    text = "İlaçlarınız başarıyla kaydedildi!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Başka ilaç ekle
                    Button(
                        onClick = onAddAnother,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoziTurquoise),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Başka İlaç Ekle", fontWeight = FontWeight.Bold)
                    }

                    // Bitir
                    OutlinedButton(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        border = BorderStroke(2.dp, Gray200),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Kapat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// KAYDETME - Multiple Medicines to Firestore
private fun saveMedicinesToFirestore(
    medicines: List<MedicineEntry>,
    hour: Int,
    minute: Int,
    frequency: String,
    xValue: Int,
    selectedDates: List<String>,
    startDate: Long,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val medicineRepository = FirestoreMedicineRepository()

    // Zamanları hesapla
    val times = listOf("%02d:%02d".format(hour, minute))

    // Günleri hesapla - "İstediğim tarihlerde" için tarihleri kullan
    val days = if (frequency == "İstediğim tarihlerde") {
        selectedDates
    } else {
        emptyList() // Diğer sıklıklar için frequency field'ı kullanılacak
    }

    // frequencyValue'yu düzgün hesapla
    val calculatedFrequencyValue = when (frequency) {
        "Her gün" -> 1
        "Gün aşırı" -> 2
        "Haftada bir" -> 7
        "15 günde bir" -> 15
        "Ayda bir" -> 30
        "Her X günde bir" -> xValue
        else -> 1 // "İstediğim tarihlerde" için önemsiz
    }

    // Her ilaç için Medicine nesnesi oluştur ve kaydet
    CoroutineScope(Dispatchers.IO).launch {
        var allSuccess = true

        medicines.forEach { medicineEntry ->
            val dosage = if (medicineEntry.dosageType == "custom") {
                medicineEntry.customDosage
            } else {
                medicineEntry.dosageType
            }

            val medicine = Medicine(
                id = "", // Repository tarafından oluşturulacak
                userId = "", // Repository tarafından oluşturulacak
                name = medicineEntry.name,
                dosage = "$dosage adet",
                form = "tablet",
                times = times,
                days = days,
                frequency = frequency,
                frequencyValue = calculatedFrequencyValue,
                startDate = startDate,
                endDate = null,
                stockCount = 0,
                boxSize = 0,
                notes = if (frequency == "Her X günde bir") "Her $xValue günde bir" else "",
                reminderEnabled = true,
                icon = "💊"
            )

            val success = medicineRepository.addMedicine(medicine)
            if (!success) {
                allSuccess = false
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            if (allSuccess) {
                onSuccess()
            } else {
                onError()
            }
        }
    }
}
