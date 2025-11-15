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
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text("Badilerim", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddBadi) {
                        Icon(Icons.Default.PersonAdd, "Badi Ekle", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(com.bardino.dozi.core.ui.theme.DoziTurquoise, com.bardino.dozi.core.ui.theme.DoziPurple)
                    )
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color(0xFFF5F7FA),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddBadi,
                containerColor = com.bardino.dozi.core.ui.theme.DoziTurquoise
            ) {
                Icon(Icons.Default.PersonAdd, "Badi Ekle", tint = Color.White)
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
            if (uiState.badis.isEmpty()) {
                EmptyBadiState(onAddBadi = onNavigateToAddBadi)
            } else {
                BadiList(
                    badis = uiState.badis,
                    onBadiClick = { badi ->
                        android.util.Log.d("BadiListScreen", "Badi clicked: id=${badi.badi.id}, userId=${badi.badi.userId}, buddyUserId=${badi.badi.buddyUserId}, userName=${badi.user.name}")
                        onNavigateToBadiDetail(badi.badi.id)
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
    requests: List<BadiRequestWithUser>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        com.bardino.dozi.core.ui.theme.WarningOrange.copy(alpha = 0.1f),
                        com.bardino.dozi.core.ui.theme.DoziTurquoise.copy(alpha = 0.08f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(com.bardino.dozi.core.ui.theme.WarningOrange, com.bardino.dozi.core.ui.theme.DoziTurquoise)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📬",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                "Bekleyen İstekler (${requests.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = com.bardino.dozi.core.ui.theme.TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        requests.forEach { request ->
            BadiRequestCard(
                request = request,
                onAccept = { onAccept(request.request.id) },
                onReject = { onReject(request.request.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun BadiRequestCard(
    request: BadiRequestWithUser,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            com.bardino.dozi.core.ui.theme.WarningOrange.copy(alpha = 0.08f),
                            com.bardino.dozi.core.ui.theme.DoziTurquoise.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profil fotoğrafı
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(com.bardino.dozi.core.ui.theme.WarningOrange, com.bardino.dozi.core.ui.theme.DoziTurquoise)
                            ),
                            shape = CircleShape
                        )
                        .padding(2.dp)
                ) {
                    AsyncImage(
                        model = request.fromUser.photoUrl,
                        contentDescription = "Profil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Kullanıcı bilgisi
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.fromUser.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = com.bardino.dozi.core.ui.theme.TextPrimary
                    )
                    Text(
                        request.fromUser.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = com.bardino.dozi.core.ui.theme.TextSecondary
                    )
                    request.request.message?.let { message ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "\"$message\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.bardino.dozi.core.ui.theme.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Butonlar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(com.bardino.dozi.core.ui.theme.SuccessGreen, com.bardino.dozi.core.ui.theme.DoziTurquoise)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(onClick = onAccept),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            "Kabul Et",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(com.bardino.dozi.core.ui.theme.ErrorRed, com.bardino.dozi.core.ui.theme.WarningOrange)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(onClick = onReject),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Reddet",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BadiList(
    badis: List<BadiWithUser>,
    onBadiClick: (BadiWithUser) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(badis, key = { it.badi.id }) { badi ->
            BadiCard(
                badi = badi,
                onClick = { onBadiClick(badi) }
            )
        }
    }
}

@Composable
fun BadiCard(
    badi: BadiWithUser,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            com.bardino.dozi.core.ui.theme.DoziTurquoise.copy(alpha = 0.08f),
                            com.bardino.dozi.core.ui.theme.DoziPurple.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profil fotoğrafı
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(com.bardino.dozi.core.ui.theme.DoziTurquoise, com.bardino.dozi.core.ui.theme.DoziPurple)
                            ),
                            shape = CircleShape
                        )
                        .padding(3.dp)
                ) {
                    AsyncImage(
                        model = badi.user.photoUrl,
                        contentDescription = "Profil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Kullanıcı bilgisi
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        badi.badi.nickname ?: badi.user.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = com.bardino.dozi.core.ui.theme.TextPrimary
                    )
                    Text(
                        badi.user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.bardino.dozi.core.ui.theme.TextSecondary
                    )

                    // İzin durumu
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (badi.badi.permissions.canViewReminders) {
                            Chip(text = "📋 İlaçlar", color = com.bardino.dozi.core.ui.theme.DoziTurquoise)
                        }
                        if (badi.badi.notificationPreferences.onMedicationTime) {
                            Chip(text = "🔔 Bildirim", color = com.bardino.dozi.core.ui.theme.DoziPurple)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(com.bardino.dozi.core.ui.theme.DoziTurquoise, com.bardino.dozi.core.ui.theme.DoziPurple)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        "Detay",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, color: Color = com.bardino.dozi.core.ui.theme.DoziTurquoise) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EmptyBadiState(onAddBadi: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(
                                com.bardino.dozi.core.ui.theme.DoziTurquoise.copy(alpha = 0.2f),
                                com.bardino.dozi.core.ui.theme.DoziPurple.copy(alpha = 0.2f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🤝",
                    style = MaterialTheme.typography.displayLarge
                )
            }
            Text(
                "Henüz Badiniz Yok",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = com.bardino.dozi.core.ui.theme.TextPrimary
            )
            Text(
                "Sevdiklerinizi badi olarak ekleyin\nilaç takibinizi birlikte yönetin",
                style = MaterialTheme.typography.bodyLarge,
                color = com.bardino.dozi.core.ui.theme.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAddBadi,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(com.bardino.dozi.core.ui.theme.DoziTurquoise, com.bardino.dozi.core.ui.theme.DoziPurple)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, "Ekle", tint = Color.White)
                        Text("Badi Ekle", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
