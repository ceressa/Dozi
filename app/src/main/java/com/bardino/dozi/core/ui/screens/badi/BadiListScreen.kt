package com.bardino.dozi.core.ui.screens.badi

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.bardino.dozi.core.data.model.BadiWithUser
import com.bardino.dozi.core.data.model.BadiRequestWithUser
import com.bardino.dozi.core.ui.viewmodel.BadiViewModel
import coil.compose.AsyncImage

/**
 * Badi Listesi Ekranı
 * Kullanıcının badilerini ve bekleyen isteklerini gösterir
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadiListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddBadi: () -> Unit,
    onNavigateToBadiDetail: (String) -> Unit,
    viewModel: BadiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Log UI state changes
    LaunchedEffect(uiState.pendingRequests.size, uiState.badis.size) {
        android.util.Log.d("BadiListScreen", "UI State - Pending requests: ${uiState.pendingRequests.size}, Badis: ${uiState.badis.size}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🤝 Badilerim") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddBuddy) {
                        Icon(Icons.Default.PersonAdd, "Badi Ekle")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddBuddy,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.PersonAdd, "Badi Ekle")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Bekleyen istekler
            if (uiState.pendingRequests.isNotEmpty()) {
                PendingRequestsSection(
                    requests = uiState.pendingRequests,
                    onAccept = { viewModel.acceptBadiRequest(it) },
                    onReject = { viewModel.rejectBadiRequest(it) }
                )
                Divider()
            }

            // Badi listesi
            if (uiState.buddies.isEmpty()) {
                EmptyBuddyState(onAddBuddy = onNavigateToAddBuddy)
            } else {
                BuddyList(
                    buddies = uiState.buddies,
                    onBadiClick = { badi ->
                        android.util.Log.d("BadiListScreen", "Badi clicked: id=${buddy.badi.id}, userId=${buddy.badi.userId}, buddyUserId=${buddy.badi.buddyUserId}, userName=${badi.user.name}")
                        onNavigateToBadiDetail(buddy.badi.id)
                    }
                )
            }

            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Tamam")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun PendingRequestsSection(
    requests: List<BuddyRequestWithUser>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Text(
            "📬 Bekleyen İstekler (${requests.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        requests.forEach { request ->
            BuddyRequestCard(
                request = request,
                onAccept = { onAccept(request.request.id) },
                onReject = { onReject(request.request.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun BuddyRequestCard(
    request: BuddyRequestWithUser,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profil fotoğrafı
            AsyncImage(
                model = request.fromUser.photoUrl,
                contentDescription = "Profil",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Kullanıcı bilgisi
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.fromUser.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    request.fromUser.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                request.request.message?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Butonlar
            Column {
                IconButton(
                    onClick = onAccept,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        "Kabul Et",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = onReject,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Reddet",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun BuddyList(
    buddies: List<BuddyWithUser>,
    onBadiClick: (BuddyWithUser) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(buddies, key = { it.badi.id }) { badi ->
            BuddyCard(
                badi = badi,
                onClick = { onBadiClickabadi) }
            )
        }
    }
}

@Composable
fun BuddyCard(
    buddy: BadiWithUser,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profil fotoğrafı
            AsyncImage(
                model = badi.user.photoUrl,
                contentDescription = "Profil",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Kullanıcı bilgisi
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    buddy.badi.nickname ?: badi.user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    badi.user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                // DEBUG: Match kontrolü
                Text(
                    "🔍 MATCH: ${badi.user.uid == buddy.badi.buddyUserId} | uid:...${badi.user.uid.takeLast(4)} vs buddyId:...${buddy.badi.buddyUserId.takeLast(4)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )

                // İzin durumu
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (badi.badi.permissions.canViewReminders) {
                        Chip(text = "📋 Hatırlatmalar")
                    }
                    if (badi.badi.notificationPreferences.onMedicationTime) {
                        Chip(text = "🔔 Bildirimler")
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                "Detay",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun EmptyBuddyState(onAddBuddy: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "🤝",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                "Henüz badiniz yok",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Sevdiklerinizi badi olarak ekleyin\nilaç takibinizi birlikte yönetin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAddBuddy) {
                Icon(Icons.Default.PersonAdd, "Ekle")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Badi Ekle")
            }
        }
    }
}
