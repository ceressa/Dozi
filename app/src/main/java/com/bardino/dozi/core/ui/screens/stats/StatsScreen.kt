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

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "İstatistikler",
                canNavigateBack = false,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DoziCharacterLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(contentPadding)
        ) {
            if (uiState.isLoading) {
                // Yükleniyor göstergesi
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DoziRed)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 24.dp),
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

                    // 📈 Uyumluluk Oranı
                    ComplianceCard(stats = uiState.stats)
                }
            }
        }
    }
}

@Composable
private fun InsightsSection(stats: UserStats?) {
    val complianceRate = stats?.complianceRate ?: 0f
    val currentStreak = stats?.currentStreak ?: 0

    // Basit insight'lar oluştur (gerçek uygulamada GenerateInsightsUseCase kullanılır)
    val insights = remember(stats) {
        mutableListOf<Triple<String, String, String>>().apply {
            // Trend analizi
            when {
                complianceRate >= 90f -> add(Triple(
                    "Mükemmel Performans!",
                    "%${complianceRate.toInt()} uyumluluk oranı ile harikasınız",
                    "INFO"
                ))
                complianceRate >= 70f -> add(Triple(
                    "İyi İlerleme",
                    "Uyumluluğunuz %${complianceRate.toInt()}. Biraz daha dikkat ile mükemmel olabilirsiniz",
                    "INFO"
                ))
                complianceRate >= 50f -> add(Triple(
                    "Dikkat Gerekli",
                    "Uyumluluğunuz %${complianceRate.toInt()}. Hatırlatma saatlerinizi gözden geçirin",
                    "WARNING"
                ))
                complianceRate > 0f -> add(Triple(
                    "Uyumluluk Düşük",
                    "Uyumluluğunuz %${complianceRate.toInt()}. İlaçlarınızı almayı unutmayın",
                    "CRITICAL"
                ))
            }

            // Seri analizi
            if (currentStreak >= 7) {
                add(Triple(
                    "Seri Başarısı!",
                    "$currentStreak gündür hiç aksatmadınız. Harika!",
                    "INFO"
                ))
            } else if (currentStreak == 0 && complianceRate > 0) {
                add(Triple(
                    "Seriyi Başlatın",
                    "Bugün ilaçlarınızı alarak yeni bir seri başlatın",
                    "WARNING"
                ))
            }
        }
    }

    if (insights.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "💡 Öngörüler",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DoziRed.copy(alpha = 0.08f),
                            WarningOrange.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Flame Icon
                Text(
                    "🔥",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp)
                )
                Spacer(Modifier.height(8.dp))

                // Current Streak
                Text(
                    "$currentStreak",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (currentStreak > 0) DoziRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Günlük Seri",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                if (currentStreak > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Harika gidiyorsun! 🎉",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DoziTurquoise,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(
                    color = Gray200,
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
                        label = "En Uzun Seri",
                        value = "$longestStreak",
                        icon = Icons.Default.TrendingUp
                    )
                    StatItem(
                        label = "Toplam Doz",
                        value = "$totalMedications",
                        icon = Icons.Default.Medication
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
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
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
                Text(
                    "📊 Son 7 Gün",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
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
                Text(
                    "🏆 Başarımlar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "$unlockedCount/$totalCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DoziTurquoise
                )
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
                    it.type.name.startsWith("STREAK_")
                }
                val perfectAchievements = achievements.filter {
                    it.type.name.startsWith("PERFECT_")
                }
                val firstStepAchievements = achievements.filter {
                    it.type.name.startsWith("FIRST_")
                }
                val collectorAchievements = achievements.filter {
                    it.type.name.startsWith("MEDICINE_COLLECTOR")
                }
                val doseAchievements = achievements.filter {
                    it.type.name.startsWith("TOTAL_DOSES")
                }

                // 🔥 Streak Başarıları
                if (streakAchievements.isNotEmpty()) {
                    AchievementCategory("🔥 Streak Başarıları", streakAchievements)
                    Spacer(Modifier.height(8.dp))
                }

                // 🎯 Mükemmel Uyum
                if (perfectAchievements.isNotEmpty()) {
                    AchievementCategory("🎯 Mükemmel Uyum", perfectAchievements)
                    Spacer(Modifier.height(8.dp))
                }

                // 🏅 İlk Adımlar
                if (firstStepAchievements.isNotEmpty()) {
                    AchievementCategory("🏅 İlk Adımlar", firstStepAchievements)
                    Spacer(Modifier.height(8.dp))
                }

                // 📚 Koleksiyoncu
                if (collectorAchievements.isNotEmpty()) {
                    AchievementCategory("📚 Koleksiyoncu", collectorAchievements)
                    Spacer(Modifier.height(8.dp))
                }

                // 💯 Toplam Doz
                if (doseAchievements.isNotEmpty()) {
                    AchievementCategory("💯 Toplam Doz", doseAchievements)
                }

                if (unlockedCount == 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "💪 Düzenli ilaç kullanımını sürdürerek başarım kazanabilirsin!",
                        style = MaterialTheme.typography.bodySmall,
                        color = DoziTurquoise,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
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
    val complianceRate = stats?.complianceRate ?: 0f
    val color = when {
        complianceRate >= 90f -> SuccessGreen
        complianceRate >= 70f -> WarningOrange
        else -> ErrorRed
    }

    val encouragementText = when {
        complianceRate >= 90f -> "Mükemmel! 🌟"
        complianceRate >= 70f -> "İyi gidiyorsun! 💪"
        complianceRate >= 50f -> "Devam et! 🎯"
        complianceRate > 0f -> "Başlangıç güzel! 🚀"
        else -> "Haydi başla! 🌱"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "📈 Uyumluluk Oranı",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(24.dp))

            // Circular Progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                CircularProgressIndicator(
                    progress = { complianceRate / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    strokeWidth = 14.dp,
                    trackColor = color.copy(alpha = 0.15f)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${complianceRate.toInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                encouragementText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Son 30 günlük ortalama",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
