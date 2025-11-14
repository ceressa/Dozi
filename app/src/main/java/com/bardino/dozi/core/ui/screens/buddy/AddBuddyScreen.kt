package com.bardino.dozi.core.ui.screens.buddy

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bardino.dozi.core.ui.viewmodel.BuddyViewModel

/**
 * Buddy Ekleme Ekranı
 * Kullanıcı buddy kodu veya email ile buddy ekleyebilir
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBuddyScreen(
    onNavigateBack: () -> Unit,
    viewModel: BuddyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showMyCodeDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("➕ Buddy Ekle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
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
            MyBuddyCodeCard(
                onClick = {
                    viewModel.generateBuddyCode()
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
                        label = { Text("Buddy Kodu") },
                        placeholder = { Text("6 haneli kod girin") },
                        leadingIcon = { Icon(Icons.Default.Pin, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.length == 6) {
                                    viewModel.searchUserByBuddyCode(searchQuery)
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
                        0 -> viewModel.searchUserByBuddyCode(searchQuery)
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
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "✅ Kullanıcı Bulundu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    user.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                                viewModel.sendBuddyRequest(
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
                            Text("Buddy İsteği Gönder")
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "💡 İpuçları",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "• Buddy kodunuz ile arkadaşlarınız sizi kolayca bulabilir",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Email ile arama yapmak için kayıtlı email adresi gereklidir",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Buddy istekleri 7 gün geçerlidir",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Kodumu göster dialog
    if (showMyCodeDialog && uiState.buddyCode != null) {
        MyBuddyCodeDialog(
            code = uiState.buddyCode!!,
            onDismiss = { showMyCodeDialog = false }
        )
    }
}

@Composable
fun MyBuddyCodeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Kodumu Göster",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Arkadaşlarınız bu kodu kullanarak sizi bulabilir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(Icons.Default.QrCode, "Kod")
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
                "Buddy Kodunuz",
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
