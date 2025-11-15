package com.bardino.dozi.core.ui.screens.medicine

import android.Manifest
import android.content.Intent
import android.os.Parcelable
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.bardino.dozi.core.data.Ilac
import com.bardino.dozi.core.data.IlacJsonRepository
import com.bardino.dozi.core.data.IlacSearchResult
import com.bardino.dozi.core.data.OnboardingPreferences
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import com.bardino.dozi.navigation.Screen
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class IlacSearchResultParcelable(
    val item: @RawValue Ilac,
    val dosage: String? = null
) : Parcelable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineLookupScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<IlacSearchResult>>(emptyList()) }
    var selected by remember { mutableStateOf<IlacSearchResult?>(null) }
    var showDetail by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf("text") } // "text", "barcode", "voice"

    // Onboarding'den ilaç eklendikten sonra geri dönme kontrolü
    LaunchedEffect(Unit) {
        if (OnboardingPreferences.isInOnboarding(context) &&
            OnboardingPreferences.getOnboardingStep(context) == "medicine_completed") {
            // İlaç eklendi, onboarding'e geri dön
            onNavigateBack()
        }
    }

    // ✅ Barkod Tarayıcı
    val barcodeOptions = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8)
        .build()

    val barcodeScanner = remember { GmsBarcodeScanning.getClient(context, barcodeOptions) }

    // Ses tanıma
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()?.let {
                query = it
                searchMode = "text"
                Toast.makeText(context, "🎙️ \"$it\" algılandı", Toast.LENGTH_SHORT).show()
            }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "İlaç adını söyleyin")
            }
            voiceLauncher.launch(intent)
        } else Toast.makeText(context, "Mikrofon izni gerekli", Toast.LENGTH_SHORT).show()
    }

    // ✅ Barkod Tarama Fonksiyonu
    fun startBarcodeScanning() {
        isSearching = true
        searchMode = "barcode"
        barcodeScanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (rawValue != null) {
                    Toast.makeText(context, "📦 Barkod: $rawValue", Toast.LENGTH_SHORT).show()
                    // Barkoda göre arama yap
                    val foundMedicine = IlacJsonRepository.searchByBarcode(context, rawValue)
                    if (foundMedicine != null) {
                        results = listOf(IlacSearchResult(foundMedicine, foundMedicine.Product_Name))
                        query = foundMedicine.Product_Name ?: rawValue
                    } else {
                        Toast.makeText(context, "❌ Barkod bulunamadı: $rawValue", Toast.LENGTH_LONG).show()
                        query = rawValue
                    }
                } else {
                    Toast.makeText(context, "Barkod okunamadı", Toast.LENGTH_SHORT).show()
                }
                isSearching = false
                searchMode = "text"
            }
            .addOnCanceledListener {
                Toast.makeText(context, "Tarama iptal edildi", Toast.LENGTH_SHORT).show()
                isSearching = false
                searchMode = "text"
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Barkod okuma hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                isSearching = false
                searchMode = "text"
            }
    }

    // Debounce arama
    LaunchedEffect(query) {
        if (searchMode == "text" && query.length >= 2) {
            isSearching = true
            delay(700)
            results = IlacJsonRepository.search(context, query)
            isSearching = false
        } else if (query.isEmpty()) {
            results = emptyList()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DoziTopBar(
                title = "İlaç Arama",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    // ✅ Ses Butonu
                    IconButton(
                        onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(DoziTurquoise.copy(alpha = 0.1f), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Mic, null, tint = DoziCoral)
                    }

                    Spacer(Modifier.width(8.dp))

                    // ✅ Barkod Butonu
                    IconButton(
                        onClick = { startBarcodeScanning() },
                        modifier = Modifier
                            .size(44.dp)
                            .background(DoziCoral.copy(alpha = 0.1f), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, tint = DoziCoral)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Arama kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            searchMode = "text"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("İlaç adı, barkod veya etken madde...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        leadingIcon = {
                            Icon(
                                if (searchMode == "barcode") Icons.Default.QrCodeScanner else Icons.Default.Search,
                                null,
                                tint = if (searchMode == "barcode") DoziCoral else DoziTurquoise
                            )
                        },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = {
                                    query = ""
                                    searchMode = "text"
                                }) {
                                    Icon(Icons.Default.Clear, null, tint = MediumGray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoziTurquoise,
                            unfocusedBorderColor = VeryLightGray,
                            cursorColor = DoziTurquoise,
                            focusedContainerColor = DoziTurquoise.copy(alpha = 0.05f), // ✅ YENİ
                            unfocusedContainerColor = Color.White // ✅ YENİ
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {})
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (searchMode) {
                                "barcode" -> "Barkod ile aranıyor..."
                                "voice" -> "Sesli arama..."
                                else -> "Yazarak, barkod okutarak veya sesli arayabilirsiniz"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // İçerik bölümü
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isSearching -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = DoziTurquoise,
                            strokeWidth = 3.dp
                        )
                        Text(
                            if (searchMode == "barcode") "Barkod okunuyor..." else "Aranıyor...",
                            color = TextSecondary
                        )
                    }

                    results.isNotEmpty() -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(results, key = { it.item.ID ?: it.item.Product_Name!! }) { res ->
                            MedicineResultCard(
                                result = res,
                                onClick = {
                                    selected = res
                                    showDetail = true
                                },
                                onQuickAdd = {
                                    navController.currentBackStackEntry?.savedStateHandle?.set(
                                        "selectedMedicine",
                                        IlacSearchResultParcelable(res.item, res.dosage)
                                    )
                                    navController.navigate(Screen.MedicineEdit.createRoute("new"))
                                }
                            )
                        }
                    }

                    query.isNotBlank() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            null,
                            tint = MediumGray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Sonuç bulunamadı", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Farklı bir arama terimi deneyin veya barkod okutun",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(90.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = DoziTurquoise.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Medication,
                                    null,
                                    tint = DoziTurquoise,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "İlaç aramaya başlayın",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Yukarıdaki kutuya yazın, barkod okutun veya sesli arama yapın",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }

        if (showDetail && selected != null) {
            MedicineDetailDialog(
                result = selected!!,
                onDismiss = { showDetail = false },
                onConfirm = {
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "selectedMedicine",
                        IlacSearchResultParcelable(selected!!.item, selected!!.dosage)
                    )
                    navController.navigate(Screen.MedicineEdit.createRoute("new"))
                    showDetail = false
                }
            )
        }
    }
}

@Composable
private fun MedicineResultCard(
    result: IlacSearchResult,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, VeryLightGray)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                Modifier.size(48.dp),
                color = DoziTurquoise.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Medication, null, tint = DoziTurquoise)
                }
            }

            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                result.item.Product_Name?.let {
                    Text(it, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                result.item.Active_Ingredient?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                result.dosage?.let {
                    Surface(color = DoziTurquoise.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                        Text(it, color = DoziTurquoise, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }

            Surface(
                onClick = onQuickAdd,
                color = SuccessGreen.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = SuccessGreen)
                }
            }
        }
    }
}

@Composable
private fun MedicineDetailDialog(
    result: IlacSearchResult,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = Color.White) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        Modifier.size(56.dp),
                        color = DoziTurquoise.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Medication, null, tint = DoziTurquoise)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("İlaç Detayı", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Divider(color = VeryLightGray)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("İlaç Adı", result.item.Product_Name)
                    result.item.Active_Ingredient?.takeIf { it.isNotBlank() }?.let {
                        DetailRow("Etken Madde", it)
                    }
                    result.dosage?.let { DetailRow("Dozaj", it) }
                }
                Divider(color = VeryLightGray)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, VeryLightGray),
                        shape = MaterialTheme.shapes.medium
                    ) { Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DoziTurquoise),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ekle")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        if (value != null) Text(value, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
