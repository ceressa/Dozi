package com.bardino.dozi.core.ui.screens.stats

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.data.model.UserStats
import com.bardino.dozi.core.data.model.Achievement
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.components.base.DoziInsightCard
import com.bardino.dozi.core.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val viewModel: StatsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A237E),
                        Color(0xFF0D47A1),
                        DoziCharacterLight
                    ),
                    startY = 0f,
                    endY = 600f
                )
            )
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "İstatistikler",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DoziTurquoise)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 🔥 Streak Kartı
                    StreakCard(stats = uiState.stats)

                    // 💡 Öngörüler (Insights)
                    InsightsSection(stats = uiState.stats)

                    // 📊 Haftalık Özet
                    WeeklySummaryCard(weeklyData = uiState.weeklyLogs)

                    // 🏆 Başarımlar
                    AchievementsCard(achievements = uiState.achievements)

                    // 📈 Seri Durumu
                    ComplianceCard(stats = uiState.stats)
                }
            }
        }
    }
}

@Composable
private fun InsightsSection(stats: UserStats?) {
    val currentStreak = stats?.currentStreak ?: 0
    val longestStreak = stats?.longestStreak ?: 0
    val totalTaken = stats?.totalMedicationsTaken ?: 0

    // Streak ve düzenlilik odaklı insight'lar
    val insights = remember(stats) {
        mutableListOf<Triple<String, String, String>>().apply {
            // Seri analizi
            when {
                currentStreak >= 30 -> add(Triple(
                    "Muhteşem Seri!",
                    "$currentStreak gündür hatırlatmalarınızı kaçırmıyorsunuz. Harika!",
                    "INFO"
                ))
                currentStreak >= 7 -> add(Triple(
                    "Seri Başarısı!",
                    "$currentStreak gündür düzenli devam ediyorsunuz. Devam edin!",
                    "INFO"
                ))
                currentStreak >= 3 -> add(Triple(
                    "İyi Gidiyorsunuz",
                    "$currentStreak günlük seriniz var. Böyle devam!",
                    "INFO"
                ))
                currentStreak == 0 && totalTaken > 0 -> add(Triple(
                    "Yeni Seri Başlatın",
                    "Bugün hatırlatmalarınızı alarak yeni bir seri başlatın",
                    "WARNING"
                ))
            }

            // Rekor analizi
            if (longestStreak > currentStreak && longestStreak >= 7) {
                add(Triple(
                    "Rekorunuza Yaklaşın",
                    "En uzun seriniz $longestStreak gündü. Hedef bu!",
                    "INFO"
                ))
            }

            // Başlangıç teşviki
            if (totalTaken == 0) {
                add(Triple(
                    "Haydi Başlayalım!",
                    "İlk hatırlatmanızı alarak yolculuğunuza başlayın",
                    "INFO"
                ))
            }
        }
    }

    if (insights.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                DoziPurple.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💡", fontSize = 18.sp)
                    }
                    Text(
                        "Öngörüler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E)
                    )
                }

                Spacer(Modifier.height(4.dp))

                insights.forEach { (title, description, severity) ->
                    DoziInsightCard(
                        title = title,
                        description = description,
                        severity = severity,
                        recommendation = when (severity) {
                            "WARNING" -> "Hatırlatma ayarlarınızı kontrol edin"
                            "CRITICAL" -> "İlaç rutininizi gözden geçirin"
                            else -> null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakCard(stats: UserStats?) {
    val currentStreak = stats?.currentStreak ?: 0
    val longestStreak = stats?.longestStreak ?: 0
    val totalMedications = stats?.totalMedicationsTaken ?: 0
    val quickResponses = stats?.quickResponseCount ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DoziTurquoise.copy(alpha = 0.1f),
                            DoziPurple.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Flame Icon with glow effect
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    WarningOrange.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🔥",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp)
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Current Streak
                Text(
                    "$currentStreak",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (currentStreak > 0) DoziTurquoise else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Günlük Seri",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                if (currentStreak > 0) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = DoziTurquoise.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "Harika gidiyorsun! 🎉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DoziTurquoise,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(
                    color = DoziTurquoise.copy(alpha = 0.2f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(20.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Rekor Seri",
                        value = "$longestStreak",
                        icon = Icons.Default.EmojiEvents
                    )
                    StatItem(
                        label = "Hızlı Yanıt",
                        value = "$quickResponses",
                        icon = Icons.Default.Bolt
                    )
                    StatItem(
                        label = "Toplam",
                        value = "$totalMedications",
                        icon = Icons.Default.CheckCircle
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun WeeklySummaryCard(weeklyData: List<DayLog>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            DoziTurquoise.copy(alpha = 0.15f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📊", fontSize = 18.sp)
                }
                Text(
                    "Son 7 Gün",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A237E)
                )
            }
            Spacer(Modifier.height(8.dp))

            if (weeklyData.isEmpty()) {
                // Boş durum
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "📅",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Henüz veri yok",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "İlaçlarını almaya başla!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                weeklyData.forEach { day ->
                    WeeklyDayRow(day)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun WeeklyDayRow(day: DayLog) {
    val progress = day.progress
    val color = when {
        progress >= 0.9f -> SuccessGreen
        progress >= 0.5f -> WarningOrange
        else -> ErrorRed
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                day.dayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                day.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(100.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
            Text(
                "${day.taken}/${day.total}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun AchievementsCard(achievements: List<com.bardino.dozi.core.data.model.Achievement>) {
    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount = achievements.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                WarningOrange.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏆", fontSize = 18.sp)
                    }
                    Text(
                        "Başarımlar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E)
                    )
                }
                Surface(
                    color = DoziTurquoise.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "$unlockedCount/$totalCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DoziTurquoise,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (achievements.isEmpty()) {
                // Hiç başarım yoksa (yükleniyor veya hata)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = DoziTurquoise, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Başarımlar yükleniyor...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Kategorilere göre grupla
                val streakAchievements = achievements.filter {
                    it.type.name.startsWith("STREAK_") || it.type.name.startsWith("PERFECT_")
                }
                val firstStepAchievements = achievements.filter {
                    it.type.name.startsWith("FIRST_")
                }
                val reminderAchievements = achievements.filter {
                    it.type.name.startsWith("REMINDERS_")
                }
                val quickResponseAchievements = achievements.filter {
                    it.type.name.startsWith("QUICK_") || it.type.name.startsWith("SUPER_QUICK")
                }
                val socialAchievements = achievements.filter {
                    it.type.name in listOf("FAMILY_MEMBER", "CARING_BUDDY")
                }

                // 🔥 Streak & Düzenlilik Başarıları
                if (streakAchievements.isNotEmpty()) {
                    AchievementCategory("🔥 Düzenlilik", streakAchievements)
                    Spacer(Modifier.height(12.dp))
                }

                // 🏅 İlk Adımlar
                if (firstStepAchievements.isNotEmpty()) {
                    AchievementCategory("🏅 İlk Adımlar", firstStepAchievements)
                    Spacer(Modifier.height(12.dp))
                }

                // ⏰ Hatırlatma Kurulum
                if (reminderAchievements.isNotEmpty()) {
                    AchievementCategory("⏰ Hatırlatmalar", reminderAchievements)
                    Spacer(Modifier.height(12.dp))
                }

                // ⚡ Hızlı Yanıt
                if (quickResponseAchievements.isNotEmpty()) {
                    AchievementCategory("⚡ Hızlı Yanıt", quickResponseAchievements)
                    Spacer(Modifier.height(12.dp))
                }

                // 👨‍👩‍👧 Sosyal
                if (socialAchievements.isNotEmpty()) {
                    AchievementCategory("👨‍👩‍👧 Sosyal", socialAchievements)
                }

                if (unlockedCount == 0) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = DoziTurquoise.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "💪 Düzenli olmaya devam ederek başarım kazanabilirsin!",
                            style = MaterialTheme.typography.bodySmall,
                            color = DoziTurquoise,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCategory(
    title: String,
    achievements: List<com.bardino.dozi.core.data.model.Achievement>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        achievements.forEach { achievement ->
            AchievementItemWithProgress(achievement)
        }
    }
}

@Composable
private fun AchievementItemWithProgress(achievement: com.bardino.dozi.core.data.model.Achievement) {
    val progressPercentage = achievement.type.getProgressPercentage(achievement.progress)
    val color = try {
        Color(android.graphics.Color.parseColor(achievement.type.color))
    } catch (e: Exception) {
        DoziTurquoise
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (achievement.isUnlocked) color.copy(alpha = 0.1f) else Gray200.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji Icon
                Text(
                    achievement.type.emoji,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (achievement.isUnlocked) color.copy(alpha = 0.2f) else Gray200.copy(alpha = 0.5f))
                        .wrapContentSize(Alignment.Center),
                    fontSize = 24.sp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        achievement.type.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        achievement.type.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                if (achievement.isUnlocked) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Unlocked",
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Progress Bar
            if (!achievement.isUnlocked && achievement.target > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${achievement.progress} / ${achievement.target}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            "${progressPercentage.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            fontSize = 11.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = progressPercentage / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = Gray200.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(
    title: String,
    description: String,
    icon: String,
    isUnlocked: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isUnlocked) DoziTurquoise.copy(alpha = 0.1f) else Gray200.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) DoziTurquoise.copy(alpha = 0.2f) else Gray200.copy(alpha = 0.5f))
                    .padding(8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isUnlocked) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = SuccessGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ComplianceCard(stats: UserStats?) {
    val currentStreak = stats?.currentStreak ?: 0
    val longestStreak = stats?.longestStreak ?: 0

    val color = when {
        currentStreak >= 30 -> DoziTurquoise
        currentStreak >= 7 -> Color(0xFF00ACC1)
        currentStreak >= 3 -> WarningOrange
        else -> Color(0xFF78909C)
    }

    val encouragementText = when {
        currentStreak >= 30 -> "Muhteşem! 🌟"
        currentStreak >= 7 -> "Harika gidiyorsun! 💪"
        currentStreak >= 3 -> "Devam et! 🎯"
        currentStreak > 0 -> "Başlangıç güzel! 🚀"
        else -> "Haydi başla! 🌱"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            color.copy(alpha = 0.08f),
                            Color.White
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔥", fontSize = 18.sp)
                    }
                    Text(
                        "Seri Durumu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E)
                    )
                }
                Spacer(Modifier.height(24.dp))

                // Streak display
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    // Background circle
                    CircularProgressIndicator(
                        progress = { if (longestStreak > 0) (currentStreak.toFloat() / longestStreak).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxSize(),
                        color = color,
                        strokeWidth = 16.dp,
                        trackColor = color.copy(alpha = 0.12f)
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "$currentStreak",
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 48.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = color
                        )
                        Text(
                            "gün",
                            style = MaterialTheme.typography.bodyLarge,
                            color = color,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Surface(
                    color = color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        encouragementText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Rekor: $longestStreak gün",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = DoziTurquoise,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Günlük veri modeli
 */
@RequiresApi(Build.VERSION_CODES.O)
data class DayLog(
    val date: String,
    val dayName: String,
    val taken: Int,
    val total: Int
) {
    val progress: Float
        get() = if (total > 0) taken.toFloat() / total else 0f
}
