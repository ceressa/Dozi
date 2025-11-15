package com.bardino.dozi.core.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.data.model.PremiumAnalytics
import com.bardino.dozi.core.data.model.DailyAnalytics
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*

/**
 * 📊 Analytics Dashboard (Admin Only)
 *
 * Premium kullanıcı istatistikleri, retention metrikleri ve kullanım verileri
 *
 * NOT: Bu ekran sadece admin kullanıcıları için. Production'da
 * erişim kontrolü eklenmeli.
 */
@Composable
fun AnalyticsDashboardScreen(
    premiumAnalytics: PremiumAnalytics?,
    recentDailyAnalytics: List<DailyAnalytics>,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "Analytics Dashboard",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        isRefreshing = true
                        onRefresh()
                        isRefreshing = false
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            tint = if (isRefreshing) DoziPrimary else Gray600
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Premium Overview
            item {
                Text(
                    text = "Premium Özeti",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (premiumAnalytics != null) {
                item {
                    PremiumOverviewCard(premiumAnalytics)
                }

                item {
                    PremiumBreakdownCard(premiumAnalytics)
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Analytics verileri yükleniyor...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Daily Analytics
            item {
                Text(
                    text = "Günlük İstatistikler (Son 7 Gün)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(recentDailyAnalytics.take(7)) { daily ->
                DailyAnalyticsCard(daily)
            }

            // Footer note
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = InfoBlue.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = InfoBlue
                        )
                        Text(
                            text = "Bu veriler Firestore'daki analytics ve daily_analytics koleksiyonlarından gelir. Manuel olarak güncellenmelidir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumOverviewCard(analytics: PremiumAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Toplam kullanıcılar
            MetricRow(
                icon = Icons.Default.People,
                label = "Toplam Kullanıcı",
                value = analytics.totalUsers.toString(),
                color = DoziPrimary
            )

            Divider()

            // Premium kullanıcılar
            MetricRow(
                icon = Icons.Default.StarRate,
                label = "Premium Kullanıcı",
                value = analytics.premiumUsers.toString(),
                color = DoziGold
            )

            // Trial kullanıcılar
            MetricRow(
                icon = Icons.Default.CardGiftcard,
                label = "Trial Kullanıcı",
                value = analytics.trialUsers.toString(),
                color = DoziPrimary
            )

            Divider()

            // Conversion rate
            MetricRow(
                icon = Icons.Default.TrendingUp,
                label = "Dönüşüm Oranı",
                value = String.format("%.1f%%", analytics.conversionRate * 100),
                color = SuccessGreen
            )

            // Revenue
            MetricRow(
                icon = Icons.Default.AttachMoney,
                label = "Toplam Gelir",
                value = "₺${String.format("%.2f", analytics.totalRevenue)}",
                color = DoziGold
            )
        }
    }
}

@Composable
private fun PremiumBreakdownCard(analytics: PremiumAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Plan Dağılımı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            PlanBreakdownRow("Haftalık", analytics.weeklyUsers, DoziRose)
            PlanBreakdownRow("Aylık", analytics.monthlyUsers, DoziPrimary)
            PlanBreakdownRow("Yıllık", analytics.yearlyUsers, DoziGold)
            PlanBreakdownRow("Ömür Boyu", analytics.lifetimeUsers, DoziGold)
        }
    }
}

@Composable
private fun DailyAnalyticsCard(daily: DailyAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = daily.date,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SmallMetric("Aktif", daily.activeUsers.toString(), DoziPrimary)
                SmallMetric("Yeni", daily.newSignups.toString(), SuccessGreen)
                SmallMetric("Premium", daily.premiumPurchases.toString(), DoziGold)
                SmallMetric("Trial", daily.trialStarts.toString(), DoziRose)
            }

            Divider()

            Text(
                text = "Retention: 1d ${(daily.retention1Day * 100).toInt()}% | 7d ${(daily.retention7Day * 100).toInt()}% | 30d ${(daily.retention30Day * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun MetricRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun PlanBreakdownRow(
    planName: String,
    count: Int,
    color: Color
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
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, shape = RoundedCornerShape(3.dp))
            )
            Text(
                text = planName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun SmallMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
