package com.bardino.dozi.core.ui.screens.profiles

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bardino.dozi.core.data.model.ProfileStats
import com.bardino.dozi.core.ui.viewmodel.ProfileViewModel

/**
 * Profile Dashboard Screen
 * Shows statistics for all profiles
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDashboardScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load stats when screen opens
    LaunchedEffect(uiState.profiles) {
        if (uiState.profiles.isNotEmpty() && uiState.profileStats.isEmpty()) {
            viewModel.loadAllProfilesStats()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil İstatistikleri") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAllProfilesStats() }) {
                        Icon(Icons.Default.Refresh, "Yenile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (uiState.isLoadingStats) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview card
                item {
                    OverviewCard(
                        profiles = uiState.profiles,
                        stats = uiState.profileStats
                    )
                }

                // Individual profile stats
                items(uiState.profiles) { profile ->
                    val stats = uiState.profileStats[profile.id]
                    if (stats != null) {
                        ProfileStatsCard(
                            stats = stats,
                            profile = profile,
                            onClick = { onNavigateToProfile(profile.id) }
                        )
                    }
                }

                // Empty state
                if (uiState.profiles.isEmpty()) {
                    item {
                        EmptyStatsState()
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewCard(
    profiles: List<com.bardino.dozi.core.data.local.entity.ProfileEntity>,
    stats: Map<String, ProfileStats>
) {
    val totalMedicines = stats.values.sumOf { it.totalMedicines }
    val totalTakenToday = stats.values.sumOf { it.takenToday }
    val totalTodayMedicines = stats.values.sumOf { it.todayMedicines }
    val overallCompliance = if (totalTodayMedicines > 0) {
        (totalTakenToday.toFloat() / totalTodayMedicines.toFloat()) * 100f
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Genel Durum",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Profil",
                    value = "${profiles.size}",
                    icon = Icons.Default.Person
                )
                StatItem(
                    label = "Toplam İlaç",
                    value = "$totalMedicines",
                    icon = Icons.Default.Medication
                )
                StatItem(
                    label = "Bugün Alınan",
                    value = "$totalTakenToday/$totalTodayMedicines",
                    icon = Icons.Default.CheckCircle
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Overall compliance
            ComplianceBar(
                label = "Genel Uyum",
                compliance = overallCompliance
            )
        }
    }
}

@Composable
fun ProfileStatsCard(
    stats: ProfileStats,
    profile: com.bardino.dozi.core.data.local.entity.ProfileEntity,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(profile.color))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.avatarIcon,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stats.profileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${stats.totalMedicines} ilaç",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Today's stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStatItem(
                    label = "Bugün",
                    value = "${stats.todayMedicines}",
                    color = MaterialTheme.colorScheme.primary
                )
                MiniStatItem(
                    label = "Alındı",
                    value = "${stats.takenToday}",
                    color = MaterialTheme.colorScheme.tertiary
                )
                MiniStatItem(
                    label = "Atlandı",
                    value = "${stats.missedToday}",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compliance
            ComplianceBar(
                label = "Bugünün Uyumu",
                compliance = stats.complianceRate
            )

            // Last 7 days chart
            if (stats.last7DaysCompliance.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Son 7 Gün",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                ComplianceChart(stats.last7DaysCompliance)
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun MiniStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ComplianceBar(
    label: String,
    compliance: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = compliance / 100f,
        label = "compliance"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${compliance.toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    compliance >= 80 -> Color(0xFF4CAF50)
                    compliance >= 50 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when {
                compliance >= 80 -> Color(0xFF4CAF50)
                compliance >= 50 -> Color(0xFFFFC107)
                else -> Color(0xFFF44336)
            }
        )
    }
}

@Composable
fun ComplianceChart(days: List<com.bardino.dozi.core.data.model.DayCompliance>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                val barHeight = if (day.total > 0) (day.rate / 100f) else 0f
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height((60 * barHeight).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            when {
                                day.rate >= 80 -> Color(0xFF4CAF50)
                                day.rate >= 50 -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            }
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    day.date,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyStatsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.BarChart,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Henüz istatistik yok",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Profil oluşturup ilaç ekleyerek başlayın",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
