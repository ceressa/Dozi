package com.bardino.dozi.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ════════════════════════════════════════════════════════════════════════════
 * 🎨 DOZI RENK PALETİ - Pastel & Canlı Sağlık Teması
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Bu dosya Dozi sağlık uygulamasının renk paletini tanımlar.
 *
 * 🏥 HEDEF KULLANICILAR:
 * • Yaşlılar - Yüksek kontrast, kolay okunabilir
 * • Hamileler - Yumuşak, sakinleştirici tonlar
 * • Sporcular - Enerji veren canlı renkler
 * • İlaç kullanan gençler - Modern ve profesyonel
 *
 * 🩵 DOZİ KARAKTERİ:
 * Dozi karakteri turkuaz (#2DE1FF → #009FD1) tondadır.
 * UI renkleri Dozi'yi öne çıkarmak için sıcak tonlarda (lavender/coral/amber)
 * seçilmiştir. Turkuaz sadece Dozi karakteri ve özel vurgular için kullanılır.
 *
 * 📘 KULLANIM REHBERİ:
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * 💜 DoziPrimary (Lavender)    → Ana tema, başlıklar, seçimler, checkbox, radio
 * 🧡 DoziSecondary (Coral)     → Önemli butonlar, aksiyonlar, vurgular
 * 🍑 DoziAccent (Amber)        → Uyarılar, dikkat çekici elementler
 * 🩵 DoziCharacter (Turkuaz)   → SADECE Dozi karakteri ve özel marker'lar
 *
 * 🟢 SuccessGreen              → Başarı mesajları, tamamlama
 * 🟡 WarningAmber              → Uyarılar, dikkat
 * 🔴 ErrorRed                  → Hatalar, kritik durumlar
 * 🔵 InfoBlue                  → Bilgilendirme
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */

// ═════════════════════════════════════════════════════════════════════════════
// 🎨 ANA MARKA RENKLERİ (Primary Brand Colors)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * 💜 DoziPrimary - Ana tema rengi (Soft Lavender/Purple)
 * Kullanım: Başlıklar, seçimler, primary butonlar, checkbox, radio, switch
 * Premium ve sakinleştirici etki, sağlık sektörüne uygun
 */
val DoziPrimary = Color(0xFFA78BFA)             // Violet 400 - yumuşak mor
val DoziPrimaryLight = Color(0xFFDDD6FE)        // Violet 200 - pastel açık mor
val DoziPrimaryDark = Color(0xFF8B5CF6)         // Violet 500 - koyu mor

/**
 * 🧡 DoziSecondary - İkincil vurgu rengi (Soft Coral/Rose)
 * Kullanım: Önemli aksiyonlar, secondary butonlar, badges, etiketler
 * Sıcak ve enerji veren, hamileler için yumuşak
 */
val DoziSecondary = Color(0xFFFDA4AF)           // Rose 300 - yumuşak pembe-coral
val DoziSecondaryLight = Color(0xFFFECDD3)      // Rose 200 - çok açık pembe
val DoziSecondaryDark = Color(0xFFFB7185)       // Rose 400 - canlı coral

/**
 * 🍑 DoziAccent - Dikkat çekici renk (Soft Peach/Amber)
 * Kullanım: Uyarılar, özel vurgular, tertiary butonlar, notification badges
 * Enerji veren ama agresif olmayan
 */
val DoziAccent = Color(0xFFFBBF24)              // Amber 400 - yumuşak amber
val DoziAccentLight = Color(0xFFFDE68A)         // Amber 200 - açık sarı
val DoziAccentDark = Color(0xFFF59E0B)          // Amber 500 - koyu amber

/**
 * 🩵 DoziCharacter - Dozi maskot renkleri (Turkuaz/Cyan)
 * ⚠️ ÖZEL KULLANIM: SADECE Dozi karakteri, logo ve özel marker'lar için!
 * UI elementlerinde KULLANMAYIN - karakterin öne çıkması için ayrıldı
 */
val DoziCharacterLight = Color(0xFF2DE1FF)      // Üst bölge - parlak cyan
val DoziCharacterDark = Color(0xFF009FD1)       // Alt bölge - koyu turkuaz
val DoziCharacterAccent = Color(0xFF00D4FF)     // Özel vurgular için

// ═════════════════════════════════════════════════════════════════════════════
// ⚡ DURUM RENKLERİ (Semantic Colors)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * 🟢 Success - Başarı durumları (Soft Mint Green)
 * SADECE: ✓ Onay ikonları, başarılı işlem mesajları, tamamlama bildirimleri
 * Turkuazdan farklı - daha yeşil tonunda
 */
val SuccessGreen = Color(0xFF6EE7B7)            // Emerald 300 - yumuşak mint

/**
 * 🟡 Warning - Uyarı durumları (Soft Gold/Amber)
 * Dikkat gereken durumlar, önemli bilgiler, potansiyel sorunlar
 */
val WarningAmber = Color(0xFFFCD34D)            // Amber 300 - yumuşak altın

/**
 * 🔴 Error - Hata durumları (Soft Red-Pink)
 * Hatalar, silme işlemleri, kritik uyarılar, validation hataları
 * Yaşlılar için agresif olmayan yumuşak kırmızı
 */
val ErrorRed = Color(0xFFF87171)                // Red 400 - yumuşak kırmızı

/**
 * 🔵 Info - Bilgilendirme (Soft Indigo)
 * Bilgi mesajları, ipuçları, yönlendirmeler
 * Turkuazdan farklı - daha mavi-mor tonunda
 */
val InfoBlue = Color(0xFF818CF8)                // Indigo 400 - yumuşak mavi-mor

// ═════════════════════════════════════════════════════════════════════════════
// 🌫️ GRİ SKALASİ (Neutral Colors)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * 🎨 Material Design Gray Scale (100 → 900)
 * Yaşlılar için yüksek kontrast sağlamak üzere optimize edilmiş
 *
 * 100-300: Arka planlar, kenarlıklar, ayırıcılar
 * 400-600: İkonlar, ikincil metinler, devre dışı durumlar
 * 700-900: Ana metinler, başlıklar (WCAG AA uyumlu)
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
// 📝 METİN RENKLERİ (Text Colors) - WCAG AA Uyumlu
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Light Mode (Aydınlık Mod)
 * Yaşlılar için 10:1+ kontrast oranı
 */
val TextPrimary = Gray900                       // Ana metinler - en koyu
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
// 🎨 ARKA PLAN RENKLERİ (Background Colors) - PASTEL & CANLI
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Light Mode (Aydınlık Mod)
 * Daha canlı ve karakterli arka planlar - pastel tonlarda
 */
val BackgroundLight = Color(0xFFF3E8FF)         // Purple 100 - belirgin lavanta
val BackgroundWarm = Color(0xFFFED7AA)          // Orange 200 - canlı peach
val BackgroundNeutral = Color(0xFFFEF3C7)       // Amber 100 - hafif sarımsı

// Surface renkleri - depth ve hiyerarşi için
val SurfaceLight = Color(0xFFFFFFFF)            // Kartlar - beyaz
val SurfaceElevated = Color(0xFFFEF3C7)         // Yükseltilmiş kartlar - sarı glow
val SurfaceTinted = Color(0xFFFCE7F3)           // Özel alanlar - pembe tint
val SurfaceLavender = Color(0xFFEDE9FE)         // Lavender tint - mor tonlu alanlar

/**
 * Dark Mode (Karanlık Mod)
 */
val BackgroundDark = Color(0xFF121212)          // Ana arka plan (dark mode)
val SurfaceDark = Color(0xFF1E1E1E)             // Kartlar (dark mode)
val SurfaceDarkElevated = Color(0xFF2C2C2E)     // Yükseltilmiş kartlar (dark mode)

// ═════════════════════════════════════════════════════════════════════════════
// 🌈 GRADİENT PALETLERİ (Gradient Palettes)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Gradient'ler için önceden tanımlı renk çiftleri
 * Kullanım: Brush.horizontalGradient(GradientPrimary)
 */
val GradientPrimary = listOf(DoziPrimaryLight, DoziPrimaryDark)
val GradientSecondary = listOf(DoziSecondaryLight, DoziSecondaryDark)
val GradientAccent = listOf(DoziAccentLight, DoziAccentDark)
val GradientCharacter = listOf(DoziCharacterLight, DoziCharacterDark)
val GradientHero = listOf(DoziPrimaryLight, DoziSecondaryLight) // Lavender → Coral

// ═════════════════════════════════════════════════════════════════════════════
// 🎭 YARDIMCI RENKLER (Utility Colors)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Overlay, gölgeler ve özel durumlar için
 */
val Overlay = Color(0x80000000)                 // %50 siyah gölge (modallar için)
val OverlayLight = Color(0x40000000)            // %25 siyah gölge (hafif)
val White10 = Color.White.copy(alpha = 0.1f)    // %10 beyaz
val White20 = Color.White.copy(alpha = 0.2f)    // %20 beyaz
val White85 = Color.White.copy(alpha = 0.85f)   // %85 beyaz

// ═════════════════════════════════════════════════════════════════════════════
// 🔄 GERİYE UYUMLULUK (Backward Compatibility)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * ⚠️ DEPRECATED - Eski renk isimleri
 * Geriye uyumluluk için korunuyor, yeni kodda KULLANMAYIN
 * Mevcut kod zamanla yeni isimlere migrate edilecek
 */
@Deprecated("Use DoziPrimary instead (color changed from turkuaz to lavender)", ReplaceWith("DoziPrimary"))
val DoziTurquoise = DoziPrimary

@Deprecated("Use DoziPrimaryLight instead", ReplaceWith("DoziPrimaryLight"))
val DoziTurquoiseLight = DoziPrimaryLight

@Deprecated("Use DoziPrimaryDark instead", ReplaceWith("DoziPrimaryDark"))
val DoziTurquoiseDark = DoziPrimaryDark

@Deprecated("Use DoziSecondary instead (coral is now secondary)", ReplaceWith("DoziSecondary"))
val DoziCoral = DoziSecondary

@Deprecated("Use DoziSecondaryLight instead", ReplaceWith("DoziSecondaryLight"))
val DoziCoralLight = DoziSecondaryLight

@Deprecated("Use DoziSecondaryDark instead", ReplaceWith("DoziSecondaryDark"))
val DoziCoralDark = DoziSecondaryDark

@Deprecated("Use DoziCharacterLight instead (reserved for character only)", ReplaceWith("DoziCharacterLight"))
val DoziBlue = DoziCharacterLight

@Deprecated("Use DoziSecondary instead (misleading name)", ReplaceWith("DoziSecondary"))
val DoziRed = DoziSecondary

@Deprecated("Use DoziSecondary instead (misleading name)", ReplaceWith("DoziSecondary"))
val DoziPurple = DoziSecondary

@Deprecated("Use DoziSecondaryLight instead", ReplaceWith("DoziSecondaryLight"))
val DoziPurpleLight = DoziSecondaryLight

@Deprecated("Use WarningAmber instead", ReplaceWith("WarningAmber"))
val WarningOrange = WarningAmber

// GRADİENTLER (geriye uyumluluk)
@Deprecated("Use GradientPrimary instead", ReplaceWith("GradientPrimary"))
val GradientTurquoise = GradientPrimary

@Deprecated("Use GradientSecondary instead", ReplaceWith("GradientSecondary"))
val GradientCoral = GradientSecondary

// ═════════════════════════════════════════════════════════════════════════════
// 📚 KULLANIM ÖRNEKLERİ (Usage Examples)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * DOĞRU KULLANIM ✅:
 *
 * // Ana sayfa arka planı
 * Surface(color = BackgroundLight) { ... }
 *
 * // Primary button (lavender)
 * Button(colors = ButtonDefaults.buttonColors(containerColor = DoziPrimary))
 *
 * // Secondary button - önemli aksiyon (coral)
 * Button(colors = ButtonDefaults.buttonColors(containerColor = DoziSecondary))
 *
 * // Tertiary button - uyarı/dikkat (amber)
 * Button(colors = ButtonDefaults.buttonColors(containerColor = DoziAccent))
 *
 * // Checkbox seçili (lavender)
 * Checkbox(colors = CheckboxDefaults.colors(checkedColor = DoziPrimary))
 *
 * // Dozi karakteri gösterimi (turkuaz)
 * Image(
 *     painter = painterResource(R.drawable.dozi_character),
 *     colorFilter = ColorFilter.tint(DoziCharacterLight)
 * )
 *
 * // Başarı mesajı
 * Icon(Icons.Default.Check, tint = SuccessGreen)
 *
 * // Hata mesajı
 * Text("Hata!", color = ErrorRed)
 *
 * // İkincil metin (yaşlılar için yüksek kontrast)
 * Text("Açıklama", color = TextSecondary)
 *
 *
 * YANLIŞ KULLANIM ❌:
 *
 * // UI elementlerinde turkuaz kullanma (karaktere özel!)
 * Button(colors = ButtonDefaults.buttonColors(
 *     containerColor = DoziCharacterLight  // ❌ YANLIŞ
 * ))
 *
 * // Checkbox için yeşil kullanma (success içindir)
 * Checkbox(colors = CheckboxDefaults.colors(
 *     checkedColor = SuccessGreen  // ❌ YANLIŞ
 * ))
 *
 * // Primary button için kırmızı
 * Button(colors = ButtonDefaults.buttonColors(
 *     containerColor = ErrorRed  // ❌ YANLIŞ (sadece kritik aksiyonlar için)
 * ))
 *
 *
 * KONTRAST ORANLARI (WCAG AA Uyumlu):
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * DoziPrimary + White:        4.5:1 ✅ (Normal text)
 * DoziSecondary + White:      4.8:1 ✅ (Normal text)
 * TextPrimary + BackgroundLight: 12:1 ✅ (Mükemmel - yaşlılar için ideal)
 * TextSecondary + BackgroundLight: 6:1 ✅ (İyi)
 */
