package com.bardino.dozi.core.data.model

/**
 * Firestore-compatible Medicine data model
 * Represents a medicine with its schedule and dosage information
 */
data class Medicine(
    val id: String = "",
    val userId: String = "",                    // Firebase Auth UID
    val name: String = "",
    val dosage: String = "",                    // "1", "1.5", "2" etc.
    val unit: String = "hap",                   // hap, doz, mg, ml, adet, damla, kaşık
    val form: String = "tablet",                // tablet, kapsül, şurup, damla
    val times: List<String> = emptyList(),      // ["09:00", "13:00", "21:00"]
    val days: List<String> = emptyList(),       // ["Pazartesi", "Salı"] or empty for everyday
    val frequency: String = "Her gün",          // "Her gün", "Gün aşırı", "Haftada bir", "Her X günde bir", "İstediğim tarihlerde"
    val frequencyValue: Int = 1,                // X value for "Her X günde bir"
    val startDate: Long = 0L,                   // Timestamp
    val endDate: Long? = null,                  // Null = sürekli kullanım

    // 📦 Stok Takip Sistemi
    val stockCount: Int = 0,                    // Kalan ilaç sayısı
    val boxSize: Int = 0,                       // Bir kutudaki ilaç sayısı
    val stockWarningThreshold: Int = 7,         // Kaç günlük kaldığında uyarı verilsin
    val lastRestockDate: Long? = null,          // Son stok yenileme tarihi
    val autoDecrementEnabled: Boolean = true,   // Aldım dendiğinde otomatik azalsın mı?

    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reminderEnabled: Boolean = true,
    val reminderName: String = "",              // Hatırlatma adı (ör: "Sabah İlacım", "Kahvaltıdan Önce")
    val icon: String = "💊",                    // Emoji icon for visual display

    // 🤝 Badi sistem için yeni alanlar
    val sharedWithBadis: List<String> = emptyList(), // Paylaşılan badi userId'leri
    val barcode: String? = null,                // Barkod/QR kod
    val imageUrl: String? = null,               // İlaç fotoğrafı
    val manufacturer: String? = null,           // Üretici firma
    val activeIngredient: String? = null,       // Etken madde

    // 🚨 Acil durum ve kritiklik ayarları
    val criticalityLevel: MedicineCriticality = MedicineCriticality.ROUTINE,  // İlaç kritiklik seviyesi

    // 💡 Motivasyon ve görselleştirme
    val motivationReason: String = "",           // "Şeker hastalığım için", "Sağlıklı kalmak için"
    val color: MedicineColor = MedicineColor.BLUE , // İlaç renk kategorisi

    val selectedDays: List<String> = emptyList(),
    val selectedDates: List<String> = emptyList(),

    // 🎨 Özel ilaç desteği
    val isCustom: Boolean = false                   // Kullanıcı tarafından eklenen özel ilaç mı?
)

/**
 * İlaç kritiklik seviyeleri
 */
enum class MedicineCriticality {
    ROUTINE,      // Normal ilaç - DND'ye uyar
    IMPORTANT,    // Önemli ilaç - DND'de sessiz bildirim
    CRITICAL      // Kritik ilaç - DND'yi bypass eder
}

/**
 * İlaç renk kategorileri (görsel ayırt etme için)
 */
enum class MedicineColor(val displayName: String, val hexColor: String, val emoji: String) {
    BLUE("Mavi", "#2196F3", "💙"),        // Genel ilaçlar
    RED("Kırmızı", "#F44336", "❤️"),      // Kalp/tansiyon
    GREEN("Yeşil", "#4CAF50", "💚"),      // Vitamin/takviye
    YELLOW("Sarı", "#FFEB3B", "💛"),      // Ağrı kesici
    PURPLE("Mor", "#9C27B0", "💜"),       // Antibiyotik
    ORANGE("Turuncu", "#FF9800", "🧡"),   // Şeker ilaçları
    PINK("Pembe", "#E91E63", "💗"),       // Hormon
    BROWN("Kahverengi", "#795548", "🤎")  // Diğer
}

// 📦 Stock Management Extensions

/**
 * Sayılabilir birim mi? (stok takibi için)
 * Sadece bu birimler için stok yüzde hesabı mantıklı
 */
fun Medicine.isCountableUnit(): Boolean {
    return unit.lowercase() in listOf("hap", "adet", "tablet", "kapsül", "doz")
}

/**
 * Hacim/ağırlık birimi mi?
 * Bu birimler için şişe/kutu kapasitesi gerekir
 */
fun Medicine.isVolumeUnit(): Boolean {
    return unit.lowercase() in listOf("ml", "damla", "kaşık", "mg")
}

/**
 * Günlük kullanım miktarını hesapla
 */
fun Medicine.dailyUsage(): Double {
    if (times.isEmpty()) return 0.0
    val dosageAmount = dosage.toDoubleOrNull() ?: 1.0

    return when (frequency) {
        "Her gün" -> dosageAmount * times.size
        "Gün aşırı" -> (dosageAmount * times.size) / 2.0
        "Haftada bir" -> (dosageAmount * times.size) / 7.0
        "Her X günde bir" -> (dosageAmount * times.size) / frequencyValue.toDouble()
        else -> dosageAmount * times.size // Default: her gün
    }
}

/**
 * Stokta kaç gün kaldığını hesapla
 */
fun Medicine.daysRemainingInStock(): Int {
    if (stockCount <= 0) return 0
    val daily = dailyUsage()
    if (daily <= 0) return Int.MAX_VALUE

    return (stockCount / daily).toInt()
}

/**
 * Stok yüzdesi (boxSize varsa ona göre, yoksa başlangıç stoğuna göre)
 */
fun Medicine.stockPercentage(): Int {
    if (!isCountableUnit()) return 100 // Sayılamaz birimler için varsayılan
    val totalCapacity = if (boxSize > 0) boxSize else stockCount
    if (totalCapacity <= 0) return 0
    return ((stockCount.toDouble() / totalCapacity) * 100).toInt().coerceIn(0, 100)
}

/**
 * Stok yüzde bazlı düşük mü? (%5 veya altı)
 */
fun Medicine.isStockPercentageLow(threshold: Int = 5): Boolean {
    if (!isCountableUnit() || boxSize <= 0) return false
    return stockPercentage() <= threshold
}

/**
 * Stok azaldı mı? (threshold'a göre)
 */
fun Medicine.isStockLow(): Boolean {
    // Yüzde bazlı kontrol (sayılabilir birimler için)
    if (isCountableUnit() && boxSize > 0 && isStockPercentageLow(10)) {
        return true
    }
    // Gün bazlı kontrol (her zaman)
    return daysRemainingInStock() <= stockWarningThreshold
}

/**
 * Stok bitmek üzere mi? (%5 veya 3 gün)
 */
fun Medicine.isStockCritical(): Boolean {
    if (stockCount <= 0) return false
    // Yüzde bazlı: %5 veya altı
    if (isCountableUnit() && boxSize > 0 && isStockPercentageLow(5)) {
        return true
    }
    // Gün bazlı: 3 gün veya altı
    return daysRemainingInStock() <= 3
}

/**
 * Stok tamamen bitti mi?
 */
fun Medicine.isStockEmpty(): Boolean {
    return stockCount <= 0
}

/**
 * Stok uyarı mesajı
 */
fun Medicine.getStockWarningMessage(): String? {
    // Hacim birimleri için özel mesaj
    if (isVolumeUnit() && stockCount <= 0) {
        return "⚠️ $name bitmiş olabilir! Kontrol et."
    }

    return when {
        isStockEmpty() -> "⚠️ $name stoğu bitti! Yenilemeyi unutma."
        isStockCritical() -> {
            val percentMsg = if (isCountableUnit() && boxSize > 0) " (%${stockPercentage()})" else ""
            "🔴 $name stoğu kritik seviyede$percentMsg - ${daysRemainingInStock()} gün kaldı!"
        }
        isStockLow() -> {
            val percentMsg = if (isCountableUnit() && boxSize > 0) " (%${stockPercentage()})" else ""
            "🟡 $name stoğu azaldı$percentMsg - ${daysRemainingInStock()} gün kaldı."
        }
        else -> null
    }
}

/**
 * Stok seviyesi rengi
 */
fun Medicine.getStockLevelColor(): String {
    return when {
        isStockEmpty() -> "#F44336" // Red
        isStockCritical() -> "#FF5722" // Deep Orange
        isStockLow() -> "#FF9800" // Orange
        else -> "#4CAF50" // Green
    }
}

/**
 * Stoğu azalt ve yeni Medicine döndür
 */
fun Medicine.decrementStock(amount: Double = dosage.toDoubleOrNull() ?: 1.0): Medicine {
    if (!autoDecrementEnabled || stockCount <= 0) return this
    
    val newCount = (stockCount - amount.toInt()).coerceAtLeast(0)
    return this.copy(
        stockCount = newCount,
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Stoğu artır (yeni kutu ekleme)
 */
fun Medicine.addStock(amount: Int): Medicine {
    return this.copy(
        stockCount = stockCount + amount,
        lastRestockDate = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
