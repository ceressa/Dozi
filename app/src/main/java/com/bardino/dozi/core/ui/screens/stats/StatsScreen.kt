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
import com.bardino.dozi.core.data.model.Achievements
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: StatsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "İstatistikler",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 🔥 Streak Kartı
            StreakCard(stats = uiState.stats)

            // 📊 Haftalık Özet
            WeeklySummaryCard(weeklyData = uiState.weeklyLogs)

            // 🏆 Başarımlar
            AchievementsCard(stats = uiState.stats)

            // 📈 Uyumluluk Oranı
            ComplianceCard(stats = uiState.stats)
        }
    }
}

@Composable
private fun StreakCard(stats: UserStats?) {
    val currentStreak = stats?.currentStreak ?: 0
    val longestStreak = stats?.longestStreak ?: 0

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
                    Brush.horizontalGradient(
                        listOf(DoziRed.copy(alpha = 0.1f), WarningOrange.copy(alpha = 0.1f))
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "🔥",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$currentStreak",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = DoziRed
                )
                Text(
                    "Günlük Seri",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Gray200)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("En Uzun Seri", "$longestStreak gün", Icons.Default.TrendingUp)
                    StatItem("Toplam İlaç", "${stats?.totalMedicationsTaken ?: 0}", Icons.Default.Medication)
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
            Text(
                "Son 7 Gün",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))

            weeklyData.forEach { day ->
                WeeklyDayRow(day)
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
private fun AchievementsCard(stats: UserStats?) {
    val unlockedAchievements = stats?.achievements ?: emptyList()

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
                "🏆 Başarımlar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (unlockedAchievements.isEmpty()) {
                Text(
                    "Henüz başarım kazanmadın. Düzenli ilaç kullanımını sürdür!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            } else {
                Achievements.ALL.forEach { achievement ->
                    val isUnlocked = achievement.id in unlockedAchievements
                    AchievementItem(
                        title = achievement.title,
                        description = achievement.description,
                        icon = achievement.icon,
                        isUnlocked = isUnlocked
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Uyumluluk Oranı",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                CircularProgressIndicator(
                    progress = { complianceRate / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    strokeWidth = 12.dp,
                    trackColor = color.copy(alpha = 0.2f)
                )
                Text(
                    "${complianceRate.toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Son 30 günlük ortalama",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = DoziTurquoise,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
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
