package com.bardino.dozi.core.data.model

/**
 * Firestore-compatible Medicine data model
 * Represents a medicine with its schedule and dosage information
 */
data class Medicine(
    val id: String = "",
    val userId: String = "",                    // Firebase Auth UID
    val ownerProfileId: String? = null,         // Profile ID - null ise ana profilde gösterilir
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
    val stockCount: Int = 0,
    val boxSize: Int = 0,
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
    val selectedDates: List<String> = emptyList()
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
