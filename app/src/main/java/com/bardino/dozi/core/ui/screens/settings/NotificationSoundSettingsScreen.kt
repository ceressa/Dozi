package com.bardino.dozi.core.ui.screens.settings

import android.media.RingtoneManager
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.components.PremiumBadge
import com.bardino.dozi.core.ui.components.PremiumGateDialog
import com.bardino.dozi.core.ui.theme.*

/**
 * 🎵 Bildirim Sesi Özelleştirme Ekranı (Premium Feature)
 */
@Composable
fun NotificationSoundSettingsScreen(
    isPremium: Boolean,
    currentSoundUri: String?,
    currentSoundName: String,
    onSoundSelected: (uri: String, name: String) -> Unit,
    onNavigateBack: () -> Unit,
    onUpgrade: () -> Unit
) {
    var showPremiumDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Sistem sesleri listesi
    val systemSounds = remember {
        listOf(
            "Notification 1" to "android.resource://com.android.providers.settings/notification1",
            "Notification 2" to "android.resource://com.android.providers.settings/notification2",
            "Notification 3" to "android.resource://com.android.providers.settings/notification3",
            "Gentle Bell" to "android.resource://com.android.providers.settings/gentle_bell",
            "Soft Chime" to "android.resource://com.android.providers.settings/soft_chime"
        )
    }

    // Premium değilse gate göster
    if (!isPremium && showPremiumDialog) {
        PremiumGateDialog(
            visible = true,
            featureName = "Özel Bildirim Sesi",
            featureDescription = "Dozi Ekstra ile kendi bildirim sesini seçebilir, uygulamayı kişiselleştirebilirsin",
            onDismiss = { showPremiumDialog = false },
            onUpgrade = onUpgrade
        )
    }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "Bildirim Sesi",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = if (isPremium) {
                    { PremiumBadge(size = 24.dp) }
                } else {
                    null
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPremium) DoziGold.copy(alpha = 0.1f) else Gray100
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isPremium) DoziGold else DoziPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Seçili Ses",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = currentSoundName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Ses listesi
            item {
                Text(
                    text = "Mevcut Sesler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Varsayılan ses
            item {
                SoundItem(
                    name = "Varsayılan",
                    isSelected = currentSoundName == "Varsayılan",
                    isPremium = isPremium,
                    onClick = {
                        if (isPremium) {
                            onSoundSelected("", "Varsayılan")
                        } else {
                            showPremiumDialog = true
                        }
                    }
                )
            }

            // Sistem sesleri
            items(systemSounds) { (name, uri) ->
                SoundItem(
                    name = name,
                    isSelected = currentSoundName == name,
                    isPremium = isPremium,
                    onClick = {
                        if (isPremium) {
                            onSoundSelected(uri, name)
                        } else {
                            showPremiumDialog = true
                        }
                    }
                )
            }

            // Premium upsell footer
            if (!isPremium) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpgrade() },
                        colors = CardDefaults.cardColors(
                            containerColor = DoziPrimary.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = DoziPrimary
                            )
                            Text(
                                text = "Özel sesleri kullanmak için Dozi Ekstra'ya geç",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = DoziPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundItem(
    name: String,
    isSelected: Boolean,
    isPremium: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                if (isPremium) DoziGold.copy(alpha = 0.15f) else DoziPrimary.copy(alpha = 0.15f)
            } else {
                Color.White
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, if (isPremium) DoziGold else DoziPrimary)
        } else null
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
                Icon(
                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isSelected) {
                        if (isPremium) DoziGold else DoziPrimary
                    } else {
                        Gray500
                    }
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TextPrimary else TextSecondary
                )
            }

            if (!isPremium && name != "Varsayılan") {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Premium",
                    tint = Gray400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
