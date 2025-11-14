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
    val stockCount: Int = 0,
    val boxSize: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reminderEnabled: Boolean = true,
    val icon: String = "💊",                    // Emoji icon for visual display

    // 🤝 Buddy sistem için yeni alanlar
    val sharedWithBuddies: List<String> = emptyList(), // Paylaşılan buddy userId'leri
    val barcode: String? = null,                // Barkod/QR kod
    val imageUrl: String? = null,               // İlaç fotoğrafı
    val manufacturer: String? = null,           // Üretici firma
    val activeIngredient: String? = null        // Etken madde
)
