package com.bardino.dozi.core.ui.screens.home

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.bardino.dozi.DoziApplication
import com.bardino.dozi.R
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.data.repository.UserRepository
import com.bardino.dozi.core.ui.screens.home.MedicineStatus
import com.bardino.dozi.core.ui.theme.*
import com.bardino.dozi.core.ui.components.BadiPremiumDialog
import com.bardino.dozi.core.utils.SoundHelper
import com.bardino.dozi.core.utils.getMedicineStatus
import com.bardino.dozi.core.utils.getCurrentDateString
import com.bardino.dozi.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*

// Helper function: Belirli bir tarihte ilacın gösterilip gösterilmeyeceğini kontrol eder
@RequiresApi(Build.VERSION_CODES.O)
fun shouldMedicineShowOnDate(medicine: Medicine, date: LocalDate): Boolean {
    // startDate kontrolü
    val startLocalDate = Instant.ofEpochMilli(medicine.startDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    if (date.isBefore(startLocalDate)) {
        return false // Başlangıç tarihinden önce gösterme
    }

    // endDate kontrolü
    if (medicine.endDate != null) {
        val endLocalDate = Instant.ofEpochMilli(medicine.endDate!!)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        if (date.isAfter(endLocalDate)) {
            return false // Bitiş tarihinden sonra gösterme
        }
    }

    when (medicine.frequency) {
        "Her gün" -> return true

        "Gün aşırı" -> {
            // Başlangıç gününden itibaren gün aşırı: gün 0 (al), gün 1 (alma), gün 2 (al), ...
            val daysSinceStart = ChronoUnit.DAYS.between(startLocalDate, date)
            return daysSinceStart % 2 == 0L
        }

        "Haftada bir" -> {
            // Başlangıç tarihinin haftanın günü ile aynı günlerde al
            // ANCAK: Başlangıç gününden itibaren tam hafta geçtiyse göster
            if (date.isBefore(startLocalDate)) return false
            val daysSinceStart = ChronoUnit.DAYS.between(startLocalDate, date)
            return startLocalDate.dayOfWeek == date.dayOfWeek && daysSinceStart % 7 == 0L
        }

        "15 günde bir" -> {
            // Her 15 günde bir: gün 0, 15, 30, 45, ...
            if (date.isBefore(startLocalDate)) return false
            val daysSinceStart = ChronoUnit.DAYS.between(startLocalDate, date)
            return daysSinceStart % 15 == 0L
        }

        "Ayda bir" -> {
            // Her 30 günde bir: gün 0, 30, 60, 90, ...
            if (date.isBefore(startLocalDate)) return false
            val daysSinceStart = ChronoUnit.DAYS.between(startLocalDate, date)
            return daysSinceStart % 30 == 0L
        }

        "Her X günde bir" -> {
            // Her X günde bir: gün 0, X, 2X, 3X, ...
            val daysSinceStart = ChronoUnit.DAYS.between(startLocalDate, date)
            return daysSinceStart % medicine.frequencyValue.toLong() == 0L
        }

        "İstediğim tarihlerde" -> {
            // Kullanıcının seçtiği özel tarihlerde
            val dateString = "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)
            return medicine.days.contains(dateString)
        }

        else -> return false
    }
}

// Helper function: Get medicine status for a specific date with percentage-based logic
fun getMedicineStatusForDate(context: Context, date: LocalDate, medicines: List<Medicine>): MedicineStatus {
    val dateString = "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)
    val prefs = context.getSharedPreferences("medicine_status", Context.MODE_PRIVATE)

    var totalDoses = 0
    var takenCount = 0
    var skippedCount = 0
    var upcomingCount = 0

    medicines.forEach { medicine ->
        // ✅ Sadece bu tarihte gösterilmesi gereken ilaçları kontrol et
        if (!shouldMedicineShowOnDate(medicine, date)) {
            return@forEach
        }

        medicine.times.forEach { time ->
            totalDoses++
            val key = "dose_${medicine.id}_${dateString}_$time"
            val status = prefs.getString(key, null)

            when {
                status == "taken" -> takenCount++
                status?.startsWith("skipped") == true -> skippedCount++
                status == null && date >= LocalDate.now() -> upcomingCount++
            }
        }
    }

    // Eğer hiç doz yoksa
    if (totalDoses == 0) return MedicineStatus.NONE

    // Yüzde hesapla
    val takenPercentage = (takenCount * 100) / totalDoses

    return when {
        // %100 alındı -> Yeşil
        takenPercentage == 100 && skippedCount == 0 -> MedicineStatus.TAKEN

        // Karışık durum: hem alındı hem atlandı -> Turuncu (PARTIAL)
        takenCount > 0 && skippedCount > 0 -> MedicineStatus.PARTIAL

        // Sadece atlandı -> Kırmızı
        skippedCount > 0 && takenCount == 0 -> MedicineStatus.SKIPPED

        // Gelecek ilaçlar
        upcomingCount > 0 -> if (date == LocalDate.now()) MedicineStatus.UPCOMING else MedicineStatus.PLANNED

        // Hiçbiri
        else -> MedicineStatus.NONE
    }
}

// Helper function: Get medicine records for a specific date
fun getMedicineRecordsForDate(context: Context, date: LocalDate, medicines: List<Medicine>): List<MedicineRecord> {
    val dateString = "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)
    val prefs = context.getSharedPreferences("medicine_status", Context.MODE_PRIVATE)

    val records = mutableListOf<MedicineRecord>()

    medicines.forEach { medicine ->
        // ✅ Sadece bu tarihte gösterilmesi gereken ilaçları kontrol et
        if (!shouldMedicineShowOnDate(medicine, date)) {
            return@forEach
        }

        medicine.times.forEach { time ->
            val key = "dose_${medicine.id}_${dateString}_$time"
            val statusValue = prefs.getString(key, null)

            val status = when {
                statusValue == "taken" -> MedicineStatus.TAKEN
                statusValue?.startsWith("skipped") == true -> MedicineStatus.SKIPPED
                statusValue?.startsWith("snoozed") == true -> MedicineStatus.UPCOMING
                date == LocalDate.now() -> MedicineStatus.UPCOMING
                date > LocalDate.now() -> MedicineStatus.PLANNED
                else -> MedicineStatus.NONE
            }

            if (status != MedicineStatus.NONE) {
                records.add(MedicineRecord(
                    time = time,
                    name = "${medicine.name} ${medicine.dosage}",
                    status = status
                ))
            }
        }
    }

    return records.sortedBy { it.time }
}

data class MedicineRecord(
    val time: String,
    val name: String,
    val status: MedicineStatus
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues(),
    onNavigateToMedicines: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current

    // ✅ Hilt ile ViewModel inject et
    val viewModel: HomeViewModel = hiltViewModel()

    // ✅ ViewModel'den state'leri al
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Local UI state'leri (sadece UI için)
    var timelineExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showBadiPremiumDialog by remember { mutableStateOf(false) }

    // Premium durumunu hesapla
    val isPremium = uiState.user?.planType != "free"

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // ✅ Context gerektiren ViewModel fonksiyonlarını çağır
    LaunchedEffect(context) {
        // 🔥 Not: refreshMedicines artık Flow ile otomatik - polling kaldırıldı
        viewModel.loadSnoozeStateFromContext(context)
        viewModel.startSnoozeTimerWithContext(context)
    }

    // ✅ Uygulama ön plana geldiğinde state'i yenile (Widget senkronizasyonu için)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshMedicines(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 🎨 Tema renklerini al
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

    // 🎁 Badi Premium Dialog
    if (showBadiPremiumDialog) {
        BadiPremiumDialog(
            onDismiss = { showBadiPremiumDialog = false },
            onUpgrade = {
                showBadiPremiumDialog = false
                navController.navigate(Screen.PremiumIntro.route)
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(contentPadding)
                .then(
                    if (uiState.showSuccessPopup || uiState.showSkippedPopup) Modifier.blur(10.dp)
                    else Modifier
                )
        ) {
            DoziHeader(firestoreUser = uiState.user, isLoggedIn = uiState.isLoggedIn)

            // 👥 Profil Değiştirici
            // Profil yönetimi kaldırıldı

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            if (selectedDate != null) {
                                selectedDate = null
                            }
                        })
                    }
            ) {
                Spacer(Modifier.height(12.dp))

                HorizontalCalendar(
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        selectedDate = if (selectedDate == date) null else date
                    },
                    onNavigateToReminders = {
                        navController.navigate(Screen.AddReminder.route)
                    },
                    isLoggedIn = uiState.isLoggedIn
                )

                Spacer(Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(600)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                ) {
                    if (uiState.currentMedicineStatus == MedicineStatus.UPCOMING && uiState.upcomingMedicine != null) {
                        val sameTimeMedicines = uiState.allUpcomingMedicines.filter { it.second == uiState.upcomingMedicine!!.second }

                        if (sameTimeMedicines.size == 1) {
                            CurrentMedicineCard(
                                medicine = uiState.upcomingMedicine!!.first,
                                time = uiState.upcomingMedicine!!.second,
                                snoozeMinutes = uiState.snoozeMinutes,
                                onTaken = {
                                    SoundHelper.playSound(context, SoundHelper.SoundType.HERSEY_TAMAM)
                                    viewModel.onMedicineTaken(context, uiState.upcomingMedicine!!.first, uiState.upcomingMedicine!!.second)
                                },
                                onSnooze = { viewModel.setShowSnoozeDialog(true) },
                                onSkip = { viewModel.setShowSkipDialog(true) }
                            )
                        } else {
                            MultiMedicineCard(
                                medicines = sameTimeMedicines,
                                time = uiState.upcomingMedicine!!.second,
                                onTaken = { medicine ->
                                    SoundHelper.playSound(context, SoundHelper.SoundType.HERSEY_TAMAM)
                                    viewModel.onMedicineTaken(context, medicine, uiState.upcomingMedicine!!.second)
                                },
                                onSnooze = { viewModel.setShowSnoozeDialog(true) },
                                onSkip = { medicine ->
                                    SoundHelper.playSound(context, SoundHelper.SoundType.PEKALA)
                                    viewModel.onMedicineSkipped(context, medicine, uiState.upcomingMedicine!!.second)
                                }
                            )
                        }
                    } else {
                        EmptyMedicineCard(
                            currentMedicineStatus = uiState.currentMedicineStatus,
                            nextMedicine = uiState.allUpcomingMedicines.firstOrNull(),
                            isLoggedIn = uiState.isLoggedIn
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 🔥 Streak ve Badi Promotion - Yan yana daha küçük kartlar
                if (uiState.isLoggedIn) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Streak Kartı - Kompakt
                        CompactStreakCard(
                            context = context,
                            medicines = uiState.todaysMedicines,
                            onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                            modifier = Modifier.weight(1f)
                        )

                        // Badi Promotion - Kompakt
                        CompactBadiPromotionCard(
                            isPremium = isPremium,
                            onNavigateToBadi = { navController.navigate(Screen.BadiList.route) },
                            onPremiumRequired = { showBadiPremiumDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                TimelineSection(
                    medicines = uiState.todaysMedicines,
                    expanded = timelineExpanded,
                    context = context,
                    onToggle = {
                        timelineExpanded = !timelineExpanded
                        if (timelineExpanded) {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(
                                    scrollState.maxValue,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    }
                )

                Spacer(Modifier.height(100.dp))
            }
        }

        // ✅ Popup'lar
        if (uiState.showSuccessPopup) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dozi_perfect),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )
            }
        }

        if (uiState.showSkippedPopup) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dozi_happy2),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )
            }
        }

    }

    // ✅ Dialog'lar (ViewModel ile entegre)
    if (uiState.showSkipDialog) {
        val currentMedicine = uiState.upcomingMedicine
        SkipReasonDialog(
            onDismiss = {
                SoundHelper.playSound(context, SoundHelper.SoundType.PEKALA)
                viewModel.setShowSkipDialog(false)
            },
            onConfirm = { reason ->
                SoundHelper.playSound(context, SoundHelper.SoundType.PEKALA)
                currentMedicine?.let {
                    viewModel.onMedicineSkipped(context, it.first, it.second)
                }
            }
        )
    }

    if (uiState.showSnoozeDialog) {
        val currentMedicine = uiState.upcomingMedicine
        SnoozeDialog(
            onDismiss = { viewModel.setShowSnoozeDialog(false) },
            onConfirm = { minutes ->
                currentMedicine?.let {
                    viewModel.onMedicineSnoozed(context, it.first, it.second, minutes)
                }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DoziHeader(firestoreUser: User?, isLoggedIn: Boolean) {
    val hour = LocalTime.now().hour
    val greeting = remember(hour) {
        when (hour) {
            in 6..11 -> "Günaydın"
            in 12..17 -> "İyi günler"
            in 18..21 -> "İyi akşamlar"
            else -> "İyi geceler"
        }
    }

    // Kullanıcı adını al - login olmamışsa isim gösterme
    val userName = if (isLoggedIn) {
        firestoreUser?.name?.split(" ")?.firstOrNull() ?: "Arkadaşım"
    } else {
        null
    }
    val planType = firestoreUser?.planType ?: "free"
    val isPremium = planType != "free"
    var showEditNameDialog by remember { mutableStateOf(false) }
    val canEditName = firestoreUser?.name.isNullOrBlank()

    // ✅ Günün saatine göre renk değişimi
    val (baseGradientStart, baseGradientEnd) = remember(hour) {
        when (hour) {
            in 6..11 -> Pair(Color(0xFFFFB74D), Color(0xFFFF9800)) // Sabah: Turuncu tonları
            in 12..17 -> Pair(DoziTurquoise, DoziBlue) // Öğlen: Turkuaz-Mavi
            in 18..21 -> Pair(DoziCoral, DoziPurple) // Akşam: Mercan-Mor
            else -> Pair(Color(0xFF5E35B1), Color(0xFF311B92)) // Gece: Koyu mor tonları
        }
    }

    val gradientStart by animateColorAsState(
        targetValue = if (isPremium) Color(0xFFFFB300) else baseGradientStart,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "gradientStart"
    )
    val gradientEnd by animateColorAsState(
        targetValue = if (isPremium) Color(0xFFFF6F00) else baseGradientEnd,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "gradientEnd"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (isPremium) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(listOf(DoziGold, Color(0xFFFF8C00))),
                        shape = RoundedCornerShape(24.dp)
                    )
                } else Modifier
            )
            .background(
                Brush.horizontalGradient(
                    listOf(gradientStart, gradientEnd)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Login olmayan kullanıcılar için sadece greeting
                    if (userName == null) {
                        Text(
                            text = "$greeting!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    } else {
                        // Tıklanabilir kullanıcı adı
                        if (canEditName) {
                            Text(
                                text = buildAnnotatedString {
                                    append("$greeting, ")
                                    withStyle(
                                        style = SpanStyle(
                                            textDecoration = TextDecoration.Underline,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    ) {
                                        append(userName)
                                    }
                                    append("!")
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.clickable { showEditNameDialog = true }
                            )
                        } else {
                            Text(
                                text = "$greeting, $userName!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    if (isPremium) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Premium",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isLoggedIn) "İlaçlarını düzenli almayı unutma" else "Dozi ile ilaç takibini kolaylaştır",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Image(
                painter = painterResource(id = R.drawable.dozi_brand),
                contentDescription = "Dozi brand",
                modifier = Modifier
                    .size(64.dp)
                    .offset(y = (-2).dp)
            )
        }
    }

    // Kullanıcı adı düzenleme dialogu
    if (showEditNameDialog) {
        EditNameDialog(
            currentName = firestoreUser?.name ?: "",
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                showEditNameDialog = false
                // Firestore'a kaydet
                val userRepository = UserRepository()
                CoroutineScope(Dispatchers.IO).launch {
                    userRepository.updateUserField("name", newName)
                }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HorizontalCalendar(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onNavigateToReminders: () -> Unit,
    isLoggedIn: Boolean
) {
    val today = LocalDate.now()
    val context = LocalContext.current

    // 🔹 Ay başından ay sonuna kadar tüm günler
    val dates = remember(today) {
        val firstDayOfMonth = today.withDayOfMonth(1)
        val lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(firstDayOfMonth, lastDayOfMonth).toInt()
        (0..daysBetween).map { firstDayOfMonth.plusDays(it.toLong()) }
    }

    // 🔹 Bugünün listede görünmesi için index'i hesapla
    val todayIndex = remember(today, dates) {
        dates.indexOfFirst { it == today }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = todayIndex)
    val coroutineScope = rememberCoroutineScope()

    // 🔹 Medicines listesini Firebase'den al (Real-time Flow ile)
    val medicineRepository = remember { MedicineRepository() }

    // 🔥 BUG FIX: getMedicinesFlow() ile profil değişikliklerini dinle
    val allMedicines by medicineRepository.getMedicinesFlow()
        .collectAsState(initial = emptyList())

    // 🔹 Gerçek statü verisi - SharedPreferences'tan hesapla
    val dayStatuses = remember(allMedicines) {
        dates.associateWith { date ->
            getMedicineStatusForDate(context, date, allMedicines)
        }
    }

    LaunchedEffect(selectedDate) {
        selectedDate?.let { date ->
            val index = dates.indexOf(date)
            if (index != -1) {
                coroutineScope.launch {
                    listState.animateScrollToItem(index)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {})
            }
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.border(1.dp, DoziPrimaryLight, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = today.month.getDisplayName(TextStyle.FULL, Locale("tr")).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(
                    color = VeryLightGray,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
                )

                // 🔹 Ay başından ay sonuna kadar tüm günler
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    items(dates) { date ->
                        val status = dayStatuses[date] ?: MedicineStatus.NONE
                        val isSelected = date == selectedDate
                        CalendarDayCircle(
                            date = date,
                            status = status,
                            isSelected = isSelected,
                            onClick = {
                                onDateSelected(date)
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedDate != null,
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            selectedDate?.let { date ->
                val status = dayStatuses[date] ?: MedicineStatus.NONE
                CalendarExpandedContent(
                    date = date,
                    status = status,
                    onNavigateToReminders = onNavigateToReminders,
                    context = context,
                    allMedicines = allMedicines,
                    isLoggedIn = isLoggedIn
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarDayCircle(
    date: LocalDate,
    status: MedicineStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val today = LocalDate.now()
    val isToday = date == today
    val haptic = LocalHapticFeedback.current

    // 🔹 Yaşlılar için daha açık ve ayırt edici renkler
    val color = when (status) {
        MedicineStatus.TAKEN -> SuccessGreen          // ✅ Yeşil: Alındı
        MedicineStatus.PARTIAL -> WarningOrange       // 🟠 Turuncu: Kısmen alındı
        MedicineStatus.SKIPPED -> ErrorRed            // ❌ Kırmızı: Atlandı
        MedicineStatus.PLANNED -> DoziBlue            // 📅 Mavi: İleride planlanmış
        MedicineStatus.UPCOMING -> DoziTurquoise      // ⏰ Turkuaz: Bugün sırada
        else -> Gray200                               // ⚪ Gri: İlaç yok
    }

    val displayDay = date.dayOfMonth.toString()

    // Animated scale for selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "calendarScale"
    )

    // Animated ring for selected day
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 52.dp else 44.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 3.dp,
                            color = color.copy(alpha = ringAlpha),
                            shape = CircleShape
                        )
                    } else Modifier
                )
                .clip(CircleShape)
                .background(color.copy(alpha = if (status == MedicineStatus.NONE) 0.15f else 0.25f))
                .border(
                    width = if (isSelected) 2.dp else 2.dp,
                    color = color,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Bugün ise küçük Dozi emoji göster
                if (isToday) {
                    Text(
                        text = "💊",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
                Text(
                    text = displayDay,
                    color = if (status == MedicineStatus.NONE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = if (isSelected) 17.sp else if (isToday) 16.sp else 15.sp,
                    modifier = if (isToday) Modifier.offset(y = (-1).dp) else Modifier
                )
            }
        }

        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("tr", "TR")),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) DoziTurquoise else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarExpandedContent(
    date: LocalDate,
    status: MedicineStatus,
    onNavigateToReminders: () -> Unit,
    context: Context,
    allMedicines: List<Medicine>,
    isLoggedIn: Boolean
) {
    val dayLabel = "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale("tr", "TR"))}"
    val medicines = remember(date, allMedicines) {
        getMedicineRecordsForDate(context, date, allMedicines)
    }

    val character = when (status) {
        MedicineStatus.TAKEN -> R.drawable.dozi_perfect
        MedicineStatus.PARTIAL -> R.drawable.dozi_bravo
        MedicineStatus.SKIPPED -> R.drawable.dozi_unhappy
        MedicineStatus.PLANNED -> R.drawable.dozi_time
        else -> R.drawable.dozi
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, DoziPrimaryLight, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            Image(
                painter = painterResource(id = character),
                contentDescription = "Dozi durumu",
                modifier = Modifier
                    .size(64.dp)
                    .padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dayLabel.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))

                if (status == MedicineStatus.NONE) {
                    ClickableReminderText(onNavigateToReminders, isLoggedIn, date)
                } else {
                    medicines.forEach { med ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when (med.status) {
                                        MedicineStatus.TAKEN -> SuccessGreen.copy(alpha = 0.15f)
                                        MedicineStatus.SKIPPED -> ErrorRed.copy(alpha = 0.15f)
                                        MedicineStatus.UPCOMING -> DoziPurple.copy(alpha = 0.1f)
                                        else -> VeryLightGray.copy(alpha = 0.4f)
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${med.time}  •  ${med.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = when (med.status) {
                                    MedicineStatus.TAKEN -> Icons.Default.Check
                                    MedicineStatus.SKIPPED -> Icons.Default.Close
                                    MedicineStatus.UPCOMING -> Icons.Default.Alarm
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = when (med.status) {
                                    MedicineStatus.TAKEN -> SuccessGreen
                                    MedicineStatus.SKIPPED -> ErrorRed
                                    MedicineStatus.UPCOMING -> DoziPurple
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ClickableReminderText(onNavigateToReminders: () -> Unit, isLoggedIn: Boolean, date: LocalDate) {
    val today = LocalDate.now()
    val isToday = date == today
    val isFuture = date.isAfter(today)

    if (isLoggedIn) {
        // Sadece bugün ve gelecek tarihler için hatırlatma ekleme seçeneği göster
        if (isToday || isFuture) {
            ClickableText(
                text = buildAnnotatedString {
                    append("💧 ${if (isToday) "Bugün" else "Bu tarih"} için planlanmış bir ilacın yok.\n\n")
                    append("Yeni bir hatırlatma eklemek için ")
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = DoziPurple,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("buraya tıklayabilirsin.")
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp),
                onClick = { offset ->
                    // Tüm metin tıklanınca Hatırlatmalar ekranına yönlendir
                    onNavigateToReminders()
                }
            )
        } else {
            // Geçmiş tarihler için sadece bilgilendirme göster
            Text(
                text = "💧 Bu tarih için planlanmış bir ilacın yoktu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Text(
            text = "💧 Giriş yaparsan ilaçlarını beraber takip edebiliriz!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CurrentMedicineCard(
    medicine: Medicine,
    time: String,
    snoozeMinutes: Int,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var remainingSeconds by remember { mutableStateOf(0) }
    var snoozeTargetTime by remember { mutableStateOf("") }

    // ✅ Bugün bu ilacın kaç dozu olduğunu göster
    val todayDosesForThisMedicine = medicine.times.size
    val currentDoseIndex = medicine.times.indexOf(time) + 1

    // ⏰ Zaman kontrolü
    val currentTime = LocalTime.now()
    val (hour, minute) = time.split(":").map { it.toInt() }
    val medicineTime = LocalTime.of(hour, minute)

    // 30 dakika öncesinden itibaren "AL" aktif olsun
    val minutesUntilMedicine = java.time.Duration.between(currentTime, medicineTime).toMinutes()
    val canTakeMedicine = minutesUntilMedicine <= 30
    val canSnooze = minutesUntilMedicine <= 60 && minutesUntilMedicine > -15 // 1 saat önceden, 15 dk sonrasına kadar
    val isOverdue = minutesUntilMedicine < 0 // Vakti geçti mi?

    // Zaman durumu mesajı
    val timeStatusMessage = when {
        minutesUntilMedicine > 30 -> "İlaç vaktinize ${if (minutesUntilMedicine < 60) "${minutesUntilMedicine} dakika" else "${minutesUntilMedicine / 60} saat ${minutesUntilMedicine % 60} dakika"} var"
        minutesUntilMedicine in 1..30 -> "Vakti yaklaşıyor"
        minutesUntilMedicine >= -15 -> "Vakti geçti!"
        else -> "Çok geç!"
    }

    // ✅ Pulsing animation for the badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // ✅ Vakti geçmiş ilaçlar için kartın tamamı yanıp sönsün
    val cardPulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isOverdue) 0.6f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isOverdue) 600 else 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cardPulseAlpha"
    )

    // ✅ snooze_until'e göre kalan süreyi hesapla
    LaunchedEffect(snoozeMinutes) {
        if (snoozeMinutes > 0) {
            val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
            val snoozeUntil = prefs.getLong("snooze_until", 0)

            // Hedef saati hesapla
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = snoozeUntil
            }
            val targetHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val targetMinute = calendar.get(java.util.Calendar.MINUTE)
            snoozeTargetTime = String.format("%02d:%02d", targetHour, targetMinute)

            while (snoozeUntil > System.currentTimeMillis()) {
                val remainingMillis = snoozeUntil - System.currentTimeMillis()
                remainingSeconds = (remainingMillis / 1000).toInt()

                if (remainingSeconds <= 0) break

                delay(1000)
            }

            // Süre doldu
            remainingSeconds = 0
            snoozeTargetTime = ""
        }
    }

    // Glow efekti için animated border
    val glowTransition = rememberInfiniteTransition(label = "cardGlow")
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = cardPulseAlpha
                }
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            DoziPrimary.copy(alpha = glowAlpha),
                            DoziSecondary.copy(alpha = glowAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isOverdue)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Gradient accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(DoziPrimary, DoziSecondary)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            Surface(
                color = if (isOverdue) ErrorRed else DoziTurquoise,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 0.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "SIRADA",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        letterSpacing = 1.sp
                    )
                }
            }

            if (remainingSeconds > 0 && snoozeTargetTime.isNotEmpty()) {
                Surface(
                    color = WarningOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "⏰ Saat $snoozeTargetTime'de hatırlatılacak",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                        Text(
                            "${remainingSeconds / 60} dk ${remainingSeconds % 60} sn kaldı",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningOrange.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isOverdue) ErrorRed else DoziPrimary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    time,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverdue) ErrorRed else DoziPrimary
                )
            }

            Text(
                "${medicine.icon} ${medicine.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📦 ${medicine.dosage}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // ✅ Bugün kaç doz olduğunu göster
                Surface(
                    color = DoziTurquoise.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "$currentDoseIndex/$todayDosesForThisMedicine. doz",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DoziTurquoise
                    )
                }
            }

            HorizontalDivider(color = VeryLightGray, thickness = 1.dp)

            // Zaman durumu uyarısı
            if (minutesUntilMedicine != 0L) {
                Surface(
                    color = when {
                        isOverdue -> DoziRed.copy(alpha = 0.15f)
                        minutesUntilMedicine in 1..30 -> WarningOrange.copy(alpha = 0.15f)
                        else -> DoziTurquoise.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when {
                                isOverdue -> Icons.Default.Warning
                                minutesUntilMedicine in 1..30 -> Icons.Default.Schedule
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when {
                                isOverdue -> ErrorRed
                                minutesUntilMedicine in 1..30 -> WarningOrange
                                else -> DoziTurquoise
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            timeStatusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isOverdue -> ErrorRed
                                minutesUntilMedicine in 1..30 -> WarningOrange
                                else -> DoziTurquoise
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionButton(
                    text = "AL",
                    icon = Icons.Default.Check,
                    color = if (canTakeMedicine) SuccessGreen else Gray200,
                    modifier = Modifier.weight(1f),
                    enabled = canTakeMedicine,
                    onClick = onTaken
                )

                ActionButton(
                    text = "ERTELE",
                    icon = Icons.Default.AccessTime,
                    color = if (canSnooze) WarningOrange else Gray200,
                    modifier = Modifier.weight(1f),
                    enabled = canSnooze,
                    onClick = {
                        SoundHelper.playSound(context, SoundHelper.SoundType.ERTELE)
                        onSnooze()
                    }
                )

                ActionButton(
                    text = "ATLA",
                    icon = Icons.Default.Close,
                    color = ErrorRed,
                    modifier = Modifier.weight(1f),
                    onClick = onSkip
                )
            }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EmptyMedicineCard(
    currentMedicineStatus: MedicineStatus,
    nextMedicine: Pair<Medicine, String>?,
    isLoggedIn: Boolean
) {
    // ✅ Bugün kalan ilaç sayısını hesapla
    val context = LocalContext.current
    val today = getCurrentDateString()
    val medicineRepository = remember { MedicineRepository() }
    val allMedicines by medicineRepository.getMedicinesFlow()
        .collectAsState(initial = emptyList())
    val todayDate = LocalDate.now()
    val todaysMedicines = allMedicines.filter { shouldMedicineShowOnDate(it, todayDate) }
    val totalDosesToday = todaysMedicines.sumOf { it.times.size }
    val takenDosesToday = todaysMedicines.sumOf { medicine ->
        medicine.times.count { time ->
            getMedicineStatus(context, medicine.id, today, time) == "taken"
        }
    }
    val skippedDosesToday = todaysMedicines.sumOf { medicine ->
        medicine.times.count { time ->
            getMedicineStatus(context, medicine.id, today, time)?.startsWith("skipped") == true
        }
    }
    val remainingDoses = totalDosesToday - takenDosesToday - skippedDosesToday

    // Vakti geçmiş ama alınmamış/atlanmamış dozları say
    val currentTime = LocalTime.now()
    val overdueDoses = todaysMedicines.sumOf { medicine ->
        medicine.times.count { time ->
            val status = getMedicineStatus(context, medicine.id, today, time)
            if (status == "taken" || status?.startsWith("skipped") == true) {
                false
            } else {
                // Zamanı geçmiş mi kontrol et
                val (hour, minute) = time.split(":").map { it.toInt() }
                val medicineTime = LocalTime.of(hour, minute)
                currentTime.isAfter(medicineTime)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, DoziPrimaryLight, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.dozi_happy2),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )

            Text(
                if (!isLoggedIn) {
                    "Dozi ile tanışalım!"
                } else {
                    when (currentMedicineStatus) {
                        MedicineStatus.TAKEN -> "Harika! İlacını aldın"
                        MedicineStatus.SKIPPED -> "Bugün için başka ilaç yok"
                        else -> "Henüz ilaç zamanı değil"
                    }
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // ✅ Kalan doz bilgisi göster (eğer varsa)
            if (!isLoggedIn) {
                Text(
                    "Giriş yaparsan ilaçlarını beraber takip edebiliriz! 💊",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "İlaç hatırlatmaları, düzenli takip ve senkronize edilmiş veriler için giriş yapman yeterli.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            } else if (overdueDoses > 0 && currentMedicineStatus != MedicineStatus.TAKEN) {
                // Vakti geçmiş dozlar varsa kırmızı uyarı göster
                Surface(
                    color = DoziRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "⚠️ $overdueDoses doz vakti geçti!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = DoziRed
                        )
                        if (nextMedicine != null) {
                            Text(
                                "Lütfen ilacınızı almayı unutmayın",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (remainingDoses > 0 && currentMedicineStatus != MedicineStatus.TAKEN) {
                // Kalan doz varsa göster
                Surface(
                    color = DoziTurquoise.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Bugün $remainingDoses doz daha var",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = DoziTurquoise
                        )
                        if (nextMedicine != null) {
                            Text(
                                "Sıradaki: ${nextMedicine.first.name} • ${nextMedicine.second}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (nextMedicine != null && currentMedicineStatus != MedicineStatus.TAKEN && currentMedicineStatus != MedicineStatus.SKIPPED) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = DoziTurquoise.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Sıradaki Hatırlatma",
                            style = MaterialTheme.typography.labelMedium,
                            color = DoziTurquoise,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${nextMedicine.first.icon} ${nextMedicine.first.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = DoziTurquoise,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                nextMedicine.second,
                                style = MaterialTheme.typography.bodyLarge,
                                color = DoziTurquoise,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Text(
                    when (currentMedicineStatus) {
                        MedicineStatus.TAKEN -> "Sağlığınla ilgilendiğin için teşekkürler!"
                        MedicineStatus.SKIPPED -> "Yarın için planlanmış ilaçların var"
                        else -> "Vakti gelince seni uyaracağım"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.White
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.White
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TimelineSection(
    medicines: List<Medicine>,
    expanded: Boolean,
    context: Context,
    onToggle: () -> Unit
) {
    val currentTime = LocalTime.now()
    val today = getCurrentDateString()

    // Create timeline items from medicines with their times
    val timelineItems = medicines.flatMap { medicine ->
        medicine.times.map { time ->
            val savedStatus = getMedicineStatus(context, medicine.id, today, time)
            val status = when {
                savedStatus == "taken" -> TimelineStatus.TAKEN
                savedStatus == "skipped" -> TimelineStatus.SKIPPED
                savedStatus?.startsWith("snoozed_") == true -> TimelineStatus.SNOOZED
                else -> determineTimelineStatus(time, currentTime)
            }
            Triple(medicine, time, status to savedStatus)
        }
    }.sortedBy { it.second }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Bugünün İlaçları",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = DoziTurquoise,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        timelineItems.size.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = DoziTurquoise,
                modifier = Modifier.size(28.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            if (timelineItems.isEmpty()) {
                // Show empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Bugün için planlanmış ilaç yok",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(Modifier.height(8.dp))

                    timelineItems.forEachIndexed { index, (medicine, time, status) ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 400,
                                    delayMillis = index * 100,
                                    easing = FastOutSlowInEasing
                                )
                            ) + slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 400,
                                    delayMillis = index * 100,
                                    easing = FastOutSlowInEasing
                                ),
                                initialOffsetY = { it / 3 }
                            )
                        ) {
                            TimelineItem(
                                time = time,
                                medicineName = "${medicine.icon} ${medicine.name}",
                                dosageText = "${medicine.dosage} ${medicine.unit}",
                                status = status.first,
                                savedStatus = status.second,
                                currentTime = currentTime
                            )
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun determineTimelineStatus(time: String, currentTime: LocalTime): TimelineStatus {
    val (hour, minute) = time.split(":").map { it.toInt() }
    val medicineTime = LocalTime.of(hour, minute)

    return when {
        medicineTime.isBefore(currentTime.minusMinutes(30)) -> TimelineStatus.COMPLETED
        medicineTime.isAfter(currentTime.plusMinutes(30)) -> TimelineStatus.UPCOMING
        else -> TimelineStatus.CURRENT
    }
}

enum class TimelineStatus {
    COMPLETED, CURRENT, UPCOMING, TAKEN, SKIPPED, SNOOZED
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TimelineItem(
    time: String,
    medicineName: String,
    dosageText: String,
    status: TimelineStatus,
    savedStatus: String?,
    currentTime: LocalTime
) {
    val subtitle = when (status) {
        TimelineStatus.TAKEN -> "✅ Alındı"
        TimelineStatus.SKIPPED -> "⏭️ Atlandı"
        TimelineStatus.SNOOZED -> {
            val snoozeUntil = savedStatus?.substringAfter("snoozed_")?.toLongOrNull()
            if (snoozeUntil != null && snoozeUntil > System.currentTimeMillis()) {
                val remainingMs = snoozeUntil - System.currentTimeMillis()
                val remainingMin = (remainingMs / 60_000).toInt()
                "⏰ $remainingMin dk ertelendi"
            } else {
                "⏰ Ertelendi"
            }
        }
        else -> {
            val (hour, minute) = time.split(":").map { it.toInt() }
            val medicineTime = LocalTime.of(hour, minute)
            when (status) {
                TimelineStatus.COMPLETED -> {
                    val diff = java.time.Duration.between(medicineTime, currentTime)
                    val hours = diff.toHours()
                    if (hours > 0) "$hours saat önce" else "Az önce"
                }
                TimelineStatus.CURRENT -> "ŞİMDİ"
                TimelineStatus.UPCOMING -> {
                    val diff = java.time.Duration.between(currentTime, medicineTime)
                    val hours = diff.toHours()
                    if (hours > 0) "$hours saat sonra" else "${diff.toMinutes()} dk sonra"
                }
                else -> ""
            }
        }
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                TimelineStatus.TAKEN -> SuccessGreen.copy(alpha = 0.1f)
                TimelineStatus.SKIPPED -> ErrorRed.copy(alpha = 0.1f)
                TimelineStatus.SNOOZED -> WarningOrange.copy(alpha = 0.1f)
                TimelineStatus.COMPLETED -> Color.White
                TimelineStatus.CURRENT -> DoziCoralLight.copy(alpha = 0.15f)
                TimelineStatus.UPCOMING -> Color.White
            } as Color
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = when (status) {
                TimelineStatus.CURRENT -> 2.dp
                TimelineStatus.TAKEN, TimelineStatus.SKIPPED, TimelineStatus.SNOOZED -> 2.dp
                else -> 1.dp
            },
            color = when (status) {
                TimelineStatus.TAKEN -> SuccessGreen
                TimelineStatus.SKIPPED -> ErrorRed
                TimelineStatus.SNOOZED -> WarningOrange
                TimelineStatus.COMPLETED -> DoziTurquoise.copy(alpha = 0.4f)
                TimelineStatus.CURRENT -> DoziRed
                TimelineStatus.UPCOMING -> VeryLightGray
            }
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol taraf: İlaç bilgileri
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (status) {
                                TimelineStatus.TAKEN -> SuccessGreen
                                TimelineStatus.SKIPPED -> ErrorRed
                                TimelineStatus.SNOOZED -> WarningOrange
                                TimelineStatus.COMPLETED -> DoziTurquoise
                                TimelineStatus.CURRENT -> DoziRed
                                TimelineStatus.UPCOMING -> LightGray
                            }
                        )
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            dosageText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DoziTurquoise,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (status) {
                                TimelineStatus.TAKEN -> SuccessGreen
                                TimelineStatus.SKIPPED -> ErrorRed
                                TimelineStatus.SNOOZED -> WarningOrange
                                TimelineStatus.CURRENT -> DoziRed
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (status == TimelineStatus.CURRENT) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Sağ taraf: Saat
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    time,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (status) {
                        TimelineStatus.CURRENT -> DoziRed
                        TimelineStatus.COMPLETED -> DoziTurquoise
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun SnoozeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(10) }
    val options = listOf(10, 15, 30, 60)
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Ne Kadar Erteleyelim?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    "İlacını almak için biraz daha zamana mı ihtiyacın var?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { minutes ->
                        SnoozeOption(
                            minutes = minutes,
                            selected = selectedMinutes == minutes,
                            onClick = { selectedMinutes = minutes }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("İptal")
                    }

                    Button(
                        onClick = {
                            // ✅ SharedPreferences'a kaydet
                            context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE).edit()
                                .putInt("snooze_minutes", selectedMinutes)
                                .putLong("snooze_until", System.currentTimeMillis() + selectedMinutes * 60_000L)
                                .putLong("snooze_timestamp", System.currentTimeMillis()) // ✅ Zaman damgası
                                .apply()

                            onConfirm(selectedMinutes)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarningOrange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ertele")
                    }
                }
            }
        }
    }
}

@Composable
private fun SnoozeOption(
    minutes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) WarningOrange.copy(alpha = 0.1f) else VeryLightGray,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) WarningOrange else Color.Transparent
        )
    ) {
        Text(
            "$minutes dakika",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = if (selected) WarningOrange else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SkipReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var customNote by remember { mutableStateOf("") }
    var showCustomNoteField by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val reasons = listOf(
        "💊 İlacım bitti",
        "😴 Unutmuşum",
        "🏥 Doktor değiştirdi",
        "🤢 Yan etki yaşıyorum",
        "✨ Kendimi iyi hissediyorum",
        "🕐 Daha sonra alacağım",
        "✏️ Başka bir neden (not yaz)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "Neden Atlamak İstiyorsun?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    "Sebebini bilmek, ilaç takibini daha iyi yapmamı sağlar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reasons.forEach { reason ->
                        ReasonChip(
                            text = reason,
                            selected = selectedReason == reason,
                            onClick = {
                                selectedReason = reason
                                showCustomNoteField = reason.contains("Başka bir neden")
                            }
                        )
                    }
                }

                // Özel not alanı
                AnimatedVisibility(visible = showCustomNoteField) {
                    OutlinedTextField(
                        value = customNote,
                        onValueChange = { customNote = it },
                        label = { Text("Notunuz (opsiyonel)") },
                        placeholder = { Text("Örn: Bugün mide bulantım var") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoziTurquoise,
                            focusedLabelColor = DoziTurquoise
                        ),
                        maxLines = 3
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Gray200)
                    ) {
                        Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            val finalReason = if (showCustomNoteField && customNote.isNotBlank()) {
                                "Diğer: $customNote"
                            } else {
                                selectedReason ?: ""
                            }
                            onConfirm(finalReason)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedReason != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarningOrange,
                            disabledContainerColor = Gray200
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Atla", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReasonChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) DoziTurquoise.copy(alpha = 0.15f) else Color.White,
        border = BorderStroke(
            width = 2.dp,
            color = if (selected) DoziTurquoise else Gray200
        ),
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = DoziTurquoise,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) DoziTurquoise else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }
    var isError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = DoziTurquoise,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "Adını Düzenle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    "Seni nasıl çağırayım?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        isError = it.isBlank()
                    },
                    label = { Text("Adın") },
                    placeholder = { Text("Örn: Ufuk") },
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = DoziTurquoise)
                    },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Lütfen bir isim girin", color = ErrorRed) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DoziTurquoise,
                        focusedLabelColor = DoziTurquoise,
                        cursorColor = DoziTurquoise,
                        unfocusedBorderColor = VeryLightGray,
                        focusedContainerColor = DoziTurquoise.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White,
                        errorBorderColor = ErrorRed,
                        errorLabelColor = ErrorRed
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, VeryLightGray)
                    ) {
                        Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            if (nameText.isNotBlank()) {
                                onConfirm(nameText)
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DoziTurquoise
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Kaydet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
/**
 * 🔥 Streak ve Günlük Özet Kartı
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun StreakAndDailySummaryCard(
    context: Context,
    medicines: List<Medicine>,
    onNavigateToStats: () -> Unit
) {
    val today = getCurrentDateString()

    // ✅ Doz bazlı hesaplama: Her bir ilacın her bir saatini ayrı ayrı say
    val takenCount = medicines.sumOf { medicine ->
        medicine.times.count { time ->
            getMedicineStatus(context, medicine.id, today, time) == "taken"
        }
    }
    val totalDoses = medicines.sumOf { it.times.size }
    val progress = if (totalDoses > 0) takenCount.toFloat() / totalDoses else 0f

    // ✅ Streak bilgisi - Bugün %100 başarılıysa streak'i artır
    val prefs = context.getSharedPreferences("dozi_streak", Context.MODE_PRIVATE)
    var currentStreak by remember { mutableStateOf(prefs.getInt("current_streak", 0)) }

    // ✅ Bugünün streak'ini güncelle (sadece bir kez, gün değiştiğinde)
    LaunchedEffect(today, takenCount, totalDoses) {
        val lastStreakDate = prefs.getString("last_streak_date", "")

        // Bugün için streak güncellemesi yapıldı mı?
        if (lastStreakDate != today && totalDoses > 0) {
            // Bugün %100 başarılıysa streak'i artır
            if (takenCount == totalDoses) {
                val newStreak = currentStreak + 1
                prefs.edit()
                    .putInt("current_streak", newStreak)
                    .putString("last_streak_date", today)
                    .apply()
                currentStreak = newStreak
            } else if (takenCount < totalDoses && !LocalTime.now().isBefore(LocalTime.of(23, 0))) {
                // Gün sonunda %100 değilse streak sıfırla (sadece saat 23:00'dan sonra)
                prefs.edit()
                    .putInt("current_streak", 0)
                    .putString("last_streak_date", today)
                    .apply()
                currentStreak = 0
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onNavigateToStats),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol: Günlük Özet (Kompakt)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Progress bar circle
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(48.dp),
                        color = DoziTurquoise,
                        strokeWidth = 4.dp,
                        trackColor = DoziTurquoise.copy(alpha = 0.15f)
                    )
                    Text(
                        "$takenCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DoziTurquoise
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Bugün",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$takenCount/$totalDoses doz",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Divider
            VerticalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = Gray200
            )

            // Sağ: Streak (Kompakt)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    "🔥",
                    style = MaterialTheme.typography.headlineMedium
                )
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        "$currentStreak gün",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DoziRed
                    )
                    Text(
                        "seri",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// MultiMedicineCard - Aynı saatte birden fazla ilaç olduğunda
@Composable
private fun MultiMedicineCard(
    medicines: List<Pair<Medicine, String>>,
    time: String,
    onTaken: (Medicine) -> Unit,
    onSnooze: () -> Unit,
    onSkip: (Medicine) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "İlaç Zamanı",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = DoziTurquoise,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            time,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = DoziTurquoise
                        )
                    }
                }
                Surface(
                    color = DoziCoral.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "${medicines.size} ilaç",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = DoziCoral
                    )
                }
            }

            Divider(color = Gray200)

            // İlaç Listesi
            medicines.forEach { (medicine, _) ->
                Card(

                    modifier = Modifier.fillMaxWidth(),

                    colors = CardDefaults.cardColors(containerColor = DoziTurquoise.copy(alpha = 0.05f)),

                    shape = RoundedCornerShape(16.dp),

                    elevation = CardDefaults.cardElevation(0.dp)

                ) {

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(16.dp),

                        verticalArrangement = Arrangement.spacedBy(12.dp)

                    ) {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(12.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(

                                medicine.icon,

                                style = MaterialTheme.typography.headlineMedium

                            )

                            Column(modifier = Modifier.weight(1f)) {

                                Text(

                                    medicine.name,

                                    style = MaterialTheme.typography.titleMedium,

                                    fontWeight = FontWeight.Bold,

                                    color = MaterialTheme.colorScheme.onSurface

                                )

                                Text(

                                    medicine.dosage,

                                    style = MaterialTheme.typography.bodyMedium,

                                    color = MaterialTheme.colorScheme.onSurfaceVariant

                                )

                            }

                        }



                        // Butonlar

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            ActionButton(

                                text = "AL",

                                icon = Icons.Default.Check,

                                color = SuccessGreen,

                                modifier = Modifier.weight(1f),

                                onClick = { onTaken(medicine) }

                            )



                            ActionButton(

                                text = "ERTELE",

                                icon = Icons.Default.AccessTime,

                                color = WarningOrange,

                                modifier = Modifier.weight(1f),

                                onClick = onSnooze

                            )
                            ActionButton(

                                text = "ATLA",

                                icon = Icons.Default.Close,

                                color = ErrorRed,

                                modifier = Modifier.weight(1f),

                                onClick = { onSkip(medicine) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 👥 Badi Promotion Card - Kullanıcıları Badi özelliğine yönlendirir
@Composable
private fun BadiPromotionCard(
    onNavigateToBadi: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onNavigateToBadi() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(DoziTurquoise.copy(alpha = 0.15f), DoziPurple.copy(alpha = 0.15f))
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(DoziTurquoise, DoziPurple)),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Text content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Badi ile Birlikte Takip Et",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFF1A237E)
                    )
                    Text(
                        text = "Sevdiklerinle ilaç takibini paylaş, onlar da senin için hatırlatsın!",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = Color(0xFF546E7A),
                        lineHeight = 16.sp
                    )
                }

                // Arrow icon
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = DoziTurquoise,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}


/**
 * 🔥 Kompakt Streak Kartı - Yan yana kullanım için
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CompactStreakCard(
    context: Context,
    medicines: List<Medicine>,
    onNavigateToStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = getCurrentDateString()
    val takenCount = medicines.sumOf { medicine ->
        medicine.times.count { time ->
            getMedicineStatus(context, medicine.id, today, time) == "taken"
        }
    }
    val totalDoses = medicines.sumOf { it.times.size }

    val prefs = context.getSharedPreferences("dozi_streak", Context.MODE_PRIVATE)
    val currentStreak = prefs.getInt("current_streak", 0)

    // Streak seviyesine göre emoji
    val streakEmoji = when {
        currentStreak == 0 -> "💧"
        currentStreak in 1..6 -> "🔥"
        currentStreak in 7..29 -> "🔥🔥"
        currentStreak in 30..99 -> "💪"
        currentStreak in 100..364 -> "🌟"
        else -> "👑" // 365+ gün
    }

    val streakTitle = when {
        currentStreak == 0 -> "Başla"
        currentStreak in 1..6 -> "Streak"
        currentStreak in 7..29 -> "Güçlü"
        currentStreak in 30..99 -> "Harika"
        currentStreak in 100..364 -> "Efsane"
        else -> "Kral"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable { onNavigateToStats() }
            .border(1.dp, DoziSecondaryLight, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(DoziSecondary.copy(alpha = 0.1f), DoziSecondary.copy(alpha = 0.05f))
                    )
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = streakEmoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "$currentStreak gün",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DoziSecondary
            )
            Text(
                text = streakTitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

/**
 * 👥 Kompakt Badi Promotion Kartı - Yan yana kullanım için
 */
@Composable
private fun CompactBadiPromotionCard(
    isPremium: Boolean,
    onNavigateToBadi: () -> Unit,
    onPremiumRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable {
                if (isPremium) {
                    onNavigateToBadi()
                } else {
                    onPremiumRequired()
                }
            }
            .border(1.dp, DoziPrimaryLight, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(DoziPrimary.copy(alpha = 0.1f), DoziPrimary.copy(alpha = 0.05f))
                    )
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                tint = DoziTurquoise,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "Badi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DoziTurquoise
            )
            Text(
                text = "Birlikte takip et",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
