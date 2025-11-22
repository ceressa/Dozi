package com.bardino.dozi.core.ui.screens.medicine

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.bardino.dozi.core.data.Ilac
import com.bardino.dozi.core.data.LocalMedicine
import com.bardino.dozi.core.data.MedicineLookupRepository
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.model.MedicineColor
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.bardino.dozi.core.premium.PremiumManager
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.ui.components.PremiumLimitDialog
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// EntryPoint for accessing PremiumManager in Composable
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MedicineEditEntryPoint {
    fun premiumManager(): PremiumManager
    fun medicineRepository(): MedicineRepository
}

// Emoji ikonları for custom medicines
private val medicineEmojis = listOf(
    "💊", "💉", "🩹", "🧴", "🌿", "🍃", "✨", "❤️", "🧬", "🩺"
)

// --------------------------------------------------------------------
// 🔍 Bellekten İlaç Doğrulama
// --------------------------------------------------------------------
fun verifyMedicine(name: String): Ilac? {
    return MedicineLookupRepository.findByNameOrIngredient(name)
}

// --------------------------------------------------------------------
// 📦 İlaç isminden stok bilgisini çıkart
// --------------------------------------------------------------------
fun extractStockFromName(productName: String?): Int {
    if (productName.isNullOrBlank()) return 0

    // "20 tablet", "x30", "30 kapsül", "50'li kutu" gibi patternler
    val patterns = listOf(
        Regex("""(\d+)\s*tablet""", RegexOption.IGNORE_CASE),
        Regex("""(\d+)\s*kaps[üu]l""", RegexOption.IGNORE_CASE),
        Regex("""x\s*(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""(\d+)'li\s*kutu""", RegexOption.IGNORE_CASE),
        Regex("""(\d+)\s*adet""", RegexOption.IGNORE_CASE),
        Regex("""\b(\d+)\s*(tb|kps|amp|flakon)\b""", RegexOption.IGNORE_CASE)
    )

    patterns.forEach { pattern ->
        pattern.find(productName)?.groups?.get(1)?.value?.toIntOrNull()?.let {
            android.util.Log.d("MedicineEdit", "✅ Extracted stock: $it from '$productName'")
            return it
        }
    }

    android.util.Log.d("MedicineEdit", "⚠️ No stock found in '$productName', defaulting to 0")
    return 0
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
    medicineId: String,
    onNavigateBack: () -> Unit,
    onNavigateToReminder: ((String) -> Unit)? = null,
    onNavigateToPremium: () -> Unit = {},
    savedStateHandle: androidx.lifecycle.SavedStateHandle? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 💎 Premium Manager için EntryPoint erişimi
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MedicineEditEntryPoint::class.java
        )
    }
    val premiumManager = entryPoint.premiumManager()
    val medicineRepository = entryPoint.medicineRepository()

    // 📊 Limit kontrolü için state'ler
    var showLimitDialog by remember { mutableStateOf(false) }
    var currentMedicineCount by remember { mutableStateOf(0) }
    var medicineLimit by remember { mutableStateOf(0) }

    // Yeni ilaç ekleniyorsa limit kontrolü yap
    LaunchedEffect(medicineId) {
        if (medicineId == "new") {
            try {
                currentMedicineCount = medicineRepository.getMedicineCount()
                medicineLimit = premiumManager.getMedicineLimit()
                android.util.Log.d("MedicineEditScreen", "📊 Medicine limits - Current: $currentMedicineCount, Limit: $medicineLimit")
            } catch (e: Exception) {
                android.util.Log.e("MedicineEditScreen", "Error loading medicine limits", e)
            }
        }
    }

    // Limit dialog'u
    if (showLimitDialog) {
        PremiumLimitDialog(
            title = "İlaç Limiti",
            message = "Ücretsiz planda en fazla $medicineLimit ilaç ekleyebilirsiniz. Sınırsız ilaç için Dozi Ekstra'ya geçin.",
            currentCount = currentMedicineCount,
            maxCount = medicineLimit,
            requiredPlan = "Dozi Ekstra",
            onDismiss = { showLimitDialog = false },
            onUpgrade = {
                showLimitDialog = false
                onNavigateToPremium()
            }
        )
    }

    // Lookup ekranından gelen ilaç bilgisi
    val selectedMedicine = savedStateHandle?.get<IlacSearchResultParcelable>("selectedMedicine")

    // Mevcut ilaç bilgilerini yükle
    val existing = MedicineLookupRepository.getLocalMedicine(context, medicineId)
    var name by remember { mutableStateOf(
        existing?.name
            ?: selectedMedicine?.item?.Product_Name
            ?: ""
    ) }

    // ✅ Stok bilgisini akıllıca belirle:
    // 1. Mevcut ilaç varsa -> onun stoğunu kullan
    // 2. Yoksa -> 0 (ad yazıldıkça otomatik güncellen ecek)
    var stock by remember { mutableStateOf(
        existing?.stock?.toString() ?: "0"
    ) }

    // Custom medicine fields
    var isCustomMedicine by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(MedicineColor.BLUE) }
    var selectedEmoji by remember { mutableStateOf("💊") }
    var firestoreMedicine by remember { mutableStateOf<Medicine?>(null) }

    // Load from Firestore for existing medicines
    LaunchedEffect(medicineId) {
        if (medicineId != "new") {
            withContext(Dispatchers.IO) {
                try {
                    val repo = com.bardino.dozi.core.data.repository.MedicineRepository()
                    val medicine = repo.getMedicineById(medicineId)
                    if (medicine != null) {
                        withContext(Dispatchers.Main) {
                            firestoreMedicine = medicine
                            name = medicine.name
                            stock = medicine.stockCount.toString()
                            isCustomMedicine = medicine.isCustom
                            selectedColor = medicine.color
                            selectedEmoji = medicine.icon

                            android.util.Log.d(
                                "MedicineEditScreen",
                                "Loaded from Firestore: ${medicine.name}, isCustom: ${medicine.isCustom}"
                            )
                        }
                    } else {
                        Unit
                    }


                } catch (e: Exception) {
                    android.util.Log.e("MedicineEditScreen", "Failed to load from Firestore", e)
                }
            }
        }
    }


    // İlaç adı değiştiğinde stok'u otomatik güncelle
    LaunchedEffect(name) {
        if (existing == null && name.isNotBlank()) {
            val extracted = extractStockFromName(name)
            if (extracted > 0) {
                stock = extracted.toString()
            }
        }
    }

    // Lookup'tan gelen veriyi işle ve temizle
    LaunchedEffect(selectedMedicine) {
        if (selectedMedicine != null) {
            // Seçilen ilaçtan stok bilgisini çıkart
            selectedMedicine.item.Product_Name?.let { productName ->
                val extracted = extractStockFromName(productName)
                android.util.Log.d("MedicineEdit", "📦 Product: $productName, Extracted stock: $extracted")
                if (extracted > 0 && existing == null) {
                    stock = extracted.toString()
                    android.util.Log.d("MedicineEdit", "✅ Stock updated to: $stock")
                }
            }
            savedStateHandle?.remove<IlacSearchResultParcelable>("selectedMedicine")
        }
    }

    // Hata durumları
    var nameError by remember { mutableStateOf(false) }
    var stockError by remember { mutableStateOf(false) }

    // Hatırlatma dialog'u
    var showReminderDialog by remember { mutableStateOf(false) }
    var savedMedicineId by remember { mutableStateOf<String?>(null) }

    // Animasyon
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = if (medicineId == "new") "Yeni İlaç Ekle" else "İlaç Düzenle",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                backgroundColor = MaterialTheme.colorScheme.surface
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Başlık
                AnimatedVisibility(visible = isVisible, enter = fadeIn()) {
                    Text(
                        text = if (medicineId == "new") "✨ Yeni ilaç ekle" else "İlaç bilgilerini düzenle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 📊 Free kullanıcı için limit bilgisi
                if (medicineId == "new" && medicineLimit > 0) {
                    AnimatedVisibility(visible = isVisible, enter = fadeIn()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentMedicineCount >= medicineLimit)
                                    Color(0xFFFFEBEE) // Kırmızı arka plan
                                else
                                    Color(0xFFF3E5F5) // Mor arka plan
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (currentMedicineCount >= medicineLimit)
                                        Color(0xFFE53935)
                                    else
                                        DoziPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (currentMedicineCount >= medicineLimit)
                                        "İlaç limitinize ulaştınız ($currentMedicineCount/$medicineLimit). Daha fazla ilaç için Dozi Ekstra'ya geçin."
                                    else
                                        "Ücretsiz planda $medicineLimit ilaç ekleyebilirsiniz. ($currentMedicineCount/$medicineLimit kullanıldı)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (currentMedicineCount >= medicineLimit)
                                        Color(0xFFE53935)
                                    else
                                        Color(0xFF7B1FA2)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = isVisible, enter = slideInVertically() + fadeIn()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // 🔹 İlaç Adı
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; nameError = it.isBlank() },
                            label = { Text("İlaç Adı *", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                            placeholder = { Text("İlaç adını girin", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )

                        if (nameError) ErrorCard("İlaç adı boş bırakılamaz.")

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
                            label = { Text("Evde Kaç Adet Var? (1–999) *", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                            placeholder = { Text("Örn: 20 (kaç tablet/kapsül var)", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )

                        if (stockError) ErrorCard("Stok bilgisi geçersiz.")

                        // 🎨 Custom medicine fields - Emoji ve Renk seçimi
                        if (isCustomMedicine) {
                            Spacer(Modifier.height(8.dp))

                            // Emoji Seçimi
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "İkon Seç",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(medicineEmojis) { emoji ->
                                        Surface(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (selectedEmoji == emoji) 2.dp else 1.dp,
                                                    color = if (selectedEmoji == emoji) DoziTurquoise else VeryLightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedEmoji = emoji },
                                            color = if (selectedEmoji == emoji) DoziTurquoise.copy(alpha = 0.15f) else Color.Transparent
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = emoji, fontSize = 24.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Renk Seçimi
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Renk Kategorisi",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(MedicineColor.entries.toList()) { color ->
                                        val colorValue = try {
                                            Color(android.graphics.Color.parseColor(color.hexColor))
                                        } catch (e: Exception) {
                                            DoziTurquoise
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (selectedColor == color) 3.dp else 1.dp,
                                                    color = if (selectedColor == color) colorValue else VeryLightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedColor = color },
                                            color = colorValue.copy(alpha = 0.3f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (selectedColor == color) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = colorValue,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.size(0.dp))
                                                }

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save button at bottom
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.background
            ) {
                // 💾 Kaydet Butonu
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                        nameError = name.isBlank()
                        stockError = stock.isEmpty() || stock.toIntOrNull() == null

                        if (!nameError && !stockError) {
                            val newId = if (medicineId == "new") UUID.randomUUID().toString() else medicineId
                            val updated = LocalMedicine(newId, name.trim(), stock = stock.toInt())

                            // İlaç doğrulama
                            val match = MedicineLookupRepository.findByNameOrIngredient(name)
                            if (match != null) {
                                Toast.makeText(
                                    context,
                                    "✅ Doğrulandı: ${match.Product_Name}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            MedicineLookupRepository.saveLocalMedicine(context, updated)

                            // 🔥 Firestore'a da kaydet (stockCount ile)
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                try {
                                    val firestoreRepo = com.bardino.dozi.core.data.repository.MedicineRepository()
                                    // Mevcut Firestore ilaç var mı kontrol et
                                    val existingMedicine = firestoreRepo.getMedicineById(newId)

                                    if (existingMedicine != null) {
                                        // Mevcut ilacı güncelle
                                        var allSuccess = true

                                        // Temel alanları güncelle
                                        if (!firestoreRepo.updateMedicineField(newId, "stockCount", stock.toInt())) {
                                            allSuccess = false
                                        }
                                        if (!firestoreRepo.updateMedicineField(newId, "name", name.trim())) {
                                            allSuccess = false
                                        }

                                        // Custom medicine ise ek alanları da güncelle
                                        if (isCustomMedicine) {
                                            if (!firestoreRepo.updateMedicineField(newId, "color", selectedColor.name)) {
                                                allSuccess = false
                                            }
                                            if (!firestoreRepo.updateMedicineField(newId, "icon", selectedEmoji)) {
                                                allSuccess = false
                                            }
                                        }

                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            if (allSuccess) {
                                                android.util.Log.d("MedicineEditScreen", "✅ Firestore güncellendi: $newId -> ${name.trim()}, stock: ${stock.toInt()}")
                                                if (isCustomMedicine) {
                                                    android.util.Log.d("MedicineEditScreen", "✅ Custom fields güncellendi: color=${selectedColor.name}, icon=$selectedEmoji")
                                                }
                                            } else {
                                                android.util.Log.e("MedicineEditScreen", "❌ Firestore güncelleme kısmen başarısız: $newId")
                                                Toast.makeText(context, "⚠️ Bazı değişiklikler kaydedilemedi", Toast.LENGTH_LONG).show()
                                            }
                                            // Mevcut ilaç güncelleme - geri dön
                                            onNavigateBack()
                                        }
                                    } else {
                                        // 📊 Yeni ilaç eklemeden önce limit kontrolü
                                        if (medicineLimit > 0 && currentMedicineCount >= medicineLimit) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                showLimitDialog = true
                                            }
                                            return@launch
                                        }

                                        // Yeni ilaç oluştur (temel bilgilerle)
                                        val firestoreMedicine = com.bardino.dozi.core.data.model.Medicine(
                                            id = "",
                                            name = name.trim(),
                                            stockCount = stock.toInt(),
                                            boxSize = stock.toInt(),
                                            reminderEnabled = false
                                        )
                                        val savedMedicineResult = firestoreRepo.addMedicine(firestoreMedicine)
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            if (savedMedicineResult != null) {
                                                android.util.Log.d("MedicineEditScreen", "✅ Firestore'a yeni ilaç eklendi: ${name.trim()} with id: ${savedMedicineResult.id}")
                                                // Yeni ilaç eklendiyse hatırlatma dialog'unu göster - Firestore ID kullan
                                                if (medicineId == "new" && onNavigateToReminder != null) {
                                                    savedMedicineId = savedMedicineResult.id  // Firestore'dan dönen ID'yi kullan
                                                    showReminderDialog = true
                                                } else {
                                                    onNavigateBack()
                                                }
                                            } else {
                                                android.util.Log.e("MedicineEditScreen", "❌ Firestore kayıt başarısız: ${name.trim()}")
                                                Toast.makeText(context, "⚠️ İlaç yerel olarak kaydedildi ama sunucuya gönderilemedi", Toast.LENGTH_LONG).show()
                                                // Firestore başarısız olsa da local ID ile devam et
                                                if (medicineId == "new" && onNavigateToReminder != null) {
                                                    savedMedicineId = newId
                                                    showReminderDialog = true
                                                } else {
                                                    onNavigateBack()
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MedicineEditScreen", "❌ Firestore kayıt hatası", e)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        Toast.makeText(context, "⚠️ İlaç yerel olarak kaydedildi ama sunucu bağlantısı başarısız", Toast.LENGTH_LONG).show()
                                        // Hata durumunda da local ID ile devam et
                                        if (medicineId == "new" && onNavigateToReminder != null) {
                                            savedMedicineId = newId
                                            showReminderDialog = true
                                        } else {
                                            onNavigateBack()
                                        }
                                    }
                                }
                            }
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

    // Hatırlatma ekleme dialog'u
    if (showReminderDialog && savedMedicineId != null) {
        AddReminderDialog(
            medicineName = name,
            onAddReminder = {
                showReminderDialog = false
                onNavigateToReminder?.invoke(savedMedicineId!!)
            },
            onDismiss = {
                showReminderDialog = false
                onNavigateBack()
            }
        )
    }
}

// Hatırlatma ekleme dialog'u
@Composable
private fun AddReminderDialog(
    medicineName: String,
    onAddReminder: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = DoziTurquoise,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Hatırlatma Ekle",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "$medicineName için bir hatırlatma kurmak ister misin?",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hatırlatma kurarak ilaçlarını düzenli alabilirsin!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAddReminder,
                colors = ButtonDefaults.buttonColors(containerColor = DoziTurquoise)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Evet, Ekle", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Şimdi Değil", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
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

    val dosage = normLines.firstNotNullOfOrNull { dosageRx.find(it)?.value }
        ?: dosageRx.find(merged)?.value ?: ""
    val stock = normLines.firstNotNullOfOrNull { stockRx.find(it)?.groupValues?.get(1) }
        ?: stockRx.find(merged)?.groupValues?.get(1) ?: ""

    val nameCandidates = merged.split(" ").filter {
        it.length > 2 && it !in forbidden && it.any { c -> c.isLetter() }
    }
    val name = nameCandidates.firstOrNull()?.capitalizeFirst() ?: ""

    return Triple(name, dosage, stock)
}

// --------------------------------------------------------------------
// UI Yardımcıları
// --------------------------------------------------------------------
private fun String.capitalizeFirst(): String =
    if (isEmpty()) this else this[0].uppercase() + substring(1)

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