package com.bardino.dozi.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ════════════════════════════════════════════════════════════════════════════
 * 🎨 DOZI RENK PALETİ
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Bu dosya Dozi uygulamasının tüm renk paletini tanımlar.
 * Tutarlılık için aşağıdaki kullanım rehberine uyun:
 *
 * 📘 KULLANIM REHBERİ:
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * 🩵 DoziTurquoise    → Ana tema rengi, seçimler, vurgular, butonlar, linkler
 * 🔴 DoziCoral        → İkincil vurgu rengi, dikkat çekmek için (butonlar, badges)
 * 🔵 DoziBlue         → Bilgilendirme, saat/zaman gösterimleri
 *
 * 🟢 SuccessGreen     → SADECE başarı mesajları (✓ onay, tamamlama)
 * 🟠 WarningOrange    → Uyarılar, dikkat gerektiren durumlar
 * 🔴 ErrorRed         → Hatalar, silme işlemleri, kritik uyarılar
 * 🔵 InfoBlue         → Bilgilendirme mesajları
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */

// ═════════════════════════════════════════════════════════════════════════════
// 🎨 ANA MARKA RENKLERİ (Primary Brand Colors)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * 🩵 DoziTurquoise - Ana tema rengi
 * Kullanım: Seçimler, vurgular, primary butonlar, checkbox, radio, switch
 */
val DoziTurquoise = Color(0xFF66E6EB)           // Ana turkuaz
val DoziTurquoiseLight = Color(0xFF99EFF2)      // Açık varyant (hover, backgrounds)
val DoziTurquoiseDark = Color(0xFF26C6DA)       // Koyu varyant (pressed, borders)

/**
 * 🔴 DoziCoral - İkincil vurgu rengi
 * Kullanım: Dikkat çekici butonlar, önemli aksiyonlar, badges, etiketler
 */
val DoziCoral = Color(0xFFFF6B6B)               // Ana coral/kırmızı
val DoziCoralLight = Color(0xFFFF9999)          // Açık varyant
val DoziCoralDark = Color(0xFFFF5252)           // Koyu varyant

/**
 * 🔵 DoziBlue - Bilgilendirme rengi
 * Kullanım: Zaman/saat gösterimleri, bilgi kartları, soğuk tonlu elementler
 */
val DoziBlue = Color(0xFF5DD9E2)                // Açık mavi-turkuaz

// ═════════════════════════════════════════════════════════════════════════════
// ⚡ DURUM RENKLERİ (Semantic Colors)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * 🟢 Success - Başarı durumları
 * SADECE: ✓ Onay ikonları, başarılı işlem mesajları, tamamlama bildirimleri
 */
val SuccessGreen = Color(0xFF66BB6A)            // Yeşil - başarı

/**
 * 🟠 Warning - Uyarı durumları
 * Dikkat gereken durumlar, önemli bilgiler, potansiyel sorunlar
 */
val WarningOrange = Color(0xFFF59E0B)           // Turuncu - uyarı
val WarningAmber = Color(0xFFFFCA28)            // Amber - alternatif uyarı rengi

/**
 * 🔴 Error - Hata durumları
 * Hatalar, silme işlemleri, kritik uyarılar, validation hataları
 */
val ErrorRed = Color(0xFFEF5350)                // Kırmızı - hata

/**
 * 🔵 Info - Bilgilendirme
 * Bilgi mesajları, ipuçları, yönlendirmeler
 */
val InfoBlue = Color(0xFF3B82F6)                // Mavi - bilgi

// ═════════════════════════════════════════════════════════════════════════════
// 🌫️ GRİ SKALASİ (Neutral Colors)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * 🎨 Material Design Gray Scale (100 → 900)
 * 100-300: Arka planlar, kenarlıklar, ayırıcılar
 * 400-600: İkonlar, ikincil metinler, devre dışı durumlar
 * 700-900: Ana metinler, başlıklar, koyu arka planlar
 */
val Gray100 = Color(0xFFF5F5F5)                 // En açık gri - backgrounds
val Gray200 = Color(0xFFEEEEEE)                 // Açık gri - dividers, borders
val Gray300 = Color(0xFFE0E0E0)                 // Orta-açık gri - disabled backgrounds
val Gray400 = Color(0xFFBDBDBD)                 // Orta gri - disabled text
val Gray500 = Color(0xFF9E9E9E)                 // Orta-koyu gri - placeholders
val Gray600 = Color(0xFF757575)                 // Koyu gri - secondary text
val Gray700 = Color(0xFF616161)                 // Daha koyu - tertiary text
val Gray800 = Color(0xFF424242)                 // Çok koyu - dark mode surfaces
val Gray900 = Color(0xFF212121)                 // En koyu - primary text

// Alias'lar (daha kolay kullanım için)
val VeryLightGray = Gray100
val LightGray = Gray200
val MediumGray = Gray500
val DarkGray = Gray700

// ═════════════════════════════════════════════════════════════════════════════
// 📝 METİN RENKLERİ (Text Colors)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Light Mode (Aydınlık Mod)
 */
val TextPrimary = Gray900                       // Ana metinler
val TextSecondary = Gray600                     // İkincil metinler, açıklamalar
val TextTertiary = Gray500                      // Üçüncül metinler, timestamp'ler

val TextPrimaryLight = TextPrimary              // Alias (geriye uyumluluk)
val TextSecondaryLight = TextSecondary          // Alias (geriye uyumluluk)

/**
 * Dark Mode (Karanlık Mod)
 */
val TextPrimaryDark = Color(0xFFF9FAFB)        // Ana metinler (dark mode)
val TextSecondaryDark = Color(0xFFD1D5DB)      // İkincil metinler (dark mode)

// ═════════════════════════════════════════════════════════════════════════════
// 🎨 ARKA PLAN RENKLERİ (Background Colors)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Light Mode (Aydınlık Mod)
 */
val BackgroundLight = Color(0xFFF9F9FB)         // Ana arka plan
val SurfaceLight = Color(0xFFFFFFFF)            // Kartlar, yüzeyler

/**
 * Dark Mode (Karanlık Mod)
 */
val BackgroundDark = Color(0xFF121212)          // Ana arka plan (dark mode)
val SurfaceDark = Color(0xFF1E1E1E)             // Kartlar (dark mode)

// ═════════════════════════════════════════════════════════════════════════════
// 🌈 GRADİENT PALETLERİ (Gradient Palettes)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Gradient'ler için önceden tanımlı renk çiftleri
 * Kullanım: Brush.horizontalGradient(GradientTurquoise)
 */
val GradientTurquoise = listOf(DoziTurquoiseLight, DoziTurquoiseDark)
val GradientCoral = listOf(DoziCoralLight, DoziCoralDark)
val GradientHero = listOf(DoziTurquoiseLight, DoziTurquoise)

// ═════════════════════════════════════════════════════════════════════════════
// 🎭 YARDIMCI RENKLER (Utility Colors)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Overlay, gölgeler ve özel durumlar için
 */
val Overlay = Color(0x80000000)                 // %50 siyah gölge (modallar için)
val White10 = Color.White.copy(alpha = 0.1f)    // %10 beyaz
val White20 = Color.White.copy(alpha = 0.2f)    // %20 beyaz
val White85 = Color.White.copy(alpha = 0.85f)   // %85 beyaz

// ═════════════════════════════════════════════════════════════════════════════
// 🔄 GERİYE UYUMLULUK (Backward Compatibility)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * ⚠️ DEPRECATED - Geriye uyumluluk için korunuyor
 * Yeni kod yazmayın, mevcut kod zamanla migrated edilecek
 */
@Deprecated("Use DoziCoral instead", ReplaceWith("DoziCoral"))
val DoziRed = DoziCoral

@Deprecated("Use DoziCoral instead (misleading name - it's actually red/coral)", ReplaceWith("DoziCoral"))
val DoziPurple = DoziCoral

@Deprecated("Use DoziCoralLight instead", ReplaceWith("DoziCoralLight"))
val DoziPurpleLight = DoziCoralLight

@Deprecated("Use DoziTurquoise instead", ReplaceWith("DoziTurquoise"))
val DoziPrimary = DoziTurquoise

@Deprecated("Use DoziTurquoiseLight instead", ReplaceWith("DoziTurquoiseLight"))
val DoziPrimaryLight = DoziTurquoiseLight

@Deprecated("Use DoziTurquoiseDark instead", ReplaceWith("DoziTurquoiseDark"))
val DoziPrimaryDark = DoziTurquoiseDark

@Deprecated("Use DoziCoral instead", ReplaceWith("DoziCoral"))
val DoziAccent = DoziCoral

@Deprecated("Use DoziCoralLight instead", ReplaceWith("DoziCoralLight"))
val DoziAccentLight = DoziCoralLight

@Deprecated("Use DoziCoralDark instead", ReplaceWith("DoziCoralDark"))
val DoziAccentDark = DoziCoralDark

// GRADİENTLER (geriye uyumluluk)
@Deprecated("Use GradientTurquoise instead", ReplaceWith("GradientTurquoise"))
val GradientPrimary = GradientTurquoise

@Deprecated("Use GradientCoral instead", ReplaceWith("GradientCoral"))
val GradientAccent = GradientCoral

// ═════════════════════════════════════════════════════════════════════════════
// 📚 KULLANIM ÖRNEKLERİ (Usage Examples)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * DOĞRU KULLANIM ✅:
 *
 * // Primary button
 * Button(colors = ButtonDefaults.buttonColors(containerColor = DoziTurquoise))
 *
 * // Secondary button (önemli aksiyon)
 * Button(colors = ButtonDefaults.buttonColors(containerColor = DoziCoral))
 *
 * // Checkbox seçili
 * Checkbox(colors = CheckboxDefaults.colors(checkedColor = DoziTurquoise))
 *
 * // Başarı mesajı
 * Icon(Icons.Default.Check, tint = SuccessGreen)
 *
 * // Hata mesajı
 * Text("Hata!", color = ErrorRed)
 *
 * // İkincil metin
 * Text("Açıklama", color = TextSecondary)
 *
 *
 * YANLIŞ KULLANIM ❌:
 *
 * // Checkbox için yeşil kullanma
 * Checkbox(colors = CheckboxDefaults.colors(checkedColor = SuccessGreen))  // ❌
 *
 * // Sıklık seçimi için yeşil
 * RadioButton(colors = RadioButtonDefaults.colors(selectedColor = SuccessGreen))  // ❌
 *
 * // Primary button için kırmızı
 * Button(colors = ButtonDefaults.buttonColors(containerColor = ErrorRed))  // ❌
 * (ErrorRed sadece silme gibi kritik aksiyonlar için kullanılabilir)
 */
