package com.bardino.dozi.core.ui.screens.badi

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.bardino.dozi.core.data.model.*
import com.bardino.dozi.core.ui.theme.DoziPurple
import com.bardino.dozi.core.ui.theme.DoziTurquoise
import com.bardino.dozi.core.ui.viewmodel.BadiViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Badi Detay Ekranı
 * Bir badinin detaylarını, izinlerini ve ayarlarını gösterir
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadiDetailScreen(
    badiId: String,
    onNavigateBack: () -> Unit,
    viewModel: BadiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val badiWithUser = uiState.badis.firstOrNull { it.badi.id == badiId }

    var showRemoveDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text("Badi Detayları", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(DoziTurquoise, DoziPurple)
                    )
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        if (badiWithUser == null) {
            // Badi bulunamadı
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Badi bulunamadı",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onNavigateBack) {
                        Text("Geri Dön")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Badi Profil Kartı
                BadiProfileCard(badiWithUser)

                // Rol ve İzinler Kartı
                RoleAndPermissionsCard(
                    badi = badiWithUser.badi,
                    onEditRole = { showRoleDialog = true }
                )

                // Bildirim Tercihleri Kartı
                NotificationPreferencesCard(
                    preferences = badiWithUser.badi.notificationPreferences,
                    onUpdatePreferences = { prefs ->
                        viewModel.updateBadiNotificationPreferences(badiId, prefs)
                    }
                )

                // Tehlikeli İşlemler
                DangerZoneCard(
                    badiStatus = badiWithUser.badi.status,
                    onPauseBadi = {
                        viewModel.updateBadiStatus(badiId, BadiStatus.PAUSED)
                    },
                    onActivateBadi = {
                        viewModel.updateBadiStatus(badiId, BadiStatus.ACTIVE)
                    },
                    onRemoveBadi = { showRemoveDialog = true }
                )
            }

            // Rol Değiştirme Dialog
            if (showRoleDialog) {
                RoleSelectionDialog(
                    currentRole = badiWithUser.badi.permissions.role,
                    onDismiss = { showRoleDialog = false },
                    onRoleSelected = { role ->
                        viewModel.updateBadiRole(badiId, role)
                        showRoleDialog = false
                    }
                )
            }

            // Badi Kaldırma Dialog
            if (showRemoveDialog) {
                AlertDialog(
                    onDismissRequest = { showRemoveDialog = false },
                    title = { Text("Badi'yi Kaldır") },
                    text = {
                        Text(
                            "${badiWithUser.user.name} adlı badi'yi kaldırmak istediğinizden emin misiniz? " +
                                    "Bu işlem geri alınamaz."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.removeBadi(badiId)
                                showRemoveDialog = false
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Kaldır")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRemoveDialog = false }) {
                            Text("İptal")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BadiProfileCard(badiWithUser: BadiWithUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profil Resmi
            AsyncImage(
                model = badiWithUser.user.photoUrl ?: "https://via.placeholder.com/150",
                contentDescription = "Profil Resmi",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(DoziTurquoise.copy(alpha = 0.2f))
            )

            // İsim ve Nickname
            Text(
                text = badiWithUser.user.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (!badiWithUser.badi.nickname.isNullOrBlank()) {
                Text(
                    text = "\"${badiWithUser.badi.nickname}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // Email
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Text(
                    text = badiWithUser.user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Badi olma tarihi
            badiWithUser.badi.createdAt?.let { timestamp ->
                val date = timestamp.toDate()
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale("tr"))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "Badi olma tarihi: ${formatter.format(date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Durum Badge
            BadiStatusBadge(status = badiWithUser.badi.status)
        }
    }
}

@Composable
private fun BadiStatusBadge(status: BadiStatus) {
    val (text, color) = when (status) {
        BadiStatus.ACTIVE -> "Aktif" to Color(0xFF4CAF50)
        BadiStatus.PAUSED -> "Duraklatıldı" to Color(0xFFFF9800)
        BadiStatus.REMOVED -> "Kaldırıldı" to Color(0xFFF44336)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoleAndPermissionsCard(
    badi: Badi,
    onEditRole: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    text = "Rol ve İzinler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onEditRole) {
                    Icon(Icons.Default.Edit, "Rolü Düzenle", tint = DoziTurquoise)
                }
            }

            Divider()

            // Rol
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = badi.permissions.role.toTurkish(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DoziPurple
                    )
                    Text(
                        text = badi.permissions.role.toDescription(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Divider()

            // İzin Listesi
            Text(
                text = "İzinler:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )

            PermissionItem("Hatırlatmaları Görüntüleme", badi.permissions.canViewReminders)
            PermissionItem("Bildirim Alma", badi.permissions.canReceiveNotifications)
            PermissionItem("İlaç Aldı İşaretleme", badi.permissions.canMarkAsTaken)
            PermissionItem("Hatırlatmaları Düzenleme", badi.permissions.canEditReminders)
            PermissionItem("İlaç Ekleme", badi.permissions.canAddMedicine)
            PermissionItem("İlaç Silme", badi.permissions.canDeleteMedicine)
            PermissionItem("İlaç Geçmişini Görüntüleme", badi.permissions.canViewMedicationHistory)
            PermissionItem("Badileri Yönetme", badi.permissions.canManageBadis)
        }
    }
}

@Composable
private fun PermissionItem(permission: String, hasPermission: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (hasPermission) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.3f)
        )
        Text(
            text = permission,
            style = MaterialTheme.typography.bodyMedium,
            color = if (hasPermission) Color.Black else Color.Gray
        )
    }
}

@Composable
private fun NotificationPreferencesCard(
    preferences: BadiNotificationPreferences,
    onUpdatePreferences: (BadiNotificationPreferences) -> Unit
) {
    var currentPrefs by remember { mutableStateOf(preferences) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Bildirim Tercihleri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Divider()

            NotificationToggleItem(
                title = "İlaç Zamanı",
                description = "İlaç zamanı geldiğinde bildir",
                checked = currentPrefs.onMedicationTime,
                onCheckedChange = {
                    currentPrefs = currentPrefs.copy(onMedicationTime = it)
                    onUpdatePreferences(currentPrefs)
                }
            )

            NotificationToggleItem(
                title = "İlaç Alındı",
                description = "İlaç alındığında bildir",
                checked = currentPrefs.onMedicationTaken,
                onCheckedChange = {
                    currentPrefs = currentPrefs.copy(onMedicationTaken = it)
                    onUpdatePreferences(currentPrefs)
                }
            )

            NotificationToggleItem(
                title = "İlaç Atlandı",
                description = "İlaç atlandığında bildir",
                checked = currentPrefs.onMedicationSkipped,
                onCheckedChange = {
                    currentPrefs = currentPrefs.copy(onMedicationSkipped = it)
                    onUpdatePreferences(currentPrefs)
                }
            )

            NotificationToggleItem(
                title = "İlaç Kaçırıldı",
                description = "İlaç kaçırıldığında bildir",
                checked = currentPrefs.onMedicationMissed,
                onCheckedChange = {
                    currentPrefs = currentPrefs.copy(onMedicationMissed = it)
                    onUpdatePreferences(currentPrefs)
                }
            )
        }
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DoziTurquoise,
                checkedTrackColor = DoziTurquoise.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun DangerZoneCard(
    badiStatus: BadiStatus,
    onPauseBadi: () -> Unit,
    onActivateBadi: () -> Unit,
    onRemoveBadi: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
                Text(
                    text = "Tehlikeli İşlemler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
            }

            Divider(color = Color(0xFFFF9800).copy(alpha = 0.3f))

            when (badiStatus) {
                BadiStatus.ACTIVE -> {
                    OutlinedButton(
                        onClick = onPauseBadi,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF9800)
                        )
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Badi İlişkisini Duraklat")
                    }
                }
                BadiStatus.PAUSED -> {
                    Button(
                        onClick = onActivateBadi,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Badi İlişkisini Aktifleştir")
                    }
                }
                BadiStatus.REMOVED -> {
                    // Kaldırılmış badi için bir şey gösterme
                }
            }

            Button(
                onClick = onRemoveBadi,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.PersonRemove, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Badi İlişkisini Kaldır")
            }
        }
    }
}

@Composable
private fun RoleSelectionDialog(
    currentRole: BadiRole,
    onDismiss: () -> Unit,
    onRoleSelected: (BadiRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(currentRole) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Badi Rolünü Seç") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BadiRole.values().forEach { role ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRole = role }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = DoziTurquoise
                            )
                        )
                        Column {
                            Text(
                                text = role.toTurkish(),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = role.toDescription(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRoleSelected(selectedRole) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = DoziTurquoise
                )
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
