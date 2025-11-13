package com.bardino.dozi.core.ui.screens.medicine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.bardino.dozi.core.data.Ilac
import com.bardino.dozi.core.data.MedicineRepository
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.util.UUID
import com.bardino.dozi.core.data.Medicine
import com.bardino.dozi.core.data.MedicineRepository.loadMedicines
import com.bardino.dozi.core.data.MedicineRepository.saveMedicine
import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color


// --------------------------------------------------------------------
// 🔍 Bellekten İlaç Doğrulama (MedicineRepository üzerinden)
// --------------------------------------------------------------------
fun verifyMedicine(name: String): Ilac? {
    return MedicineRepository.findByNameOrIngredient(name)
}


// --------------------------------------------------------------------
// 📷 Geçici fotoğraf dosyası URI'si oluşturur
// --------------------------------------------------------------------
private fun createImageUri(context: Context): Uri {
    val file = File.createTempFile(
        "ocr_photo_${System.currentTimeMillis()}",
        ".jpg",
        context.cacheDir
    )
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}

// --------------------------------------------------------------------
// 🧠 OCR işlemini çalıştırır
// --------------------------------------------------------------------
private fun processImageFromUri(
    context: Context,
    uri: Uri,
    onResult: (name: String, dosage: String, stock: String) -> Unit
) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        val image = InputImage.fromFilePath(context, uri)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val lines = result.textBlocks.flatMap { it.lines }.map { it.text }
                val merged = lines.joinToString(" ")
                val (n, d, s) = extractMedicineInfo(lines, merged)
                onResult(n, d, s)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Tanıma hatası oluştu.", Toast.LENGTH_SHORT).show()
            }
    } catch (e: IOException) {
        Toast.makeText(context, "Görüntü okunamadı: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// --------------------------------------------------------------------
// ✳️ Ana Ekran: Yeni İlaç Ekle / Düzenle
// --------------------------------------------------------------------
@Composable
fun MedicineEditScreen(
    navController: NavController,
    medicineId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val existing = remember(medicineId) {
        if (medicineId == "new") null else
            MedicineRepository.loadMedicines(context).find { it.id == medicineId }
    }
    val focusManager = LocalFocusManager.current

    var nameError by remember { mutableStateOf(false) }
    var nameEditable by remember { mutableStateOf(false) }
    var dosageError by remember { mutableStateOf(false) }
    var stockError by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var medicineNameState by remember { mutableStateOf("") }
    var selectedTimeState by remember { mutableStateOf("") }

    // 🔹 Lookup’tan gelen veriyi al
    val selectedMedicine =
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<IlacSearchResultParcelable>("selectedMedicine")

    // 💊 Ürün adından dozaj tespiti
    val dosageFromName = Regex(
        """\b(\d{1,4}(?:[.,]\d{1,2})?)\s*(mg|ml|µg|mcg|gr|g|iu)\b""",
        RegexOption.IGNORE_CASE
    ).find(selectedMedicine?.item?.Product_Name ?: "")
        ?.value

    // 🔹 Ürün adından stok tespiti
    val stockFromName = Regex(
        """\b(\d{1,4})\s*(tablet|kapsül|ampul|flakon|draje|adet|kutu)\b""",
        RegexOption.IGNORE_CASE
    ).find(selectedMedicine?.item?.Product_Name ?: "")
        ?.groupValues?.get(1)

    // 🔹 Form değerleri
    var name by remember { mutableStateOf(selectedMedicine?.item?.Product_Name ?: existing?.name ?: "") }
    var dosage by remember {
        mutableStateOf(dosageFromName ?: selectedMedicine?.dosage ?: existing?.dosage ?: "")
    }
    var stock by remember { mutableStateOf(stockFromName ?: existing?.stock?.toString() ?: "") }

    // 🧠 Sesli komutla gelen verileri yakala
    val suggestedName = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<String>("suggestedName")

    val preselectedTime = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<String>("preselectedTime")

    // 🩵 Eğer ekrandaki alanlar boşsa sesli gelen verileri yerleştir
    if (suggestedName != null && medicineNameState.isBlank()) {
        medicineNameState = suggestedName
    }

    if (preselectedTime != null && selectedTimeState.isBlank()) {
        selectedTimeState = preselectedTime
    }

    // UI bileşenleri buradan devam eder
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = medicineNameState,
            onValueChange = { medicineNameState = it },
            label = { Text("İlaç Adı") }
        )

        TextField(
            value = selectedTimeState,
            onValueChange = { selectedTimeState = it },
            label = { Text("Hatırlatma Saati (HH:mm)") }
        )
    }

    LaunchedEffect(Unit) { isVisible = true }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = if (medicineId == "new") "Yeni İlaç Ekle" else "İlaç Düzenle",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                backgroundColor = Color.White
            )
        },
        containerColor = BackgroundLight
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AnimatedVisibility(visible = isVisible, enter = fadeIn()) {
                Text(
                    text = if (medicineId == "new") "✨ Yeni ilaç ekle" else "İlaç bilgilerini düzenle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            AnimatedVisibility(visible = isVisible, enter = slideInVertically() + fadeIn()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // 🔹 İlaç Adı
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = it.isBlank() },
                        label = { Text("İlaç Adı *", color = TextSecondaryLight) },
                        leadingIcon = {
                            Icon(Icons.Default.LocalPharmacy, null, tint = DoziCoralDark)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9FB), RoundedCornerShape(12.dp))
                            .padding(1.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoziTurquoise,
                            unfocusedBorderColor = VeryLightGray,
                            cursorColor = DoziTurquoise,
                            focusedContainerColor = Color(0xFFF9F9FB),
                            unfocusedContainerColor = Color(0xFFF9F9FB)
                        ),
                        singleLine = true,
                        isError = nameError,
                        readOnly = false,
                        placeholder = { Text("İlaç adını girin", color = TextSecondaryLight) }
                    )

                    if (nameError) ErrorCard("İlaç adı boş bırakılamaz.")

                    // 🔹 Dozaj
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it; dosageError = it.isBlank() },
                        label = { Text("Dozaj (örn: 500 mg) *", color = TextSecondaryLight) },
                        leadingIcon = { Icon(Icons.Default.Medication, null, tint = DoziCoralDark) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9FB), RoundedCornerShape(12.dp))
                            .padding(1.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoziCoralDark,
                            unfocusedBorderColor = VeryLightGray,
                            cursorColor = DoziCoralDark,
                            focusedContainerColor = Color(0xFFF9F9FB),
                            unfocusedContainerColor = Color(0xFFF9F9FB)
                        ),
                        singleLine = true,
                        isError = dosageError,
                        placeholder = { Text("Örneğin: 500 mg", color = TextSecondaryLight) }
                    )

                    if (dosageError) ErrorCard("Dozaj bilgisi boş bırakılamaz.")

                    // 🔹 Stok
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                val num = newValue.toIntOrNull()
                                if (num == null || num in 1..999) {
                                    stock = newValue
                                    stockError = newValue.isEmpty()
                                }
                            }
                        },
                        label = { Text("Stok Adedi (1–999) *", color = TextSecondaryLight) },
                        leadingIcon = { Icon(Icons.Default.Inventory, null, tint = DoziCoralDark) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9FB), RoundedCornerShape(12.dp))
                            .padding(1.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoziBlue,
                            unfocusedBorderColor = VeryLightGray,
                            cursorColor = DoziBlue,
                            focusedContainerColor = Color(0xFFF9F9FB),
                            unfocusedContainerColor = Color(0xFFF9F9FB)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = stockError,
                        placeholder = { Text("Stok miktarını girin", color = TextSecondaryLight) }
                    )

                    if (stockError) ErrorCard("Stok bilgisi geçersiz.")
                }
            }

            Spacer(Modifier.weight(1f))

            // 💾 Kaydet Butonu
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()
            ) {
                Button(
                    onClick = {
                        nameError = name.isBlank()
                        dosageError = dosage.isBlank()
                        stockError = stock.isEmpty() || stock.toIntOrNull() == null

                        if (!nameError && !dosageError && !stockError) {
                            val newId = if (medicineId == "new") UUID.randomUUID().toString() else medicineId
                            val updated = Medicine(newId, name.trim(), dosage.trim(), stock.toInt())

                            val match = MedicineRepository.findByNameOrIngredient(name)
                            if (match != null) {
                                Toast.makeText(
                                    context,
                                    "✅ Doğrulandı: ${match.Product_Name}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            MedicineRepository.saveMedicine(context, updated)
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DoziTurquoise)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (medicineId == "new") "İlaç Ekle" else "Kaydet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

}

// --------------------------------------------------------------------
// 🧠 OCR Metin Analizi
// --------------------------------------------------------------------
private fun extractMedicineInfo(lines: List<String>, mergedRaw: String): Triple<String, String, String> {
    val forbidden = setOf("tablet", "kapsül", "ampul", "flakon", "draje", "adet", "kutu", "mg", "ml", "µg", "mcg", "gr", "g", "iu", "film")

    fun normalize(s: String) = s.replace("\n", " ").replace(Regex("\\s+"), " ").replace(",", " ").trim()

    val merged = normalize(mergedRaw).lowercase()
    val normLines = lines.map { normalize(it).lowercase() }

    val dosageRx = Regex("""(\d{1,4}(?:[.,]\d{1,2})?)\s*(mg|ml|µg|mcg|gr|g|iu)\b""")
    val stockRx = Regex("""(\d{1,3})\s*(tablet|kapsül|ampul|flakon|draje|adet|kutu)\b""")


    val dosage = normLines.firstNotNullOfOrNull { dosageRx.find(it)?.value } ?: dosageRx.find(merged)?.value ?: ""
    val stock = normLines.firstNotNullOfOrNull { stockRx.find(it)?.groupValues?.get(1) } ?: stockRx.find(merged)?.groupValues?.get(1) ?: ""

    val nameCandidates = merged.split(" ").filter { it.length > 2 && it !in forbidden && it.any { c -> c.isLetter() } }
    val name = nameCandidates.firstOrNull()?.capitalizeFirst() ?: ""

    return Triple(name, dosage, stock)
}

// --------------------------------------------------------------------
// UI Yardımcıları
// --------------------------------------------------------------------
private fun String.capitalizeFirst(): String = if (isEmpty()) this else this[0].uppercase() + substring(1)

@Composable
private fun ErrorCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ErrorRed.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
        Text(
            message,
            color = ErrorRed,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
        )
    }
}


@Composable
fun rememberVoiceRecognitionLauncher(
    onResult: (String) -> Unit
): ActivityResultLauncher<Intent> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        spokenText?.let { onResult(it) }
    }
}
