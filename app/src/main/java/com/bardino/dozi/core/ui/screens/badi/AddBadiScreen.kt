package com.bardino.dozi.core.ui.screens.badi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.bardino.dozi.core.premium.PremiumManager
import com.bardino.dozi.core.ui.components.PremiumLimitDialog
import com.bardino.dozi.core.common.Constants
import com.bardino.dozi.core.ui.viewmodel.BadiViewModel
import com.bardino.dozi.navigation.Screen

// EntryPoint for accessing PremiumManager in Composable
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AddBadiEntryPoint {
    fun premiumManager(): PremiumManager
}

/**
 * Badi Ekleme Ekranı
 * Kullanıcı badi kodu veya email ile badi ekleyebilir
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBadiScreen(
    onNavigateBack: () -> Unit,
    navController: NavController,
    viewModel: BadiViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()

    // 💎 Premium Manager için EntryPoint erişimi
    val premiumManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AddBadiEntryPoint::class.java
        ).premiumManager()
    }

    // 📊 Badi limit state'leri
    var showLimitDialog by remember { mutableStateOf(false) }
    var currentBadiCount by remember { mutableStateOf(0) }
    var badiLimit by remember { mutableStateOf(0) }

    // Limit bilgilerini yükle
    LaunchedEffect(Unit) {
        try {
            currentBadiCount = viewModel.getBadiCount()
            badiLimit = premiumManager.getBadiLimit()
            android.util.Log.d("AddBadiScreen", "📊 Badi limits loaded - Current: $currentBadiCount, Limit: $badiLimit")
        } catch (e: Exception) {
            android.util.Log.e("AddBadiScreen", "Error loading badi limits", e)
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showMyCodeDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

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
                        Text("Badi Ekle", color = Color.White, fontWeight = FontWeight.ExtraBold)
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
                        listOf(com.bardino.dozi.core.ui.theme.DoziTurquoise, com.bardino.dozi.core.ui.theme.DoziPurple)
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
            // Kodumu göster butonu
            MyBadiCodeCard(
                onClick = {
                    viewModel.generateBadiCode()
                    showMyCodeDialog = true
                }
            )

            Divider()

            // Tab seçimi
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("🔢 Kod ile") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("📧 Email ile") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Arama alanı
            when (selectedTab) {
                0 -> {
                    // Kod ile arama
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("BadiKodu") },
                        placeholder = { Text("6 haneli kod girin") },
                        leadingIcon = { Icon(Icons.Default.Pin, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.length == 6) {
                                    viewModel.searchUserByBadiCode(searchQuery)
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        singleLine = true
                    )
                }
                1 -> {
                    // Email ile arama
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email Adresi") },
                        placeholder = { Text("ornek@email.com") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotEmpty()) {
                                    viewModel.searchUserByEmail(searchQuery)
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        singleLine = true
                    )
                }
            }

            // Ara butonu
            Button(
                onClick = {
                    when (selectedTab) {
                        0 -> viewModel.searchUserByBadiCode(searchQuery)
                        1 -> viewModel.searchUserByEmail(searchQuery)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = searchQuery.isNotEmpty() && !searchState.isSearching
            ) {
                if (searchState.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Search, "Ara")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kullanıcı Ara")
                }
            }

            // Kullanıcı bulundu mu?
            searchState.foundUser?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(
                                        com.bardino.dozi.core.ui.theme.SuccessGreen.copy(alpha = 0.12f),
                                        com.bardino.dozi.core.ui.theme.DoziTurquoise.copy(alpha = 0.12f)
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(com.bardino.dozi.core.ui.theme.SuccessGreen, com.bardino.dozi.core.ui.theme.DoziTurquoise)
                                            ),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    "Kullanıcı Bulundu",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(com.bardino.dozi.core.ui.theme.DoziTurquoise.copy(alpha = 0.2f), com.bardino.dozi.core.ui.theme.DoziPurple.copy(alpha = 0.2f))
                                            ),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        null,
                                        modifier = Modifier.size(32.dp),
                                        tint = com.bardino.dozi.core.ui.theme.DoziTurquoise
                                    )
                                }
                                Column {
                                    Text(
                                        user.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        user.email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Mesaj alanı
                            OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Mesaj (Opsiyonel)") },
                            placeholder = { Text("Bir mesaj ekleyin") },
                            maxLines = 3
                            )

                            // İstek gönder butonu
                            Button(
                                onClick = {
                                    // 📊 Badi limit kontrolü
                                    if (badiLimit != Constants.UNLIMITED && currentBadiCount >= badiLimit) {
                                        showLimitDialog = true
                                        return@Button
                                    }

                                    viewModel.sendBadiRequest(
                                        user.uid,
                                        message.ifEmpty { null }
                                    )
                                    searchQuery = ""
                                    message = ""
                                    viewModel.clearSearchState()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isLoading
                            ) {
                                Icon(Icons.Default.Send, "Gönder")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Badiİsteği Gönder")
                            }
                        }
                    }
                }
            }

            // Hata mesajı
            searchState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // İpuçları
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(
                                    com.bardino.dozi.core.ui.theme.WarningOrange.copy(alpha = 0.08f),
                                    com.bardino.dozi.core.ui.theme.DoziTurquoise.copy(alpha = 0.08f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            listOf(com.bardino.dozi.core.ui.theme.WarningOrange, com.bardino.dozi.core.ui.theme.DoziTurquoise)
                                        ),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "💡",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                "İpuçları",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            "• Badi kodunuz ile arkadaşlarınız sizi kolayca bulabilir",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "• Email ile arama yapmak için kayıtlı email adresi gereklidir",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "• Badi istekleri 7 gün geçerlidir",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 📊 Premium limit dialog'u
    if (showLimitDialog) {
        PremiumLimitDialog(
            title = "Badi Limitine Ulaştınız",
            message = "Ücretsiz planda badi ekleyemezsiniz. Daha fazla badi eklemek için Dozi Ekstra'ya yükseltin.",
            currentCount = currentBadiCount,
            maxCount = badiLimit,
            requiredPlan = "Dozi Ekstra",
            onDismiss = {
                showLimitDialog = false
            },
            onUpgrade = {
                showLimitDialog = false
                navController.navigate(Screen.Premium.route)
            }
        )
    }

    // Kodumu göster dialog
    if (showMyCodeDialog && uiState.badiCode != null) {
        MyBuddyCodeDialog(
            code = uiState.badiCode!!,
            onDismiss = { showMyCodeDialog = false }
        )
    }
}

@Composable
fun MyBadiCodeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            com.bardino.dozi.core.ui.theme.DoziTurquoise.copy(alpha = 0.15f),
                            com.bardino.dozi.core.ui.theme.DoziPurple.copy(alpha = 0.15f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "🎫",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Kodumu Göster",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Arkadaşlarınız bu kodu kullanarak sizi bulabilir",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(com.bardino.dozi.core.ui.theme.DoziTurquoise, com.bardino.dozi.core.ui.theme.DoziPurple)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        "Kod",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MyBuddyCodeDialog(
    code: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.QrCode, null, modifier = Modifier.size(48.dp)) },
        title = {
            Text(
                "BadiKodunuz",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Bu kodu arkadaşlarınızla paylaşın",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        code,
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(code))
                }
            ) {
                Icon(Icons.Default.ContentCopy, "Kopyala")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kopyala")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }

    )
}

