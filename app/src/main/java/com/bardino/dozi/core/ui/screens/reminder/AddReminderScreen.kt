package com.bardino.dozi.core.ui.screens.reminder

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.bardino.dozi.DoziApplication
import com.bardino.dozi.R
import com.bardino.dozi.core.data.MedicineLookupRepository
import com.bardino.dozi.core.data.OnboardingPreferences
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.model.MedicineCriticality
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import com.bardino.dozi.core.utils.SoundHelper
import com.bardino.dozi.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.bardino.dozi.core.premium.PremiumManager
import com.bardino.dozi.core.ui.components.PremiumLimitDialog
import com.bardino.dozi.core.ui.components.LimitIndicator
import com.bardino.dozi.core.common.Constants

// EntryPoint for accessing PremiumManager in Composable
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AddReminderEntryPoint {
    fun premiumManager(): PremiumManager
}

// Medicine entry data class
data class MedicineEntry(
    val id: Int,
    var name: String = "",
    var reminderName: String = "", // Hatırlatma için özel isim
    var dosageType: String = "1",
    var customDosage: String = "",
    var unit: String = "hap",
    var isCritical: Boolean = false // Kritik ilaç - tüm bildirimler IMPORTANT seviyesinde
)

// Time entry data class - Her saat için not tutulabilir
data class TimeEntry(
    val time: String,           // "08:00"
    val note: String = ""       // "Tok karnına", "Aç karnına", vs.
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedPlace by remember { mutableStateOf<String?>(null) }

    // 💎 Premium Manager için EntryPoint erişimi
    val premiumManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AddReminderEntryPoint::class.java
        ).premiumManager()
    }

    // 📊 Limit kontrol state'leri
    var showLimitDialog by remember { mutableStateOf(false) }
    var limitDialogType by remember { mutableStateOf("medicine") } // "medicine" veya "reminder"
    var currentMedicineCount by remember { mutableStateOf(0) }
    var currentReminderCount by remember { mutableStateOf(0) }
    var medicineLimit by remember { mutableStateOf(Constants.FREE_MEDICINE_LIMIT) }
    var reminderLimit by remember { mutableStateOf(Constants.FREE_REMINDER_LIMIT) }

    // 🔔 Bildirim izni kontrolü ve isteme
    var showPermissionDialog by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("AddReminder", "✅ Bildirim izni verildi")
            Toast.makeText(context, "Bildirim izni verildi ✅", Toast.LENGTH_SHORT).show()
        } else {
            android.util.Log.w("AddReminder", "⚠️ Bildirim izni reddedildi")
            Toast.makeText(
                context,
                "⚠️ Bildirim izni olmadan hatırlatmalar çalışmayacak",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Edit mode kontrolü veya medicineId ile direkt ilaç seçimi
    val isEditMode = medicineId != null
    val isPreselectedMedicine = medicineId != null

    // Onboarding'den hatırlatma eklendikten sonra geri dönme kontrolü
    // Ekran her görünür olduğunda (back navigation dahil) state'i kontrol et
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (OnboardingPreferences.isInOnboarding(context) &&
                OnboardingPreferences.getOnboardingStep(context) == "reminder_completed") {
                // Hatırlatma eklendi, onboarding'e geri dön
                onNavigateBack()
            }
        }
    }

    // 📊 Limit ve sayaçları yükle
    LaunchedEffect(Unit) {
        // Onboarding'deyse limit kontrolü yapma
        if (OnboardingPreferences.isInOnboarding(context)) return@LaunchedEffect

        try {
            val medicineRepository = MedicineRepository()

            // Mevcut sayıları al
            val allMedicines = medicineRepository.getAllMedicines()
            currentMedicineCount = allMedicines.size
            currentReminderCount = allMedicines.sumOf { it.times.size }

            // Limitleri al
            medicineLimit = premiumManager.getMedicineLimit()
            reminderLimit = premiumManager.getReminderLimit()

            android.util.Log.d("AddReminder", "📊 Limits loaded - Medicines: $currentMedicineCount/$medicineLimit, Reminders: $currentReminderCount/$reminderLimit")
        } catch (e: Exception) {
            android.util.Log.e("AddReminder", "Error loading limits", e)
        }
    }

    var isLoading by remember { mutableStateOf(false) }

    // State'ler
    var step by remember { mutableStateOf(1) }
    var medicines by remember { mutableStateOf(listOf(MedicineEntry(id = 0))) }
    var selectedTimes by remember { mutableStateOf<List<TimeEntry>>(listOf(TimeEntry("08:00"))) }
    var frequency by remember { mutableStateOf("Her gün") }
    var xValue by remember { mutableStateOf(2) }
    var selectedDates by remember { mutableStateOf<List<String>>(emptyList()) }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) } // Başlangıç tarihi
    var endDate by remember { mutableStateOf<Long?>(null) } // Bitiş tarihi (opsiyonel)
    var showError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    val storedMedicines = remember {
        MedicineLookupRepository
            .loadLocalMedicines(context).map { it.name }
    }
    var selectedMedicineIndex by remember { mutableStateOf(-1) }

    // MedicineId ile geldiyse, o ilacın adını yükle ve otomatik seç
    LaunchedEffect(medicineId) {
        if (isPreselectedMedicine && medicineId != null) {
            isLoading = true
            try {
                // Local repository'den ilaç adını al
                val localMedicine = MedicineLookupRepository.getLocalMedicine(context, medicineId)
                if (localMedicine != null) {
                    medicines = listOf(MedicineEntry(
                        id = 0,
                        name = localMedicine.name,
                        dosageType = "1",
                        customDosage = "",
                        unit = "hap"
                    ))
                    android.util.Log.d("AddReminder", "✅ Pre-selected medicine: ${localMedicine.name}")
                }
            } catch (e: Exception) {
                android.util.Log.e("AddReminder", "Error loading pre-selected medicine", e)
            } finally {
                isLoading = false
            }
        }
    }

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
                val repository = MedicineRepository()
                val medicine = repository.getMedicineById(medicineId)

                if (medicine != null) {
                    // Medicine verisini state'lere doldur
                    medicines = listOf(MedicineEntry(
                        id = 0,
                        name = medicine.name,
                        dosageType = medicine.dosage.split(" ").firstOrNull() ?: "1",
                        customDosage = medicine.dosage
                    ))

                    // Saatleri yükle
                    selectedTimes = if (medicine.times.isNotEmpty()) {
                        medicine.times.map { TimeEntry(it) }
                    } else {
                        listOf(TimeEntry("08:00"))
                    }

                    frequency = medicine.frequency
                    xValue = medicine.frequencyValue
                    selectedDates = medicine.days
                    // Eğer startDate 0 veya 1970 öncesi ise bugünü kullan
                    startDate = if (medicine.startDate > 0L && medicine.startDate > 946684800000L) {
                        medicine.startDate
                    } else {
                        System.currentTimeMillis()
                    }
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
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Ekrandan çıkışta çalan sesi durdur
            SoundHelper.stopCurrentSound()
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
                        }
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (soundEnabled) "Sesi Kapat" else "Sesi Aç",
                            tint = if (soundEnabled) DoziTurquoise else MaterialTheme.colorScheme.onSurfaceVariant
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
                            endDate = endDate,
                            onSelect = { frequency = it },
                            onXChange = { xValue = it },
                            onDatesChange = { selectedDates = it },
                            onStartDateChange = { startDate = it },
                            onEndDateChange = { endDate = it },
                            context = context
                        )

                        3 -> TimeStep(
                            selectedTimes = selectedTimes,
                            onTimesChange = { selectedTimes = it },
                            context = context
                        )

                        4 -> SummaryStep(
                            medicines = medicines,
                            selectedTimes = selectedTimes,
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
                                // 🔔 Bildirim izni kontrolü (Android 13+)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (!com.bardino.dozi.notifications.PermissionHandler.hasNotificationPermission(context)) {
                                        // İzin yok, dialog göster
                                        showPermissionDialog = true
                                        return@NavigationButtons
                                    }
                                }

                                // 📊 Limit kontrolü (Onboarding ve Edit mode hariç)
                                if (!OnboardingPreferences.isInOnboarding(context) && !isEditMode) {
                                    val newTimeSlotsCount = selectedTimes.size

                                    // İlaç limiti kontrolü
                                    if (medicineLimit != Constants.UNLIMITED && currentMedicineCount >= medicineLimit) {
                                        limitDialogType = "medicine"
                                        showLimitDialog = true
                                        return@NavigationButtons
                                    }

                                    // Hatırlatma (time slot) limiti kontrolü
                                    if (reminderLimit != Constants.UNLIMITED &&
                                        (currentReminderCount + newTimeSlotsCount) > reminderLimit) {
                                        limitDialogType = "reminder"
                                        showLimitDialog = true
                                        return@NavigationButtons
                                    }
                                }

                                // Tüm ilaçları kaydet
                                saveMedicinesToFirestore(
                                    context = context,
                                    medicines = medicines,
                                    selectedTimes = selectedTimes,
                                    frequency = frequency,
                                    xValue = xValue,
                                    selectedDates = selectedDates,
                                    startDate = startDate,
                                    endDate = endDate,
                                    medicineId = medicineId,
                                    onSuccess = {
                                        if (soundEnabled) playSuccessSound(context)
                                        showSuccess = true
                                        // Onboarding state kontrolü
                                        if (OnboardingPreferences.isInOnboarding(context) &&
                                            OnboardingPreferences.getOnboardingStep(context) == "reminder") {
                                            OnboardingPreferences.setOnboardingStep(context, "reminder_completed")
                                        }
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
                endDate = null
                selectedTimes = listOf(TimeEntry("08:00"))
            },
            onFinish = {
                showSuccess = false
                onNavigateBack()
            }
        )
    }

    // 📊 Premium limit dialog'u
    if (showLimitDialog) {
        val issMedicineLimit = limitDialogType == "medicine"
        PremiumLimitDialog(
            title = if (issMedicineLimit) "İlaç Limitine Ulaştınız" else "Hatırlatma Limitine Ulaştınız",
            message = if (issMedicineLimit)
                "Ücretsiz planda sadece 1 ilaç ekleyebilirsiniz. Sınırsız ilaç için Dozi Ekstra'ya yükseltin."
            else
                "Ücretsiz planda sadece 2 hatırlatma saati ekleyebilirsiniz. Sınırsız hatırlatma için Dozi Ekstra'ya yükseltin.",
            currentCount = if (issMedicineLimit) currentMedicineCount else currentReminderCount,
            maxCount = if (issMedicineLimit) medicineLimit else reminderLimit,
            requiredPlan = "Dozi Ekstra",
            onDismiss = {
                showLimitDialog = false
            },
            onUpgrade = {
                showLimitDialog = false
                // Premium ekranına yönlendir
                navController.navigate(Screen.Premium.route)
            }
        )
    }

    // 🔔 Bildirim izni dialog'u
    if (showPermissionDialog) {
        NotificationPermissionDialog(
            onRequestPermission = {
                showPermissionDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onOpenSettings = {
                showPermissionDialog = false
                com.bardino.dozi.notifications.PermissionHandler.openAppSettings(context)
            },
            onDismiss = {
                showPermissionDialog = false
                Toast.makeText(
                    context,
                    "⚠️ Bildirim izni olmadan hatırlatmalar çalışmayacak",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}

private fun playStepSound(context: Context, step: Int, soundEnabled: Boolean) {
    if (!soundEnabled) return

    try {
        val soundType = when (step) {
            1 -> SoundHelper.SoundType.REMINDER_1
            2 -> SoundHelper.SoundType.REMINDER_2
            3 -> SoundHelper.SoundType.REMINDER_3
            4 -> SoundHelper.SoundType.REMINDER_4
            else -> return
        }

        SoundHelper.playSound(context, soundType)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun playSuccessSound(context: Context) {
    try {
        SoundHelper.playSound(context, SoundHelper.SoundType.HERSEY_TAMAM)
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

            color = MaterialTheme.colorScheme.onSurfaceVariant,

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
        1 -> R.drawable.dozi_teach1
        2 -> R.drawable.dozi_teach2
        3 -> R.drawable.dozi_teach3
        4 -> R.drawable.dozi_teach4
        else -> R.drawable.dozi_perfect
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

                    // Hatırlatma adı (özel isim)
                    Text(
                        "Hatırlatma Adı (İsteğe Bağlı)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = medicine.reminderName,
                        onValueChange = { newName ->
                            onMedicinesChange(medicines.toMutableList().also {
                                it[index] = it[index].copy(reminderName = newName)
                            })
                        },
                        label = { Text("Hatırlatmaya özel isim", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        placeholder = { Text("Örn: Sabah ilacım, Tansiyon ilacı") },
                        leadingIcon = { Icon(Icons.Default.Label, null, tint = DoziCoral) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoziCoral,
                            focusedLabelColor = DoziCoral,
                            cursorColor = DoziCoral,
                            unfocusedBorderColor = Gray200,
                            focusedContainerColor = DoziCoral.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // 🚨 Kritik İlaç Seçimi
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (medicine.isCritical)
                                Color(0xFFFFEBEE) // Light red background
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (medicine.isCritical) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Kritik İlaç",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (medicine.isCritical) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Hayati önem taşıyan ilaç",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = medicine.isCritical,
                                    onCheckedChange = { isChecked ->
                                        onMedicinesChange(medicines.toMutableList().also {
                                            it[index] = it[index].copy(isCritical = isChecked)
                                        })
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFD32F2F),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }

                            // Bilgi mesajı
                            if (medicine.isCritical) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color(0xFFD32F2F).copy(alpha = 0.1f),
                                            MaterialTheme.shapes.small
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Bu ilaç için tüm bildirimler en yüksek öncelikte gönderilecek. " +
                                        "Telefon sessiz modda olsa bile alarm sesi çalacak ve " +
                                        "Rahatsız Etmeyin modunu geçecek. " +
                                        "Kaçırıldığında buddy'lerinize hemen bildirim gidecek.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD32F2F),
                                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dozaj seçici
                    Text(
                        "Kaç Adet?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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

                            label = { Text("Adet Girin", color = MaterialTheme.colorScheme.onSurfaceVariant) },

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

                    // Birim seçici
                    Text(
                        "Birim",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,

                        color = MaterialTheme.colorScheme.onSurface,

                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UnitChip(
                            label = "Hap",
                            isSelected = medicine.unit == "hap",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(unit = "hap")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                        UnitChip(
                            label = "Doz",
                            isSelected = medicine.unit == "doz",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(unit = "doz")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                        UnitChip(
                            label = "Adet",
                            isSelected = medicine.unit == "adet",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(unit = "adet")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UnitChip(
                            label = "mg",
                            isSelected = medicine.unit == "mg",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(unit = "mg")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                        UnitChip(
                            label = "ml",
                            isSelected = medicine.unit == "ml",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(unit = "ml")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                        UnitChip(
                            label = "Damla",
                            isSelected = medicine.unit == "damla",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(unit = "damla")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UnitChip(
                            label = "Kaşık",
                            isSelected = medicine.unit == "kaşık",
                            onClick = {
                                onMedicinesChange(medicines.toMutableList().also {
                                    it[index] = it[index].copy(unit = "kaşık")
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(2f))
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
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun UnitChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DoziCoral else Color.White
        ),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) DoziCoral else Gray200
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
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
        // ✅ Box wrapper ile click event'i yakala
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSelect)
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                label = { Text("İlaç Seç") },
                placeholder = { Text("Kayıtlı ilaçlardan seç") },
                leadingIcon = {
                    Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = DoziCoral)
                },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = DoziCoral)
                },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false, // ✅ Disabled ama görsel olarak düzgün
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface, // ✅ Okunabilir
                    disabledBorderColor = Gray200,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = DoziCoral,
                    disabledTrailingIconColor = DoziCoral,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = DoziCoral.copy(alpha = 0.05f)
                ),
                shape = MaterialTheme.shapes.medium
            )
        }

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
                color = MaterialTheme.colorScheme.onSurface
            )

            Divider(color = Gray200, thickness = 1.dp)

            items.forEach { medicine ->
                ListItem(
                    headlineContent = {
                        Text(
                            medicine,

                            color = MaterialTheme.colorScheme.onSurface,

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
    endDate: Long?,
    onSelect: (String) -> Unit,
    onXChange: (Int) -> Unit,
    onDatesChange: (List<String>) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long?) -> Unit,
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
                    endDate = endDate,
                    onSelect = { onSelect(option) },
                    onXChange = onXChange,
                    onDatesChange = onDatesChange,
                    onStartDateChange = onStartDateChange,
                    onEndDateChange = onEndDateChange,
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
    endDate: Long?,
    onSelect: () -> Unit,
    onXChange: (Int) -> Unit,
    onDatesChange: (List<String>) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long?) -> Unit,
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
                        color = if (isSelected) DoziTurquoise else MaterialTheme.colorScheme.onSurface
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

                // Bitiş tarihi seçici (opsiyonel)
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "📅 Bitiş Tarihi (İsteğe Bağlı)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = DoziCoral
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showEndDatePicker(context, startDate, endDate) { newEndDate ->
                                onEndDateChange(newEndDate)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(2.dp, DoziCoral),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DoziCoral)
                    ) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, tint = DoziCoral)
                        Spacer(Modifier.width(8.dp))
                        val formatter = SimpleDateFormat("dd MMM yyyy", Locale("tr", "TR"))
                        Text(
                            if (endDate != null) formatter.format(Date(endDate)) else "Tarih Seç",
                            fontWeight = FontWeight.Bold,
                            color = DoziCoral
                        )
                    }

                    // Temizle butonu
                    if (endDate != null) {
                        IconButton(
                            onClick = { onEndDateChange(null) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = ErrorRed.copy(alpha = 0.1f),
                                contentColor = ErrorRed
                            )
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle")
                        }
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

private fun showEndDatePicker(context: Context, startDate: Long, currentEndDate: Long?, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = currentEndDate ?: startDate
    }

    val picker = DatePickerDialog(
        context,
        { _, year, month, day ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, day, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }
            onDateSelected(selectedDate.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Başlangıç tarihinden önceki tarihleri seçilemez yap
    picker.datePicker.minDate = startDate
    picker.show()
}

// ADIM 3 - SAAT (Önceki adım 2'ydi, şimdi adım 3) - ÇOK SAAT DESTEĞİ
@Composable
private fun TimeStep(
    selectedTimes: List<TimeEntry>,
    onTimesChange: (List<TimeEntry>) -> Unit,
    context: Context
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Preset saatler
        Text(
            text = "⚡ Hızlı Seçim",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DoziTurquoise
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetTimeChip(
                label = "Sabah",
                time = "08:00",
                icon = "🌅",
                modifier = Modifier.weight(1f)
            ) {
                if (!selectedTimes.any { it.time == "08:00" }) {
                    onTimesChange(selectedTimes + TimeEntry("08:00"))
                }
            }

            PresetTimeChip(
                label = "Öğle",
                time = "12:00",
                icon = "☀️",
                modifier = Modifier.weight(1f)
            ) {
                if (!selectedTimes.any { it.time == "12:00" }) {
                    onTimesChange(selectedTimes + TimeEntry("12:00"))
                }
            }

            PresetTimeChip(
                label = "Akşam",
                time = "20:00",
                icon = "🌙",
                modifier = Modifier.weight(1f)
            ) {
                if (!selectedTimes.any { it.time == "20:00" }) {
                    onTimesChange(selectedTimes + TimeEntry("20:00"))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Seçili saatler listesi
        Text(
            text = "⏰ Seçili Saatler (${selectedTimes.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DoziTurquoise
        )

        selectedTimes.forEachIndexed { index, timeEntry ->
            TimeEntryCard(
                timeEntry = timeEntry,
                onRemove = {
                    if (selectedTimes.size > 1) {
                        onTimesChange(selectedTimes.filterIndexed { i, _ -> i != index })
                    }
                },
                onEdit = { newTime ->
                    val updatedList = selectedTimes.toMutableList()
                    updatedList[index] = timeEntry.copy(time = newTime)
                    onTimesChange(updatedList)
                },
                onNoteChange = { newNote ->
                    val updatedList = selectedTimes.toMutableList()
                    updatedList[index] = timeEntry.copy(note = newNote)
                    onTimesChange(updatedList)
                },
                context = context,
                canRemove = selectedTimes.size > 1
            )
        }

        // + Saat Ekle butonu
        OutlinedButton(
            onClick = {
                // Yeni saat ekle dialog
                TimePickerDialog(context, { _, h, m ->
                    val newTime = "%02d:%02d".format(h, m)
                    if (!selectedTimes.any { it.time == newTime }) {
                        onTimesChange(selectedTimes + TimeEntry(newTime))
                    }
                }, 8, 0, true).show()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(2.dp, DoziTurquoise),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DoziTurquoise),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Saat Ekle", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PresetTimeChip(
    label: String,
    time: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        border = BorderStroke(2.dp, DoziTurquoise),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DoziTurquoise),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimeEntryCard(
    timeEntry: TimeEntry,
    onRemove: () -> Unit,
    onEdit: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    context: Context,
    canRemove: Boolean
) {
    var showNoteInput by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(timeEntry.note) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(2.dp, DoziTurquoise.copy(alpha = 0.3f))
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
                // Saat gösterimi
                Surface(
                    color = DoziTurquoise.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.clickable {
                        val (hour, minute) = timeEntry.time.split(":").map { it.toInt() }
                        TimePickerDialog(context, { _, h, m ->
                            onEdit("%02d:%02d".format(h, m))
                        }, hour, minute, true).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = DoziTurquoise,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = timeEntry.time,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = DoziTurquoise
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Not ekle/düzenle butonu
                    IconButton(
                        onClick = { showNoteInput = !showNoteInput }
                    ) {
                        Icon(
                            if (timeEntry.note.isNotEmpty()) Icons.Default.Edit else Icons.Default.Note,
                            contentDescription = "Not",
                            tint = if (timeEntry.note.isNotEmpty()) DoziCoral else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Kaldır butonu
                    if (canRemove) {
                        IconButton(onClick = onRemove) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kaldır",
                                tint = ErrorRed
                            )
                        }
                    }
                }
            }

            // Not gösterimi
            if (timeEntry.note.isNotEmpty() && !showNoteInput) {
                Surface(
                    color = DoziCoral.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = DoziCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = timeEntry.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Not input
            AnimatedVisibility(
                visible = showNoteInput,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Hızlı seçenekler
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Tok karnına", "Aç karnına", "Yemekle birlikte", "Yemekten önce", "Yemekten sonra").forEach { note ->
                            AssistChip(
                                onClick = {
                                    noteText = note
                                    onNoteChange(note)
                                    showNoteInput = false
                                },
                                label = { Text(note, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = DoziCoral.copy(alpha = 0.1f),
                                    labelColor = DoziCoral
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Özel Not") },
                        placeholder = { Text("Örn: Tok karnına, Aç karnına, vb.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoziCoral,
                            focusedLabelColor = DoziCoral,
                            cursorColor = DoziCoral
                        ),
                        trailingIcon = {
                            Row {
                                if (noteText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        noteText = ""
                                        onNoteChange("")
                                    }) {
                                        Icon(Icons.Default.Clear, "Temizle", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = {
                                    onNoteChange(noteText)
                                    showNoteInput = false
                                }) {
                                    Icon(Icons.Default.Check, "Kaydet", tint = DoziCoral)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
// ADIM 4 - ÖZET
@Composable
private fun SummaryStep(
    medicines: List<MedicineEntry>,
    selectedTimes: List<TimeEntry>,
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
                    color = MaterialTheme.colorScheme.onSurface
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
                    val dosageAmount = when (medicine.dosageType) {
                        "0.5" -> "Yarım"
                        "0.25" -> "Çeyrek"
                        "1.5" -> "1.5"
                        "custom" -> medicine.customDosage
                        else -> medicine.dosageType
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = DoziTurquoise.copy(alpha = 0.05f)
                        ),
                        border = BorderStroke(1.dp, DoziTurquoise.copy(alpha = 0.3f)),
                        shape = MaterialTheme.shapes.small,
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // İlaç ismi - uzun isimleri ellipsize ile kes
                            Text(
                                text = "${index + 1}. ${medicine.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Dozaj bilgisi - yatay düzen
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocalHospital,
                                    contentDescription = null,
                                    tint = DoziTurquoise,
                                    modifier = Modifier.size(16.dp)
                                )
                                Surface(
                                    color = DoziTurquoise.copy(alpha = 0.15f),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = "$dosageAmount ${medicine.unit}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DoziTurquoise,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
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
                // Saatler
                Text(
                    text = "⏰ Saatler (${selectedTimes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WarningOrange,
                    modifier = Modifier.padding(top = 8.dp)
                )

                selectedTimes.forEach { timeEntry ->
                    Surface(
                        color = WarningOrange.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = WarningOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = timeEntry.time,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = WarningOrange
                                    )
                                }
                            }

                            if (timeEntry.note.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = timeEntry.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                color = MaterialTheme.colorScheme.onSurfaceVariant,

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
                onClick = onNext,
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

                    color = MaterialTheme.colorScheme.onSurface,

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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text("Kapat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// BİLDİRİM İZNİ DİYALOĞU
@Composable
private fun NotificationPermissionDialog(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                // İkon
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = WarningOrange.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                // Başlık
                Text(
                    text = "Bildirim İzni Gerekli",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = WarningOrange
                )

                // Açıklama
                Text(
                    text = "İlaç hatırlatmalarının çalışabilmesi için bildirim iznine ihtiyaç var.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                // Butonlar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // İzin ver butonu
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoziTurquoise),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("İzin Ver", fontWeight = FontWeight.Bold)
                    }

                    // Ayarlara git butonu (izin zaten reddedildiyse)
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        border = BorderStroke(2.dp, DoziTurquoise),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DoziTurquoise)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ayarlara Git", fontWeight = FontWeight.Bold)
                    }

                    // İptal butonu
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Şimdi Değil",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// KAYDETME - Multiple Medicines to Firestore
private fun saveMedicinesToFirestore(
    context: Context,
    medicines: List<MedicineEntry>,
    selectedTimes: List<TimeEntry>,
    frequency: String,
    xValue: Int,
    selectedDates: List<String>,
    startDate: Long,
    endDate: Long?,
    medicineId: String? = null,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    // Onboarding'deyse veya kullanıcı giriş yapmamışsa LOCAL'e kaydet (Firebase yerine)
    val isUserAuthenticated = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
    if (OnboardingPreferences.isInOnboarding(context) || !isUserAuthenticated) {
        android.util.Log.d("AddReminder", "✅ Saving to local storage (onboarding: ${OnboardingPreferences.isInOnboarding(context)}, authenticated: $isUserAuthenticated)")
        saveRemindersToLocal(context, medicines, selectedTimes, frequency, xValue, selectedDates, startDate)
        onSuccess()
        return
    }

    val medicineRepository = MedicineRepository()

    // Zamanları listele
    val times = selectedTimes.map { it.time }

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

    // Her ilaç için Medicine nesnesi oluştur veya mevcut olanı güncelle
    CoroutineScope(Dispatchers.IO).launch {
        var allSuccess = true

        medicines.forEach { medicineEntry ->
            val dosage = if (medicineEntry.dosageType == "custom") {
                medicineEntry.customDosage
            } else {
                medicineEntry.dosageType
            }

            // 🔍 Edit mode: medicineId ile direkt ilacı al, yoksa isimle ara
            val existingMedicine = if (medicineId != null) {
                medicineRepository.getMedicineById(medicineId)
            } else {
                val existingMedicines = medicineRepository.getAllMedicines()
                existingMedicines.find {
                    it.name.equals(medicineEntry.name, ignoreCase = true)
                }
            }

            val savedMedicine = if (existingMedicine != null) {
                // ✅ Mevcut ilacı güncelle
                // Edit mode: saatleri REPLACE et, aksi halde MERGE et
                val updatedTimes = if (medicineId != null) {
                    // Edit mode: Eski saatleri sil, yeni saatleri kullan
                    times
                } else {
                    // Yeni hatırlatma ekleme: Saatleri birleştir
                    (existingMedicine.times + times).distinct().sorted()
                }

                try {
                    // Times'ı güncelle
                    medicineRepository.updateMedicineField(existingMedicine.id, "times", updatedTimes)

                    // ReminderName güncelleme (eğer yeni bir isim verildiyse)
                    if (medicineEntry.reminderName.isNotEmpty()) {
                        medicineRepository.updateMedicineField(existingMedicine.id, "reminderName", medicineEntry.reminderName)
                    }

                    // 🔥 FIX: Frequency ve frequencyValue'yu da güncelle
                    medicineRepository.updateMedicineField(existingMedicine.id, "frequency", frequency)
                    medicineRepository.updateMedicineField(existingMedicine.id, "frequencyValue", calculatedFrequencyValue)

                    // 🔥 FIX: Days listesini de güncelle (İstediğim tarihlerde için)
                    medicineRepository.updateMedicineField(existingMedicine.id, "days", days)

                    // 🔥 FIX: startDate'i güncelle
                    medicineRepository.updateMedicineField(existingMedicine.id, "startDate", startDate)

                    // 🔥 FIX: endDate'i güncelle
                    if (endDate != null) {
                        medicineRepository.updateMedicineField(existingMedicine.id, "endDate", endDate as Long)
                    } else {
                        medicineRepository.updateMedicineField(existingMedicine.id, "endDate", com.google.firebase.firestore.FieldValue.delete())
                    }


                    // 🔥 FIX: reminderEnabled'ı true yap (hatırlatma eklendi)
                    medicineRepository.updateMedicineField(existingMedicine.id, "reminderEnabled", true)

                    // 🔥 Kritiklik seviyesini güncelle
                    val criticalityLevel = if (medicineEntry.isCritical) "CRITICAL" else "ROUTINE"
                    medicineRepository.updateMedicineField(existingMedicine.id, "criticalityLevel", criticalityLevel)

                    // Notes'u güncelle - Edit mode'da REPLACE, yoksa MERGE
                    val newNotes = buildNotesFromTimes(selectedTimes, frequency, xValue)
                    val updatedNotes = if (medicineId != null) {
                        // Edit mode: Notları değiştir
                        newNotes
                    } else {
                        // Yeni hatırlatma: Notları birleştir
                        if (existingMedicine.notes.isNotEmpty() && newNotes.isNotEmpty()) {
                            "${existingMedicine.notes} | $newNotes"
                        } else if (newNotes.isNotEmpty()) {
                            newNotes
                        } else {
                            existingMedicine.notes
                        }
                    }
                    medicineRepository.updateMedicineField(existingMedicine.id, "notes", updatedNotes)

                    android.util.Log.d("AddReminderScreen", "✅ Mevcut ilaç güncellendi: ${existingMedicine.name} - Sıklık: ${existingMedicine.frequency} -> $frequency (${calculatedFrequencyValue}), Saatler: ${existingMedicine.times} -> $updatedTimes")

                    // Güncellenmiş Medicine'i al
                    medicineRepository.getMedicineById(existingMedicine.id)
                } catch (e: Exception) {
                    android.util.Log.e("AddReminderScreen", "❌ İlaç güncellenemedi: ${existingMedicine.name}", e)
                    null
                }
            } else {
                // 🆕 Yeni ilaç oluştur
                val medicine = Medicine(
                    id = "", // Repository tarafından oluşturulacak
                    userId = "", // Repository tarafından oluşturulacak
                    name = medicineEntry.name,
                    dosage = dosage,
                    unit = medicineEntry.unit,
                    form = "tablet",
                    times = times,
                    days = days,
                    frequency = frequency,
                    frequencyValue = calculatedFrequencyValue,
                    startDate = startDate,
                    endDate = endDate,
                    stockCount = 0,
                    boxSize = 0,
                    notes = buildNotesFromTimes(selectedTimes, frequency, xValue),
                    reminderEnabled = true,
                    reminderName = medicineEntry.reminderName.ifEmpty { medicineEntry.name },
                    icon = "💊",
                    // 🔥 Kritiklik seviyesi
                    criticalityLevel = if (medicineEntry.isCritical) MedicineCriticality.CRITICAL else MedicineCriticality.ROUTINE
                )

                medicineRepository.addMedicine(medicine)
            }

            if (savedMedicine == null) {
                allSuccess = false
                android.util.Log.e("AddReminderScreen", "❌ ${medicineEntry.name} kaydedilemedi")
            } else {
                // ✅ İlaç başarıyla kaydedildi/güncellendi, şimdi alarmları planla
                android.util.Log.d("AddReminderScreen", "✅ ${savedMedicine.name} kaydedildi (ID: ${savedMedicine.id})")
                com.bardino.dozi.notifications.ReminderScheduler.scheduleReminders(context, savedMedicine)
                android.util.Log.d("AddReminderScreen", "✅ ${savedMedicine.name} için alarmlar planlandı")
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            if (allSuccess) {
                // ✅ Widget'ı güncelle
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        com.bardino.dozi.widget.ReminderWidgetUpdater.updateWidgets(context)
                        android.util.Log.d("AddReminderScreen", "✅ Widget güncellendi")
                    } catch (e: Exception) {
                        android.util.Log.e("AddReminderScreen", "Widget güncelleme hatası", e)
                    }
                }
                onSuccess()
            } else {
                onError()
            }
        }
    }
}

// Helper: Saatlerden ve notlardan birleşik not oluştur
private fun buildNotesFromTimes(selectedTimes: List<TimeEntry>, frequency: String, xValue: Int): String {
    val timeNotes = selectedTimes.filter { it.note.isNotEmpty() }
        .joinToString(" | ") { "${it.time}: ${it.note}" }

    val frequencyNote = if (frequency == "Her X günde bir") "Her $xValue günde bir" else ""

    return if (timeNotes.isNotEmpty() && frequencyNote.isNotEmpty()) {
        "$frequencyNote | $timeNotes"
    } else if (timeNotes.isNotEmpty()) {
        timeNotes
    } else {
        frequencyNote
    }
}

// LOCAL KAYDETME - Onboarding sırasında kullanılır
private fun saveRemindersToLocal(
    context: Context,
    medicines: List<MedicineEntry>,
    selectedTimes: List<TimeEntry>,
    frequency: String,
    xValue: Int,
    selectedDates: List<String>,
    startDate: Long
) {
    val prefs = context.getSharedPreferences("local_reminders", Context.MODE_PRIVATE)
    val existingReminders = prefs.getString("reminders", "[]") ?: "[]"

    // JSON array olarak parse et
    val remindersArray = try {
        org.json.JSONArray(existingReminders)
    } catch (e: Exception) {
        org.json.JSONArray()
    }

    // Yeni hatırlatmaları ekle
    medicines.forEach { medicineEntry ->
        val dosage = if (medicineEntry.dosageType == "custom") {
            medicineEntry.customDosage
        } else {
            medicineEntry.dosageType
        }

        val reminderJson = org.json.JSONObject().apply {
            put("name", medicineEntry.name)
            put("dosage", dosage)
            put("unit", medicineEntry.unit)
            put("times", org.json.JSONArray(selectedTimes.map { it.time }))
            put("timeNotes", org.json.JSONObject().apply {
                selectedTimes.forEach { timeEntry ->
                    if (timeEntry.note.isNotEmpty()) {
                        put(timeEntry.time, timeEntry.note)
                    }
                }
            })
            put("frequency", frequency)
            put("xValue", xValue)
            put("selectedDates", org.json.JSONArray(selectedDates))
            put("startDate", startDate)
            put("createdAt", System.currentTimeMillis())
        }

        remindersArray.put(reminderJson)
        android.util.Log.d("AddReminder", "💾 Saved to local: ${medicineEntry.name} - ${selectedTimes.size} saat")
    }

    // Kaydet
    prefs.edit().putString("reminders", remindersArray.toString()).apply()
    android.util.Log.d("AddReminder", "✅ Total local reminders: ${remindersArray.length()}")
}
