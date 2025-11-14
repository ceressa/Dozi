package com.bardino.dozi.core.ui.screens.home

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.bardino.dozi.R
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.data.repository.UserRepository
import com.bardino.dozi.core.ui.screens.home.MedicineStatus
import com.bardino.dozi.core.ui.theme.*
import com.bardino.dozi.core.utils.SoundHelper
import com.bardino.dozi.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

// Helper function: Get medicine status for a specific date with percentage-based logic
fun getMedicineStatusForDate(context: Context, date: LocalDate, medicines: List<Medicine>): MedicineStatus {
    val dateString = "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)
    val prefs = context.getSharedPreferences("medicine_status", Context.MODE_PRIVATE)

    var totalDoses = 0
    var takenCount = 0
    var skippedCount = 0
    var upcomingCount = 0

    medicines.forEach { medicine ->
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

    // ✅ ViewModelFactory ile context inject et
    val viewModel: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(context.applicationContext) as T
            }
        }
    )

    // ✅ ViewModel'den state'leri al
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Local UI state'leri (sadece UI için)
    var timelineExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // ✅ Context gerektiren ViewModel fonksiyonlarını çağır
    LaunchedEffect(context) {
        viewModel.refreshMedicines(context)
        viewModel.loadSnoozeStateFromContext(context)
        viewModel.startSnoozeTimerWithContext(context)
    }

    // ✅ Periyodik ilaç güncelleme (context gerektiğinde)
    LaunchedEffect(context) {
        while (true) {
            delay(3000)
            viewModel.refreshMedicines(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFB2EBF2),
                            Color(0xFFE0F7FA),
                            Color(0xFFF1F8FB)
                        )
                    )
                )
                .padding(contentPadding)
                .then(
                    if (uiState.showSuccessPopup || uiState.showSkippedPopup) Modifier.blur(10.dp)
                    else Modifier
                )
        ) {
            DoziHeader(firestoreUser = uiState.user, isLoggedIn = uiState.isLoggedIn)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(Modifier.height(4.dp))

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
                    painter = painterResource(id = R.drawable.dozi_ok),
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
                    painter = painterResource(id = R.drawable.dozi_happy),
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



// İlaç durumu kaydetme fonksiyonları
fun saveMedicineStatus(context: Context, medicineId: String, date: String, time: String, status: String) {
    val prefs = context.getSharedPreferences("medicine_status", Context.MODE_PRIVATE)
    val key = "dose_${medicineId}_${date}_${time}"
    prefs.edit().putString(key, status).commit() // commit() senkron, hemen kaydet
    android.util.Log.d("HomeScreen", "Status saved: $key = $status")
}

fun getMedicineStatus(context: Context, medicineId: String, date: String, time: String): String? {
    val prefs = context.getSharedPreferences("medicine_status", Context.MODE_PRIVATE)
    val key = "dose_${medicineId}_${date}_${time}"
    return prefs.getString(key, null)
}

fun getCurrentDateString(): String {
    val calendar = java.util.Calendar.getInstance()
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val month = calendar.get(java.util.Calendar.MONTH) + 1
    val year = calendar.get(java.util.Calendar.YEAR)
    return "%02d/%02d/%d".format(day, month, year)
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

    // ✅ Animated gradient colors
    val gradientStart by animateColorAsState(
        targetValue = if (isPremium) Color(0xFFFFB300) else DoziTurquoise.copy(alpha = 0.95f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "gradientStart"
    )
    val gradientEnd by animateColorAsState(
        targetValue = if (isPremium) Color(0xFFFF6F00) else DoziPurple.copy(alpha = 0.85f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "gradientEnd"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
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
                contentDescription = "Dozi logosu",
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

    // 🔹 Medicines listesini Firebase'den al
    val medicineRepository = remember { MedicineRepository() }
    var allMedicines by remember { mutableStateOf<List<Medicine>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            allMedicines = medicineRepository.getAllMedicines()
        } catch (e: Exception) {
            // Hata durumunda boş liste
        }
    }

    // 🔹 Gerçek statü verisi - SharedPreferences'tan hesapla
    val dayStatuses = remember(allMedicines) {
        dates.associateWith { date ->
            getMedicineStatusForDate(context, date, allMedicines)
        }
    }

    var expandedDate by remember { mutableStateOf<LocalDate?>(null) }

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
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = today.month.getDisplayName(TextStyle.FULL, Locale("tr")).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimaryLight,
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
                        val isSelected = date == expandedDate
                        CalendarDayCircle(
                            date = date,
                            status = status,
                            isSelected = isSelected,
                            onClick = {
                                expandedDate = if (expandedDate == date) null else date
                                onDateSelected(date)
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expandedDate != null,
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            expandedDate?.let { date ->
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clickable(
                interactionSource = interaction,
                indication = rememberRipple(bounded = false, color = color),
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 52.dp else 44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (status == MedicineStatus.NONE) 0.15f else 0.25f))
                .border(
                    width = if (isSelected) 3.dp else 2.dp,
                    color = color,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayDay,
                color = if (status == MedicineStatus.NONE) TextSecondaryLight else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = if (isSelected) 17.sp else 15.sp
            )
        }

        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("tr", "TR")),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryLight,
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
        MedicineStatus.TAKEN -> R.drawable.dozi_happy
        MedicineStatus.PARTIAL -> R.drawable.dozi_ok
        MedicineStatus.SKIPPED -> R.drawable.dozi_unhappy
        MedicineStatus.PLANNED -> R.drawable.dozi_ok
        else -> R.drawable.dozi
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
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
                    color = TextPrimaryLight
                )
                Spacer(Modifier.height(8.dp))

                if (status == MedicineStatus.NONE) {
                    ClickableReminderText(onNavigateToReminders, isLoggedIn)
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
                                color = TextPrimaryLight,
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
                                    else -> TextSecondaryLight
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

@Composable
private fun ClickableReminderText(onNavigateToReminders: () -> Unit, isLoggedIn: Boolean) {
    if (isLoggedIn) {
        ClickableText(
            text = buildAnnotatedString {
                append("💧 Bugün için planlanmış bir ilacın yok.\n\n")
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
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight, lineHeight = 20.sp),
            onClick = { offset ->
                // Tüm metin tıklanınca Hatırlatmalar ekranına yönlendir
                onNavigateToReminders()
            }
        )
    } else {
        Text(
            text = "💧 Login olursan ilaçlarını beraber takip edebiliriz!",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryLight,
            textAlign = TextAlign.Center
        )
    }
}


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

    // ✅ snooze_until'e göre kalan süreyi hesapla
    LaunchedEffect(snoozeMinutes) {
        if (snoozeMinutes > 0) {
            val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
            val snoozeUntil = prefs.getLong("snooze_until", 0)

            while (snoozeUntil > System.currentTimeMillis()) {
                val remainingMillis = snoozeUntil - System.currentTimeMillis()
                remainingSeconds = (remainingMillis / 1000).toInt()

                if (remainingSeconds <= 0) break

                delay(1000)
            }

            // Süre doldu
            remainingSeconds = 0
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = DoziRed,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 4.dp,
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

            if (remainingSeconds > 0) {
                Text(
                    "Ertelendi: ${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WarningOrange
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = DoziRed,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    time,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = DoziRed
                )
            }

            Text(
                "${medicine.icon} ${medicine.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )

            Text(
                "📦 ${medicine.dosage}",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondaryLight
            )

            HorizontalDivider(color = VeryLightGray, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionButton(
                    text = "AL",
                    icon = Icons.Default.Check,
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onTaken
                )

                ActionButton(
                    text = "ERTELE",
                    icon = Icons.Default.AccessTime,
                    color = WarningOrange,
                    modifier = Modifier.weight(1f),
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

@Composable
private fun EmptyMedicineCard(
    currentMedicineStatus: MedicineStatus,
    nextMedicine: Pair<Medicine, String>?,
    isLoggedIn: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.dozi_happy),
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
                color = TextPrimaryLight,
                textAlign = TextAlign.Center
            )

            // Sıradaki hatırlatmayı göster veya login teşvik mesajı
            if (!isLoggedIn) {
                Text(
                    "Login olursan ilaçlarını beraber takip edebiliriz! 💊",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryLight,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "İlaç hatırlatmaları, düzenli takip ve senkronize edilmiş veriler için giriş yapman yeterli.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
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
                            color = TextPrimaryLight
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
                    color = TextSecondaryLight,
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
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
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
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(4.dp)
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(),
                    onClick = onToggle
                )
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
                    color = TextPrimaryLight
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
                        color = TextSecondaryLight
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
                        color = TextPrimaryLight,
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
                            color = TextSecondaryLight
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (status) {
                                TimelineStatus.TAKEN -> SuccessGreen
                                TimelineStatus.SKIPPED -> ErrorRed
                                TimelineStatus.SNOOZED -> WarningOrange
                                TimelineStatus.CURRENT -> DoziRed
                                else -> TextSecondaryLight
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
                        else -> TextSecondaryLight
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = TextPrimaryLight
                )

                Text(
                    "İlacını almak için biraz daha zamana mı ihtiyacın var?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryLight
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
            color = if (selected) WarningOrange else TextPrimaryLight,
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

    val reasons = listOf(
        "Zaten aldım",
        "Daha sonra alacağım",
        "İlacım bitti",
        "Yan etki yaşadım",
        "Unutmuşum",
        "Doktor değiştirdi",
        "Kendimi iyi hissediyorum",
        "Diğer"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Neden Atlamak İstiyorsun?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )

                Text(
                    "İlaç takibini daha iyi yapabilmem için nedenini öğrenmek isterim.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryLight
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reasons.forEach { reason ->
                        ReasonChip(
                            text = reason,
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
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
                            selectedReason?.let { onConfirm(it) }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedReason != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DoziRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Atla")
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
        color = if (selected) DoziRed.copy(alpha = 0.1f) else VeryLightGray,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) DoziRed else Color.Transparent
        )
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = if (selected) DoziRed else TextPrimaryLight,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
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
                        color = TextPrimaryLight
                    )
                }

                Text(
                    "Seni nasıl çağırayım?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryLight
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
                        Text("İptal", color = TextSecondaryLight)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        color = TextPrimaryLight
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

                                    color = TextPrimaryLight

                                )

                                Text(

                                    medicine.dosage,

                                    style = MaterialTheme.typography.bodyMedium,

                                    color = TextSecondaryLight

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
