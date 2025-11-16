package com.bardino.dozi.core.ui.screens.profiles

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.bardino.dozi.core.data.local.entity.ProfileEntity
import com.bardino.dozi.core.ui.viewmodel.ProfileViewModel

/**
 * Helper function to get display name for profile
 * Shows "Aile Üyesi" instead of "Varsayılan Profil" for better UX
 */
private fun getProfileDisplayName(profile: ProfileEntity): String {
    return when (profile.name) {
        "Varsayılan Profil", "default-profile" -> "Aile Üyesi"
        else -> profile.name
    }
}

/**
 * Profile Management Screen
 * Allows users to create, edit, switch, and delete profiles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<ProfileEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<ProfileEntity?>(null) }
    var showPinDialog by remember { mutableStateOf<ProfileEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aile Üyesi Yönetimi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (uiState.canAddMoreProfiles || uiState.profiles.isEmpty()) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Yeni Aile Üyesi")
                }
            } else {
                FloatingActionButton(
                    onClick = onNavigateToPremium,
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(Icons.Default.Star, "Premium")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Premium info banner
            if (!uiState.isPremium && uiState.profiles.size >= 1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNavigateToPremium() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Birden fazla profil için Premium",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Ailenizin tüm üyelerinin ilaçlarını takip edin",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Profiles list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.profiles) { profile ->
                        ProfileCard(
                            profile = profile,
                            isActive = profile.id == uiState.activeProfile?.id,
                            onSwitch = { viewModel.switchToProfile(profile.id) },
                            onEdit = { showEditDialog = profile },
                            onDelete = { showDeleteDialog = profile }
                        )
                    }

                    // Empty state
                    if (uiState.profiles.isEmpty()) {
                        item {
                            EmptyProfilesState()
                        }
                    }
                }
            }
        }

        // Create profile dialog
        if (showCreateDialog) {
            CreateProfileDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, avatar, color ->
                    viewModel.createProfile(
                        name = name,
                        avatarIcon = avatar,
                        color = color,
                        setAsActive = uiState.profiles.isEmpty() // Set as active if first profile
                    )
                    showCreateDialog = false
                },
                isPremium = uiState.isPremium
            )
        }

        // Edit profile dialog
        showEditDialog?.let { profile ->
            EditProfileDialog(
                profile = profile,
                onDismiss = { showEditDialog = null },
                onConfirm = { name, avatar, color ->
                    viewModel.updateProfileName(profile.id, name)
                    viewModel.updateProfileAvatar(profile.id, avatar)
                    viewModel.updateProfileColor(profile.id, color)
                    showEditDialog = null
                },
                onSetPin = {
                    showEditDialog = null
                    showPinDialog = profile
                },
                onRemovePin = {
                    viewModel.removeProfilePin(profile.id)
                    showEditDialog = null
                }
            )
        }

        // PIN dialog
        showPinDialog?.let { profile ->
            SetPinDialog(
                onDismiss = { showPinDialog = null },
                onConfirm = { pin ->
                    viewModel.setProfilePin(profile.id, pin)
                    showPinDialog = null
                }
            )
        }

        // Delete confirmation dialog
        showDeleteDialog?.let { profile ->
            DeleteProfileDialog(
                profile = profile,
                onDismiss = { showDeleteDialog = null },
                onConfirm = {
                    viewModel.deleteProfile(profile.id)
                    showDeleteDialog = null
                }
            )
        }
    }
}

@Composable
fun ProfileCard(
    profile: ProfileEntity,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isActive) { onSwitch() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(profile.color))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.avatarIcon,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Profile info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = getProfileDisplayName(profile),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    // PIN indicator
                    if (profile.pinCode != null) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "PIN korumalı",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (isActive) {
                    Text(
                        text = "Aktif",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (profile.pinCode != null) {
                    Text(
                        text = "PIN korumalı",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Düzenle")
                }
                if (!isActive) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Sil")
                    }
                }
                if (isActive) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Aktif",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyProfilesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Henüz profil yok",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Başlamak için yeni bir profil oluşturun",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
