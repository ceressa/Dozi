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

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    onNavigateBack: () -> Unit,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var selectedPlace by remember { mutableStateOf<String?>(null) }


    // State'ler
    var step by remember { mutableStateOf(1) }
    var medicineName by remember { mutableStateOf("") }
    var dosageType by remember { mutableStateOf("1") } // "1", "0.5", "0.25", "2", "3", "custom"
    var customDosage by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    var frequency by remember { mutableStateOf("Her gün") }
    var xValue by remember { mutableStateOf(2) }
    var selectedDates by remember { mutableStateOf<List<String>>(emptyList()) }
    var showError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var reminderTitle by remember { mutableStateOf("") }
    val storedMedicines = remember {
        MedicineRepository
            .loadMedicines(context).map { it.name }
    }
    var showPicker by remember { mutableStateOf(false) }

    // Ses kontrolü
    var soundEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
                .getBoolean("sound_enabled", true)
        )
    }

    // MedicineListScreen'den dönen ilaç ismini al
    LaunchedEffect(navController.currentBackStackEntry) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.get<String>("selectedMedicine")
            ?.let { selectedName ->
                medicineName = selectedName
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
                title = "Hatırlatma Asistanı",
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
                        1 -> MedicineStep(
                            medicineName = medicineName,
                            onNameChange = { medicineName = it },
                            dosageType = dosageType,
                            onDosageTypeChange = { dosageType = it },
                            customDosage = customDosage,
                            onCustomDosageChange = { customDosage = it },
                            reminderTitle = reminderTitle,
                            onReminderTitleChange = { reminderTitle = it },
                            storedMedicines = storedMedicines,
                            showPicker = showPicker,
                            onShowPickerChange = { showPicker = it },
                            onAddNewMedicine = {
                                navController.navigate(Screen.MedicineLookup.route)
                            },
                            showError = showError
                        )

                        2 -> FrequencyStep(
                            selected = frequency,
                            xValue = xValue,
                            selectedDates = selectedDates,
                            onSelect = { frequency = it },
                            onXChange = { xValue = it },
                            onDatesChange = { selectedDates = it },
                            context = context
                        )

                        3 -> TimeStep(
                            hour = hour,
                            minute = minute,
                            onTimeChange = { h, m -> hour = h; minute = m },
                            context = context
                        )

                        4 -> SummaryStep(
                            medicineName = medicineName,
                            reminderTitle = reminderTitle,
                            dosageType = dosageType,
                            customDosage = customDosage,
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
                    medicineName = medicineName,
                    onBack = { step-- },
                    onNext = {
                        when (step) {
                            1 -> {
                                if (medicineName.isBlank()) showError = true
                                else {
                                    showError = false
                                    step++
                                }
                            }
                            2 -> step++
                            3 -> step++
                            4 -> {
                                val finalTitle = reminderTitle.ifBlank { medicineName }
                                val finalDosage = if (dosageType == "custom") customDosage else dosageType

                                // 🗂️ Hatırlatmayı Firestore'a kaydet
                                saveReminderToFirestore(
                                    medicineName = medicineName,
                                    dosage = finalDosage,
                                    hour = hour,
                                    minute = minute,
                                    frequency = frequency,
                                    xValue = xValue,
                                    selectedDates = selectedDates,
                                    onSuccess = {
                                        if (soundEnabled) playSuccessSound(context)
                                        showSuccess = true
                                    },
                                    onError = {
                                        Toast.makeText(
                                            context,
                                            "❌ Hatırlatma kaydedilemedi",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )

                                // 📍 Eğer kullanıcı bir konum seçtiyse geofence ekle
                                selectedPlace?.let { placeName ->
                                    val prefs = context.getSharedPreferences("places", Context.MODE_PRIVATE)
                                    val placeData = prefs.getString(placeName, null)
                                    placeData?.let {
                                        val parts = it.split(",")
                                        if (parts.size >= 2) {
                                            val lat = parts[0].toDouble()
                                            val lng = parts[1].toDouble()
                                            addGeofence(context, placeName, lat, lng)
                                            Toast.makeText(
                                                context,
                                                "📍 $placeName konumu için hatırlatma eklendi",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
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
                showSuccess = false
                step = 1
                medicineName = ""
                dosageType = "1"
                customDosage = ""
                frequency = "Her gün"
                selectedDates = emptyList()
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
        1 -> "💊 Hangi ilacı hatırlatmamı istersin?"
        2 -> "🔁 Ne kadar sıklıkla alıyorsun?"
        3 -> "⏰ Harika! Şimdi ilacın saatini belirleyelim."
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

// ADIM 1 - İLAÇ VE DOZAJ
@Composable
private fun MedicineStep(
    medicineName: String,
    onNameChange: (String) -> Unit,
    dosageType: String,
    onDosageTypeChange: (String) -> Unit,
    customDosage: String,
    onCustomDosageChange: (String) -> Unit,
    reminderTitle: String,
    onReminderTitleChange: (String) -> Unit,
    storedMedicines: List<String>,
    showPicker: Boolean,
    onShowPickerChange: (Boolean) -> Unit,
    onAddNewMedicine: () -> Unit,
    showError: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hatırlatma ismi
        OutlinedTextField(
            value = reminderTitle,
            onValueChange = onReminderTitleChange,
            label = { Text("Hatırlatma İsmi (opsiyonel)", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Edit, null, tint = DoziBlue) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DoziBlue,
                focusedLabelColor = DoziBlue,
                cursorColor = DoziBlue,
                unfocusedBorderColor = Gray200,
                focusedContainerColor = DoziBlue.copy(alpha = 0.05f), // ✅ YENİ
                unfocusedContainerColor = Color.White // ✅ YENİ
            )
        )

        // İlaç seçici
        MedicinePickerRow(
            selectedName = medicineName,
            onSelect = {
                if (storedMedicines.isNotEmpty()) {
                    onShowPickerChange(true)
                } else {
                    onAddNewMedicine()
                }
            },
            onAddNew = onAddNewMedicine
        )

        // Dozaj seçici - YENİ TASARIM
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Kaç Adet Alıyorsun?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Hızlı seçenekler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DosageChip(
                    label = "1",
                    isSelected = dosageType == "1",
                    onClick = { onDosageTypeChange("1") },
                    modifier = Modifier.weight(1f)
                )
                DosageChip(
                    label = "Yarım",
                    isSelected = dosageType == "0.5",
                    onClick = { onDosageTypeChange("0.5") },
                    modifier = Modifier.weight(1f)
                )
                DosageChip(
                    label = "Çeyrek",
                    isSelected = dosageType == "0.25",
                    onClick = { onDosageTypeChange("0.25") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DosageChip(
                    label = "2",
                    isSelected = dosageType == "2",
                    onClick = { onDosageTypeChange("2") },
                    modifier = Modifier.weight(1f)
                )
                DosageChip(
                    label = "3",
                    isSelected = dosageType == "3",
                    onClick = { onDosageTypeChange("3") },
                    modifier = Modifier.weight(1f)
                )
                DosageChip(
                    label = "Diğer",
                    isSelected = dosageType == "custom",
                    onClick = { onDosageTypeChange("custom") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Custom dozaj girişi
            AnimatedVisibility(
                visible = dosageType == "custom",
                enter = fadeIn() + expandVertically()
            ) {
                OutlinedTextField(
                    value = customDosage,
                    onValueChange = onCustomDosageChange,
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
                        focusedContainerColor = DoziTurquoise.copy(alpha = 0.05f), // ✅ YENİ
                        unfocusedContainerColor = Color.White // ✅ YENİ
                    )
                )
            }
        }

        // Hata mesajı
        AnimatedVisibility(
            visible = showError && medicineName.isBlank(),
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
                        "Lütfen ilaç seçin",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if (showPicker) {
        MedicineBottomSheet(
            items = storedMedicines,
            onPick = {
                onNameChange(it)
                onShowPickerChange(false)
            },
            onDismiss = { onShowPickerChange(false) }
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
    onSelect: (String) -> Unit,
    onXChange: (Int) -> Unit,
    onDatesChange: (List<String>) -> Unit,
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
                    onSelect = { onSelect(option) },
                    onXChange = onXChange,
                    onDatesChange = onDatesChange,
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
    onSelect: () -> Unit,
    onXChange: (Int) -> Unit,
    onDatesChange: (List<String>) -> Unit,
    context: Context
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SuccessGreen.copy(alpha = 0.1f) else Color.White
        ),
        shape = MaterialTheme.shapes.medium,
        border = if (isSelected) BorderStroke(2.dp, SuccessGreen) else BorderStroke(1.dp, Gray200)
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
                        colors = RadioButtonDefaults.colors(selectedColor = SuccessGreen)
                    )
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) SuccessGreen else TextPrimary
                    )
                }
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
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
                        Icon(Icons.Default.Remove, contentDescription = "Azalt", tint = SuccessGreen)
                    }
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "$xValue gün",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                    IconButton(onClick = { onXChange(xValue + 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "Artır", tint = SuccessGreen)
                    }
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
                    border = BorderStroke(1.dp, SuccessGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen)
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
        color = SuccessGreen.copy(alpha = 0.15f),
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
                color = SuccessGreen,
                fontWeight = FontWeight.Medium
            )
            Icon(
                Icons.Default.Close,
                contentDescription = "Kaldır",
                tint = SuccessGreen,
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
    medicineName: String,
    reminderTitle: String,
    dosageType: String,
    customDosage: String,
    hour: Int,
    minute: Int,
    frequency: String,
    xValue: Int,
    selectedDates: List<String>
) {
    val displayDosage = if (dosageType == "custom") customDosage else dosageType
    val dosageText = when (dosageType) {
        "0.5" -> "Yarım adet"
        "0.25" -> "Çeyrek adet"
        "custom" -> "$customDosage adet"
        else -> "$dosageType adet"
    }

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

                SummaryRow(
                    Icons.Default.Badge,
                    "Hatırlatma",
                    if (reminderTitle.isBlank()) medicineName else reminderTitle,
                    DoziCoral
                )
                if (reminderTitle.isNotBlank()) {
                    SummaryRow(
                        Icons.Default.LocalPharmacy,
                        "İlaç",
                        medicineName,
                        DoziTurquoise
                    )
                }
                SummaryRow(
                    Icons.Default.Medication,
                    "Dozaj",
                    dosageText,
                    DoziBlue
                )
                SummaryRow(
                    Icons.Default.CalendarMonth,
                    "Sıklık",
                    when {
                        frequency == "Her X günde bir" -> "Her $xValue günde bir"
                        frequency == "İstediğim tarihlerde" -> "${selectedDates.size} tarih seçildi"
                        else -> frequency
                    },
                    SuccessGreen
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
                    text = "Her şey hazır! Kaydet butonuna basarak hatırlatmanı aktif edebilirsin.",
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
private fun ReminderSuccessDialog(onAddAnother: () -> Unit, onFinish: () -> Unit) {
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
                    text = "Harika!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
                Text(
                    text = "Hatırlatman başarıyla kaydedildi!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Divider(color = Gray200)
                Text(
                    text = "Başka bir ilaç eklemek ister misin?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAddAnother,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Evet, bir tane daha", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        border = BorderStroke(2.dp, Gray200),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Hayır, teşekkürler", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// KAYDETME - Firestore
private fun saveReminderToFirestore(
    medicineName: String,
    dosage: String,
    hour: Int,
    minute: Int,
    frequency: String,
    xValue: Int,
    selectedDates: List<String>,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val medicineRepository = FirestoreMedicineRepository()

    // Zamanları hesapla
    val times = listOf("%02d:%02d".format(hour, minute))

    // Günleri hesapla
    val days = when (frequency) {
        "Her gün" -> emptyList() // Boş liste = her gün
        "Gün aşırı" -> emptyList() // TODO: Gün aşırı mantığı eklenecek
        "Haftada bir" -> emptyList() // TODO: Haftalık mantık
        "İstediğim tarihlerde" -> selectedDates
        else -> emptyList()
    }

    // Medicine nesnesi oluştur
    val medicine = Medicine(
        id = "", // Repository tarafından oluşturulacak
        userId = "", // Repository tarafından oluşturulacak
        name = medicineName,
        dosage = "$dosage adet",
        form = "tablet",
        times = times,
        days = days,
        startDate = System.currentTimeMillis(),
        endDate = null, // Sürekli kullanım
        stockCount = 0,
        boxSize = 0,
        notes = "Sıklık: $frequency",
        reminderEnabled = true,
        icon = "💊"
    )

    // Firestore'a kaydet
    CoroutineScope(Dispatchers.IO).launch {
        val success = medicineRepository.addMedicine(medicine)
        if (success) {
            CoroutineScope(Dispatchers.Main).launch {
                onSuccess()
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                onError()
            }
        }
    }
}
