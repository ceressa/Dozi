package com.bardino.dozi.core.voice

import java.util.Locale

data class ParsedMedicineCommand(
    val name: String,          // İlaç adı (Parol)
    val dose: String?,         // Doz (500 mg / 1 tablet) - opsiyonel
    val hour: Int,             // 0..23
    val minute: Int = 0,       // varsayılan 00
    val frequency: String? = null // "her gün", "pazartesi", vb. - opsiyonel
)

/**
 * “sabah 9'da Parol 500 mg alacağım”, “akşam 8:30'da Coraspin” gibi Türkçe komutları çözer.
 * Minimal, pratik regex tabanlı ayrıştırıcı. Gerekirse ileride NLP ile güçlendir.
 */
fun parseVoiceCommand(raw: String): ParsedMedicineCommand? {
    val text = raw.lowercase(Locale("tr", "TR")).trim()

    // Saat ve dakika (09:00, 9.00, 9, 8:30 vb.)
    val timeRegex = Regex("""(?:(sabah|akşam)\s*)?(\d{1,2})(?::|\.|’|')?(\d{2})?""")
    val timeMatch = timeRegex.find(text)
    val meridiem = timeMatch?.groups?.get(1)?.value  // sabah | akşam
    val hourRaw = timeMatch?.groups?.get(2)?.value?.toIntOrNull()
    val minuteRaw = timeMatch?.groups?.get(3)?.value?.toIntOrNull() ?: 0

    val hour24 = when {
        hourRaw == null -> null
        meridiem == "akşam" && hourRaw in 1..11 -> hourRaw + 12
        else -> hourRaw
    }?.coerceIn(0, 23)

    // Doz (500 mg | 1 tablet | 5 ml vb.)
    val doseRegex = Regex("""(\d+\s?(?:mg|ml))|(\d+\s?tablet)|(\d+\s?damla)""")
    val dose = doseRegex.find(text)?.value?.replace("\\s+".toRegex(), " ")?.trim()

    // Sıklık (her gün, pazartesi, haftada 2 vb.) – şimdilik sadece örnek alan
    val freqRegex = Regex("""(her gün|hergun|pazartesi|salı|çarşamba|perşembe|cuma|cumartesi|pazar|haftada\s*\d+)""")
    val freq = freqRegex.find(text)?.value

    // İlaç adı: en zayıf halka; lookup ekranında eşleştireceğin için metinden kalan parçadan bulacağız.
    // Basit yaklaşım: saat/doz/frekans kısımlarını ayıkla, kalan kelimeler ilaç adı adayımız.
    val stripped = text
        .replace(timeRegex, " ")
        .replace(doseRegex, " ")
        .replace(freqRegex, " ")
        .replace("""alacağım|alcam|alırım|hatırlat|hatirlat""".toRegex(), " ")
        .replace("""^\s+|\s+$""".toRegex(), " ")
        .replace("""\s{2,}""".toRegex(), " ")
        .trim()

    val nameCandidate = stripped.split(" ").filter { it.isNotBlank() }.joinToString(" ").trim()

    return if (!nameCandidate.isNullOrBlank() && hour24 != null) {
        ParsedMedicineCommand(
            name = nameCandidate.replaceFirstChar { it.titlecase(Locale("tr","TR")) },
            dose = dose?.replaceFirstChar { it.titlecase(Locale("tr","TR")) },
            hour = hour24,
            minute = minuteRaw,
            frequency = freq
        )
    } else null
}
