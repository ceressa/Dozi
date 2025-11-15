package com.bardino.dozi.core.ui.screens.badi

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bardino.dozi.core.data.model.*
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import com.bardino.dozi.core.ui.viewmodel.BadiViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadiPermissionsScreen(
    badiId: String,
    onNavigateBack: () -> Unit
) {
    val viewModel: BadiViewModel = hiltViewModel()
    val badis by viewModel.badis.collectAsState()

    val currentBadi = badis.find { it.badi.id == badiId }

    if (currentBadi == null) {
        // Badi bulunamadı
        Scaffold(
            topBar = {
                DoziTopBar(
                    title = "Badi İzinleri",
                    canNavigateBack = true,
                    onNavigateBack = onNavigateBack
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Badi bulunamadı")
            }
        }
        return
    }

    var permissions by remember { mutableStateOf(currentBadi.badi.permissions) }
    var hasChanges by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            "${currentBadi.user.name} İzinleri",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri", tint = Color.White)
                    }
                },
                actions = {
                    if (hasChanges) {
                        TextButton(onClick = {
                            viewModel.updateBadiPermissions(badiId, permissions)
                            hasChanges = false
                            onNavigateBack()
                        }) {
                            Text("Kaydet", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(DoziTurquoise, DoziPurple)
                    )
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color(0xFFF5F7FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Badi Info Card
            BadiInfoCard(currentBadi.user)

            // Rol Seçimi
            RoleSelectionCard(
                currentRole = permissions.role,
                onRoleChange = { newRole ->
                    permissions = BadiPermissions.fromRole(newRole)
                    hasChanges = true
                }
            )

            // Detaylı İzinler
            Text(
                text = "Detaylı İzinler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            PermissionCard(
                icon = Icons.Default.Visibility,
                title = "Hatırlatmaları Görüntüleme",
                description = "Badin ilaç hatırlatmalarını görebilir",
                isEnabled = permissions.canViewReminders,
                onToggle = {
                    permissions = permissions.copy(canViewReminders = it)
                    hasChanges = true
                },
                color = DoziBlue
            )

            PermissionCard(
                icon = Icons.Default.Notifications,
                title = "Bildirim Alma",
                description = "Badin hatırlatma bildirimleri alır",
                isEnabled = permissions.canReceiveNotifications,
                onToggle = {
                    permissions = permissions.copy(canReceiveNotifications = it)
                    hasChanges = true
                },
                color = DoziTurquoise
            )

            PermissionCard(
                icon = Icons.Default.CheckCircle,
                title = "İlaç Alındı İşaretleme",
                description = "Badin senin adına ilaç alındı işaretleyebilir",
                isEnabled = permissions.canMarkAsTaken,
                onToggle = {
                    permissions = permissions.copy(canMarkAsTaken = it)
                    hasChanges = true
                },
                color = Color(0xFF4CAF50)
            )

            PermissionCard(
                icon = Icons.Default.Edit,
                title = "Hatırlatma Düzenleme",
                description = "Badin ilaç hatırlatmalarını düzenleyebilir",
                isEnabled = permissions.canEditReminders,
                onToggle = {
                    permissions = permissions.copy(canEditReminders = it)
                    hasChanges = true
                },
                color = Color(0xFFFF9800)
            )

            PermissionCard(
                icon = Icons.Default.Add,
                title = "İlaç Ekleme",
                description = "Badin yeni ilaç ekleyebilir",
                isEnabled = permissions.canAddMedicine,
                onToggle = {
                    permissions = permissions.copy(canAddMedicine = it)
                    hasChanges = true
                },
                color = Color(0xFF9C27B0)
            )

            PermissionCard(
                icon = Icons.Default.Delete,
                title = "İlaç Silme",
                description = "Badin ilaçları silebilir",
                isEnabled = permissions.canDeleteMedicine,
                onToggle = {
                    permissions = permissions.copy(canDeleteMedicine = it)
                    hasChanges = true
                },
                color = DoziCoralDark,
                isWarning = true
            )

            PermissionCard(
                icon = Icons.Default.History,
                title = "İlaç Geçmişini Görüntüleme",
                description = "Badin ilaç alım geçmişini görebilir",
                isEnabled = permissions.canViewMedicationHistory,
                onToggle = {
                    permissions = permissions.copy(canViewMedicationHistory = it)
                    hasChanges = true
                },
                color = DoziBlue
            )

            PermissionCard(
                icon = Icons.Default.People,
                title = "Badi Yönetimi",
                description = "Badin diğer badileri yönetebilir",
                isEnabled = permissions.canManageBadis,
                onToggle = {
                    permissions = permissions.copy(canManageBadis = it)
                    hasChanges = true
                },
                color = Color(0xFFE91E63),
                isWarning = true
            )

            // Bottom padding for fab
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun BadiInfoCard(user: User) {
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            DoziTurquoise.copy(alpha = 0.08f),
                            DoziPurple.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar with gradient border
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(DoziTurquoise, DoziPurple)
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(DoziTurquoise.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = DoziTurquoise,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleSelectionCard(
    currentRole: BadiRole,
    onRoleChange: (BadiRole) -> Unit
) {
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(DoziTurquoise, DoziPurple)
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Hızlı Rol Seçimi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                text = "Önceden tanımlı rolleri kullanarak hızlıca izin ayarlayabilirsiniz",
                style = MaterialTheme.typography.bodySmall,

                color = MaterialTheme.colorScheme.onSurfaceVariant,

                color = MaterialTheme.colorScheme.onSurfaceVariant

                fontSize = 13.sp
            )

            BadiRole.values().forEach { role ->
                RoleOption(
                    role = role,
                    isSelected = currentRole == role,
                    onClick = { onRoleChange(role) }
                )
            }
        }
    }
}

@Composable
private fun RoleOption(
    role: BadiRole,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) DoziTurquoise.copy(alpha = 0.1f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = DoziTurquoise)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = role.toTurkish(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) DoziTurquoise else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = role.toDescription(),
                style = MaterialTheme.typography.bodySmall,

                color = MaterialTheme.colorScheme.onSurfaceVariant,

                color = MaterialTheme.colorScheme.onSurfaceVariant

                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    color: Color,
    isWarning: Boolean = false
) {
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }

        // Text
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,

                    color = MaterialTheme.colorScheme.onSurface
                 color = MaterialTheme.colorScheme.onSurface
                  
                    fontSize = 15.sp
                )
                if (isWarning) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,

                color = MaterialTheme.colorScheme.onSurfaceVariant,

                color = MaterialTheme.colorScheme.onSurfaceVariant

                fontSize = 12.sp
            )
        }

            // Toggle
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = color,
                    checkedTrackColor = color.copy(alpha = 0.5f)
                )
            )
        }
    }
}
