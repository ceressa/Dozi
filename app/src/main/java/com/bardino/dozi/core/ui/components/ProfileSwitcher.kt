package com.bardino.dozi.core.ui.components

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
 * Quick profile switcher component for Home screen
 * Shows active profile as a chip and allows switching profiles via bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcher(
    modifier: Modifier = Modifier,
    onNavigateToProfileManagement: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProfile = uiState.activeProfile
    var showBottomSheet by remember { mutableStateOf(false) }

    // Active profile chip
    if (activeProfile != null) {
        Card(
            modifier = modifier.clickable { showBottomSheet = true },
            colors = CardDefaults.cardColors(
                containerColor = Color(android.graphics.Color.parseColor(activeProfile.color)).copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(activeProfile.color))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeProfile.avatarIcon,
                        fontSize = 18.sp
                    )
                }

                // Profile name
                Text(
                    text = activeProfile.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )

                // Icon
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Profil değiştir",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Bottom sheet for profile selection
    if (showBottomSheet) {
        ProfileSwitcherBottomSheet(
            profiles = uiState.profiles,
            activeProfileId = activeProfile?.id,
            onDismiss = { showBottomSheet = false },
            onProfileSelect = { profileId ->
                viewModel.switchToProfile(profileId)
                showBottomSheet = false
            },
            onManageProfiles = {
                onNavigateToProfileManagement()
                showBottomSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherBottomSheet(
    profiles: List<ProfileEntity>,
    activeProfileId: String?,
    onDismiss: () -> Unit,
    onProfileSelect: (String) -> Unit,
    onManageProfiles: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Profil Seç",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${profiles.size} profil",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onManageProfiles) {
                    Icon(Icons.Default.Settings, "Profil yönetimi")
                }
            }

            Divider()

            // Profiles list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles) { profile ->
                    ProfileItem(
                        profile = profile,
                        isActive = profile.id == activeProfileId,
                        onClick = { onProfileSelect(profile.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileItem(
    profile: ProfileEntity,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isActive, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isActive) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
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

            // Profile info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (isActive) {
                    Text(
                        "Aktif profil",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Check icon for active profile
            if (isActive) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Aktif",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
