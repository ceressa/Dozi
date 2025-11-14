package com.bardino.dozi.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * ════════════════════════════════════════════════════════════════════════════
 * 🎨 DOZI SHAPE PALETİ - Extra Soft Rounded Design
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Dozi uygulamasının tüm köşe yuvarlaklık değerlerini tanımlar.
 *
 * 🏥 TASARIM PRENSİPLERİ:
 * • Extra Soft - Çok yumuşak köşeler, sert kenarlar yok
 * • Dostane ve modern görünüm
 * • Sağlık uygulaması için profesyonel ama sıcak
 * • Yaşlılar için kolay algılanır
 * • Hamileler için rahatlatıcı
 *
 * 📘 KULLANIM REHBERİ:
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Extra Small (16dp)  → Chips, badges, küçük indicator'lar
 * Small (20dp)        → Text fields, küçük butonlar, tags
 * Medium (28dp)       → Butonlar, standart kartlar, input'lar
 * Large (36dp)        → Büyük kartlar, dialog'lar, modal'lar
 * Extra Large (48dp)  → Bottom sheets, hero sections, özel kartlar
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */

// ═════════════════════════════════════════════════════════════════════════════
// 🎨 MATERIAL 3 STANDART SHAPES (Extra Soft)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Material 3 Shapes tanımları - Extra Soft yaklaşımı
 * Tüm Material 3 bileşenleri bu değerleri kullanır
 */
val DoziShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),     // Chips, badges, küçük elementler
    small = RoundedCornerShape(20.dp),          // Text fields, küçük butonlar
    medium = RoundedCornerShape(28.dp),         // Butonlar, kartlar - EN ÇOK KULLANILAN
    large = RoundedCornerShape(36.dp),          // Dialog'lar, büyük kartlar
    extraLarge = RoundedCornerShape(48.dp)      // Bottom sheets, hero sections
)

// ═════════════════════════════════════════════════════════════════════════════
// 🎯 ÖZEL SHAPE TANIMLARI (Component-Specific)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Buton Shapes - Farklı buton tipleri için
 */
val ButtonShapePrimary = RoundedCornerShape(28.dp)          // Ana butonlar - extra soft
val ButtonShapeSecondary = RoundedCornerShape(24.dp)        // İkincil butonlar - biraz daha az
val ButtonShapeSmall = RoundedCornerShape(20.dp)            // Küçük butonlar
val ButtonShapeLarge = RoundedCornerShape(32.dp)            // Büyük CTA butonları
val ButtonShapePill = RoundedCornerShape(percent = 50)      // Pill style (tamamen yuvarlak)

/**
 * Kart Shapes - Farklı kart tipleri için
 */
val CardShapeDefault = RoundedCornerShape(28.dp)            // Standart kartlar
val CardShapeSmall = RoundedCornerShape(20.dp)              // Küçük kartlar, list items
val CardShapeLarge = RoundedCornerShape(36.dp)              // Büyük kartlar, featured items
val CardShapeHero = RoundedCornerShape(40.dp)               // Hero kartlar, splash screens

/**
 * Input/TextField Shapes
 */
val TextFieldShape = RoundedCornerShape(20.dp)              // Input alanları - rahat yazma
val TextFieldShapeLarge = RoundedCornerShape(24.dp)         // Büyük input alanları (search)

/**
 * Dialog & Modal Shapes
 */
val DialogShape = RoundedCornerShape(36.dp)                 // Dialog pencereler
val BottomSheetShape = RoundedCornerShape(
    topStart = 48.dp,                                       // Bottom sheet - sadece üst köşeler
    topEnd = 48.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)
val ModalShape = RoundedCornerShape(40.dp)                  // Modal pencereler

/**
 * Özel Kullanım Shapes
 */
val ShapeNone = RoundedCornerShape(0.dp)                    // Köşesiz (image backgrounds)
val ShapePill = RoundedCornerShape(percent = 50)            // Tamamen yuvarlak (pills, FAB)
val ShapeCircle = RoundedCornerShape(percent = 50)          // Daireler için alias

/**
 * Sadece üst köşeler yuvarlatılmış - HomeScreen için
 */
val ShapeTopOnly = RoundedCornerShape(
    topStart = 40.dp,                                       // Daha yumuşak üst köşeler
    topEnd = 40.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

/**
 * Sadece alt köşeler yuvarlatılmış
 */
val ShapeBottomOnly = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 40.dp,
    bottomEnd = 40.dp
)

/**
 * Asimetrik yuvarlaklık - özel tasarımlar için
 */
val ShapeAsymmetric = RoundedCornerShape(
    topStart = 48.dp,                                       // Sol üst çok yumuşak
    topEnd = 24.dp,                                         // Sağ üst orta
    bottomStart = 24.dp,                                    // Sol alt orta
    bottomEnd = 48.dp                                       // Sağ alt çok yumuşak
)

// ═════════════════════════════════════════════════════════════════════════════
// 🏥 SAĞLIK UYGULAMASI ÖZEL SHAPES
// ═════════════════════════════════════════════════════════════════════════════

/**
 * İlaç kartları - ekstra yumuşak ve dostane
 */
val MedicineCardShape = RoundedCornerShape(32.dp)

/**
 * Bildirim kartları - dikkat çekici ama yumuşak
 */
val NotificationCardShape = RoundedCornerShape(24.dp)

/**
 * Profil avatarları - yuvarlak
 */
val AvatarShape = RoundedCornerShape(percent = 50)

/**
 * Ölçüm kartları (tansiyon, nabız vb.)
 */
val MetricCardShape = RoundedCornerShape(28.dp)

/**
 * Takvim gün seçimi
 */
val CalendarDayShape = RoundedCornerShape(16.dp)

/**
 * Badge/Etiket shapes
 */
val BadgeShape = RoundedCornerShape(12.dp)                  // Küçük badge'ler
val BadgeShapePill = RoundedCornerShape(percent = 50)       // Pill style badge'ler

// ═════════════════════════════════════════════════════════════════════════════
// 📚 KULLANIM ÖRNEKLERİ (Usage Examples)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * DOĞRU KULLANIM ✅:
 *
 * // Primary button - extra soft
 * Button(
 *     onClick = { },
 *     shape = ButtonShapePrimary  // veya MaterialTheme.shapes.medium
 * ) { Text("Kaydet") }
 *
 * // İlaç kartı
 * Card(
 *     shape = MedicineCardShape,
 *     modifier = Modifier.fillMaxWidth()
 * ) { ... }
 *
 * // Text input - rahat yazma için yumuşak
 * TextField(
 *     value = "",
 *     onValueChange = {},
 *     shape = TextFieldShape
 * )
 *
 * // Dialog - profesyonel ama dostane
 * AlertDialog(
 *     onDismissRequest = {},
 *     shape = DialogShape
 * ) { ... }
 *
 * // Bottom sheet - çok yumuşak üst köşeler
 * ModalBottomSheet(
 *     onDismissRequest = {},
 *     shape = BottomSheetShape
 * ) { ... }
 *
 * // Profil avatarı - tamamen yuvarlak
 * Image(
 *     painter = painterResource(R.drawable.avatar),
 *     modifier = Modifier
 *         .size(64.dp)
 *         .clip(AvatarShape)
 * )
 *
 * // FAB (Floating Action Button) - pill shape
 * FloatingActionButton(
 *     onClick = {},
 *     shape = ShapePill
 * ) { Icon(...) }
 *
 *
 * YANLIŞ KULLANIM ❌:
 *
 * // Köşesiz buton (sert görünür)
 * Button(
 *     onClick = {},
 *     shape = ShapeNone  // ❌ YANLIŞ - dostane değil
 * )
 *
 * // Çok küçük radius (material 2 tarzı)
 * Card(
 *     shape = RoundedCornerShape(4.dp)  // ❌ YANLIŞ - çok sert
 * )
 *
 *
 * 💡 İPUCU:
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Material 3 bileşenleri otomatik olarak MaterialTheme.shapes değerlerini kullanır:
 * - Button → shapes.medium (28dp) ✅
 * - Card → shapes.medium (28dp) ✅
 * - TextField → shapes.small (20dp) ✅
 *
 * Özel shape kullanmak istiyorsanız, shape parametresini override edin.
 */
