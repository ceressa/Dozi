package com.bardino.dozi.core.ui.screens.home

import android.content.Context
import android.media.MediaPlayer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.bardino.dozi.core.ui.theme.*
import com.bardino.dozi.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

data class MedicineRecord(
    val time: String,
    val name: String,
    val status: MedicineStatus
)

enum class MedicineStatus {
    TAKEN, SKIPPED, PLANNED, UPCOMING, NONE
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues(),
    onNavigateToMedicines: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var showSkipDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showSuccessPopup by remember { mutableStateOf(false) }
    var showSkippedPopup by remember { mutableStateOf(false) }
    var timelineExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var snoozeMinutes by remember { mutableStateOf(0) }
    var currentMedicineStatus by remember { mutableStateOf<MedicineStatus>(MedicineStatus.UPCOMING) }
    var lastSnoozeTimestamp by remember { mutableStateOf(0L) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // ✅ İlk açılışta kalan süreyi hesapla
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
        val snoozeUntil = prefs.getLong("snooze_until", 0)
        val timestamp = prefs.getLong("snooze_timestamp", 0)

        if (snoozeUntil > System.currentTimeMillis()) {
            val remainingMillis = snoozeUntil - System.currentTimeMillis()
            val remainingMinutes = (remainingMillis / 60_000).toInt() + 1
            snoozeMinutes = remainingMinutes
            lastSnoozeTimestamp = timestamp
        } else if (snoozeUntil > 0) {
            prefs.edit()
                .remove("snooze_minutes")
                .remove("snooze_until")
                .remove("snooze_timestamp")
                .apply()
        }
    }

    // ✅ Süreyi düzenli kontrol et
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
            val snoozeUntil = prefs.getLong("snooze_until", 0)
            val timestamp = prefs.getLong("snooze_timestamp", 0)

            if (timestamp > lastSnoozeTimestamp && snoozeUntil > System.currentTimeMillis()) {
                val remainingMillis = snoozeUntil - System.currentTimeMillis()
                snoozeMinutes = (remainingMillis / 60_000).toInt() + 1
                lastSnoozeTimestamp = timestamp
            } else if (snoozeUntil > 0 && snoozeUntil <= System.currentTimeMillis()) {
                snoozeMinutes = 0
                lastSnoozeTimestamp = 0
                prefs.edit()
                    .remove("snooze_minutes")
                    .remove("snooze_until")
                    .remove("snooze_timestamp")
                    .apply()
            }
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
                    if (showSuccessPopup || showSkippedPopup) Modifier.blur(10.dp)
                    else Modifier
                )
        ) {
            DoziHeader()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(Modifier.height(4.dp))

                // ✅ Takvim (yeni versiyon)
                HorizontalCalendar(
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        selectedDate = if (selectedDate == date) null else date
                    },
                    onNavigateToReminders = {
                        navController.navigate(Screen.AddReminder.route)
                    }
                )

                Spacer(Modifier.height(20.dp))

                if (currentMedicineStatus == MedicineStatus.UPCOMING) {
                    CurrentMedicineCard(
                        snoozeMinutes = snoozeMinutes,
                        onTaken = {
                            playSound(context, R.raw.success)
                            currentMedicineStatus = MedicineStatus.TAKEN
                            showSuccessPopup = true
                            coroutineScope.launch {
                                delay(2000)
                                showSuccessPopup = false
                            }
                        },
                        onSnooze = { showSnoozeDialog = true },
                        onSkip = { showSkipDialog = true }
                    )
                } else {
                    EmptyMedicineCard(currentMedicineStatus)
                }

                Spacer(Modifier.height(24.dp))

                TimelineSection(
                    expanded = timelineExpanded,
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

        // ✅ Popup’lar
        if (showSuccessPopup) {
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

        if (showSkippedPopup) {
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

    // ✅ Dialog’lar
    if (showSkipDialog) {
        SkipReasonDialog(
            onDismiss = {
                playSound(context, R.raw.pekala)
                showSkipDialog = false
            },
            onConfirm = { reason ->
                playSound(context, R.raw.pekala)
                currentMedicineStatus = MedicineStatus.SKIPPED
                showSkipDialog = false
                showSkippedPopup = true
                coroutineScope.launch {
                    delay(2000)
                    showSkippedPopup = false
                }
            }
        )
    }

    if (showSnoozeDialog) {
        SnoozeDialog(
            onDismiss = { showSnoozeDialog = false },
            onConfirm = { minutes ->
                snoozeMinutes = minutes
                showSnoozeDialog = false
            }
        )
    }
}


fun playSound(context: Context, resourceId: Int) {
    try {
        val mediaPlayer = MediaPlayer.create(context, resourceId)
        mediaPlayer?.setOnCompletionListener { it.release() }
        mediaPlayer?.start()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DoziHeader() {
    val hour = LocalTime.now().hour
    val greeting = remember(hour) {
        when (hour) {
            in 6..11 -> "Günaydın"
            in 12..17 -> "İyi günler"
            in 18..21 -> "İyi akşamlar"
            else -> "İyi geceler"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(DoziTurquoise.copy(alpha = 0.95f), DoziPurple.copy(alpha = 0.85f))
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
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "İlaçlarını düzenli almayı unutma",
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
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HorizontalCalendar(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onNavigateToReminders: () -> Unit
) {
    val today = LocalDate.now()

    // 🔹 Sadece 7 gün: bugünün 3 gün öncesi ve 3 gün sonrası
    val dates = remember { (-3..3).map { today.plusDays(it.toLong()) } }

    // 🔹 Bugünün listede ortada olması için başlangıç index’i 3
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 3)
    val coroutineScope = rememberCoroutineScope()

    // 🔹 Örnek statü verisi
    val dayStatuses = remember {
        mapOf(
            today.minusDays(2) to MedicineStatus.TAKEN,
            today.minusDays(1) to MedicineStatus.SKIPPED,
            today to MedicineStatus.UPCOMING,
            today.plusDays(1) to MedicineStatus.PLANNED,
            today.plusDays(2) to MedicineStatus.NONE
        )
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

                // 🔹 Toplam 7 gün, bugün ortada
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
                    onNavigateToReminders = onNavigateToReminders
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

    val color = when (status) {
        MedicineStatus.TAKEN -> SuccessGreen
        MedicineStatus.SKIPPED -> ErrorRed
        MedicineStatus.PLANNED -> WarningOrange
        MedicineStatus.UPCOMING -> DoziPurple
        else -> VeryLightGray
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
                .background(color.copy(alpha = if (status == MedicineStatus.NONE) 0.2f else 0.35f))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = color,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayDay,
                color = if (status == MedicineStatus.NONE) TextSecondaryLight else Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = if (isSelected) 16.sp else 14.sp
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
    onNavigateToReminders: () -> Unit
) {
    val dayLabel = "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale("tr", "TR"))}"
    val medicines = listOf(
        MedicineRecord("08:00", "Lustral 100mg", MedicineStatus.TAKEN),
        MedicineRecord("14:00", "Benexol 50mg", MedicineStatus.SKIPPED),
        MedicineRecord("22:00", "Vitamin D3 20mg", MedicineStatus.UPCOMING)
    )

    val character = when (status) {
        MedicineStatus.TAKEN -> R.drawable.dozi_happy
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
                    ClickableReminderText(onNavigateToReminders)
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
private fun ClickableReminderText(onNavigateToReminders: () -> Unit) {
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
}


@Composable
private fun CurrentMedicineCard(
    snoozeMinutes: Int,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var remainingSeconds by remember { mutableStateOf(0) }

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
                shadowElevation = 4.dp
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
                    "14:00",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = DoziRed
                )
            }

            Text(
                "💊 Aspirin 100mg",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )

            Text(
                "📦 1 tablet",
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
                        playSound(context, R.raw.ertele)
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
private fun EmptyMedicineCard(status: MedicineStatus) {
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
                when (status) {
                    MedicineStatus.TAKEN -> "Harika! İlacını aldın"
                    MedicineStatus.SKIPPED -> "Bugün için başka ilaç yok"
                    else -> "Henüz ilaç zamanı değil"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight,
                textAlign = TextAlign.Center
            )

            Text(
                when (status) {
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

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
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

@Composable
private fun TimelineSection(
    expanded: Boolean,
    onToggle: () -> Unit
) {
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
                        "3",
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(8.dp))

                TimelineItem(
                    time = "08:00",
                    medicineName = "Lustral 100mg",
                    status = TimelineStatus.COMPLETED,
                    subtitle = "2 saat önce"
                )

                TimelineItem(
                    time = "14:00",
                    medicineName = "Benexol 50mg",
                    status = TimelineStatus.CURRENT,
                    subtitle = "ŞİMDİ"
                )

                TimelineItem(
                    time = "22:00",
                    medicineName = "Vitamin D3",
                    status = TimelineStatus.UPCOMING,
                    subtitle = "8 saat sonra"
                )
            }
        }
    }
}

enum class TimelineStatus {
    COMPLETED, CURRENT, UPCOMING
}

@Composable
private fun TimelineItem(
    time: String,
    medicineName: String,
    status: TimelineStatus,
    subtitle: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                TimelineStatus.COMPLETED -> Color.White
                TimelineStatus.CURRENT -> DoziCoralLight.copy(alpha = 0.15f)
                TimelineStatus.UPCOMING -> Color.White
            } as Color
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = when (status) {
                TimelineStatus.CURRENT -> 2.dp
                else -> 1.dp
            },
            color = when (status) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (status) {
                                TimelineStatus.COMPLETED -> DoziTurquoise
                                TimelineStatus.CURRENT -> DoziRed
                                TimelineStatus.UPCOMING -> LightGray
                            }
                        )
                )

                Column {
                    Text(
                        medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (status) {
                            TimelineStatus.CURRENT -> DoziRed
                            else -> TextSecondaryLight
                        },
                        fontWeight = if (status == TimelineStatus.CURRENT) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

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