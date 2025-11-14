package com.bardino.dozi.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ════════════════════════════════════════════════════════════════════════════
 * 🎨 DOZI TEMA TANIMLARI
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Material 3 ColorScheme tanımları
 * Yeni pastel-canlı renk paletini kullanır
 *
 * Ana Renkler:
 * • Primary: Lavender (Mor) - Ana tema rengi
 * • Secondary: Coral (Pembe-Coral) - İkincil vurgu
 * • Tertiary: Amber - Ek vurgular
 */

// ═════════════════════════════════════════════════════════════════════════════
// 🌞 LIGHT COLOR SCHEME (Aydınlık Mod)
// ═════════════════════════════════════════════════════════════════════════════

private val LightColorScheme = lightColorScheme(
    // Primary renk ailesi - Lavender (Mor)
    primary = DoziPrimary,                          // Ana lavender
    onPrimary = Color.White,                        // Lavender üzerindeki yazılar beyaz
    primaryContainer = DoziPrimaryLight,            // Açık lavender container'lar
    onPrimaryContainer = DoziPrimaryDark,           // Container içi yazılar koyu mor

    // Secondary renk ailesi - Coral (Pembe)
    secondary = DoziSecondary,                      // Ana coral
    onSecondary = Color.White,                      // Coral üzerindeki yazılar beyaz
    secondaryContainer = DoziSecondaryLight,        // Açık coral container'lar
    onSecondaryContainer = DoziSecondaryDark,       // Container içi yazılar koyu coral

    // Tertiary renk ailesi - Amber (Sarı-Turuncu)
    tertiary = DoziAccent,                          // Ana amber
    onTertiary = Color.White,                       // Amber üzerindeki yazılar beyaz
    tertiaryContainer = DoziAccentLight,            // Açık amber container'lar
    onTertiaryContainer = DoziAccentDark,           // Container içi yazılar koyu amber

    // Error renk ailesi
    error = ErrorRed,                               // Soft kırmızı - yaşlılar için agresif değil
    onError = Color.White,                          // Hata üzerindeki yazılar beyaz
    errorContainer = ErrorRed.copy(alpha = 0.1f),   // Açık hata arka planı
    onErrorContainer = ErrorRed,                    // Hata container yazıları

    // Background ve Surface - Pastel & Canlı
    background = BackgroundLight,                   // Ana arka plan - belirgin lavanta
    onBackground = TextPrimary,                     // Arka plan üzerindeki yazılar

    surface = SurfaceLight,                         // Kartlar - beyaz
    onSurface = TextPrimary,                        // Kart üzerindeki yazılar
    surfaceVariant = SurfaceLavender,               // Varyant surface - lavender tint
    onSurfaceVariant = TextSecondary,               // Varyant surface yazıları

    // Outline ve diğerleri
    outline = DoziPrimaryLight,                     // Kenarlıklar - açık lavender
    outlineVariant = Gray300,                       // Alternatif kenarlıklar

    surfaceTint = DoziPrimary,                      // Surface tint - lavender
    inverseSurface = Gray900,                       // Ters surface (dark)
    inverseOnSurface = Color.White,                 // Ters surface yazıları
    inversePrimary = DoziPrimaryLight,              // Ters primary
)

// ═════════════════════════════════════════════════════════════════════════════
// 🌙 DARK COLOR SCHEME (Karanlık Mod)
// ═════════════════════════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    // Primary renk ailesi - Lavender (daha açık tonlar dark mode için)
    primary = DoziPrimaryLight,                     // Açık lavender (dark mode'da daha görünür)
    onPrimary = Gray900,                            // Lavender üzerindeki yazılar koyu
    primaryContainer = DoziPrimaryDark,             // Koyu lavender container'lar
    onPrimaryContainer = DoziPrimaryLight,          // Container içi yazılar açık

    // Secondary renk ailesi - Coral
    secondary = DoziSecondaryLight,                 // Açık coral (dark mode'da daha görünür)
    onSecondary = Gray900,                          // Coral üzerindeki yazılar koyu
    secondaryContainer = DoziSecondaryDark,         // Koyu coral container'lar
    onSecondaryContainer = DoziSecondaryLight,      // Container içi yazılar açık

    // Tertiary renk ailesi - Amber
    tertiary = DoziAccentLight,                     // Açık amber
    onTertiary = Gray900,                           // Amber üzerindeki yazılar koyu
    tertiaryContainer = DoziAccentDark,             // Koyu amber container'lar
    onTertiaryContainer = DoziAccentLight,          // Container içi yazılar açık

    // Error renk ailesi
    error = ErrorRed,                               // Soft kırmızı
    onError = Color.White,                          // Hata üzerindeki yazılar beyaz
    errorContainer = ErrorRed.copy(alpha = 0.2f),   // Hafif kırmızı arka plan
    onErrorContainer = ErrorRed,                    // Hata container yazıları

    // Background ve Surface - Dark Mode
    background = BackgroundDark,                    // Koyu arka plan
    onBackground = TextPrimaryDark,                 // Arka plan üzerindeki yazılar açık

    surface = SurfaceDark,                          // Kartlar - koyu gri
    onSurface = TextPrimaryDark,                    // Kart üzerindeki yazılar açık
    surfaceVariant = SurfaceDarkElevated,           // Varyant surface - daha açık koyu
    onSurfaceVariant = TextSecondaryDark,           // Varyant surface yazıları

    // Outline ve diğerleri
    outline = Color(0xFF383838),                    // Kenarlıklar - koyu gri
    outlineVariant = Gray700,                       // Alternatif kenarlıklar

    surfaceTint = DoziPrimaryLight,                 // Surface tint - açık lavender
    inverseSurface = Color.White,                   // Ters surface (light)
    inverseOnSurface = Gray900,                     // Ters surface yazıları
    inversePrimary = DoziPrimaryDark,               // Ters primary
)

// ═════════════════════════════════════════════════════════════════════════════
// 🎨 DOZI APP THEME (Ana Tema Composable)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Dozi uygulamasının ana tema composable'ı
 *
 * @param darkTheme Karanlık mod aktif mi? (varsayılan: sistem ayarı)
 * @param content İçerik composable'ı
 */
@Composable
fun DoziAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DoziTypography,    // Typography tanımı (ayrı dosyada)
        shapes = DoziShapes,            // Shapes tanımı (ayrı dosyada)
        content = content
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// 🎨 EK YARDIMCI ÖZELLİKLER (Extension Properties)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Yükseltilmiş surface rengi
 * Kartların üzerindeki kartlar, dropdown'lar vb. için
 */
val ColorScheme.surfaceElevated: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        // Light mode - hafif amber glow
        SurfaceElevated
    } else {
        // Dark mode - daha açık koyu gri
        SurfaceDarkElevated
    }

/**
 * Pembe tintli surface
 * Özel vurgu alanları için
 */
val ColorScheme.surfacePink: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        SurfaceTinted // Light mode
    } else {
        Color(0xFF2C1B2E) // Dark mode - koyu pembe
    }

/**
 * Arka plan varyantı (sıcak ton)
 * Alternatif arka planlar için
 */
val ColorScheme.backgroundWarm: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        BackgroundWarm // Light mode - peach
    } else {
        Color(0xFF1A1616) // Dark mode - ılık koyu
    }

/**
 * Renk luminance'ı (parlaklık seviyesi)
 * İç kullanım için yardımcı fonksiyon
 */
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
