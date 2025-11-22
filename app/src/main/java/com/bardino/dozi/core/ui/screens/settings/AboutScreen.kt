package com.bardino.dozi.core.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSupport: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "Hakkında",
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Logo ve Başlık
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(DoziTurquoise.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.dozi),
                    contentDescription = "Dozi",
                    modifier = Modifier.size(70.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Dozi",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sağlıklı Yaşam Asistanın",
                    style = MaterialTheme.typography.bodyLarge,
                    color = DoziTurquoise,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Versiyon $versionName (Build $versionCode)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Açıklama
            Text(
                text = "Dozi, ilaç takibini kolaylaştıran ve sağlığınızı kontrol altında tutmanıza yardımcı olan modern bir sağlık asistanıdır. İlaçlarınızı zamanında almayı unutmayın, ailenizi takip edin ve sağlıklı bir yaşam sürün!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Özellikler
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureItem(
                    icon = Icons.Default.Notifications,
                    title = "Akıllı Hatırlatmalar",
                    description = "İlaçlarınızı zamanında almanız için özelleştirilebilir bildirimler"
                )
                FeatureItem(
                    icon = Icons.Default.MedicalServices,
                    title = "Kolay Takip",
                    description = "İlaçlarınızı, dozlarınızı ve stok durumunuzu tek ekranda görün"
                )
                FeatureItem(
                    icon = Icons.Default.CloudSync,
                    title = "Bulut Yedekleme",
                    description = "Verileriniz güvenle Firebase'de saklanır ve cihazlar arası senkronize olur"
                )
                FeatureItem(
                    icon = Icons.Default.Group,
                    title = "Aile Takibi",
                    description = "Sevdiklerinizin ilaç uyumunu uzaktan kontrol edin"
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Geliştirici Bilgisi
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bardino Technology",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "© 2025 Tüm hakları saklıdır",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Politika Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DoziTurquoise
                    )
                ) {
                    Text("Gizlilik Politikasi")
                }
                OutlinedButton(
                    onClick = { showTermsDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DoziTurquoise
                    )
                ) {
                    Text("Kullanim Sartlari")
                }
            }

            // Destek Butonu
            if (onNavigateToSupport != null) {
                Button(
                    onClick = onNavigateToSupport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DoziTurquoise
                    )
                ) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Destek ve SSS")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Gizlilik Politikası Dialog
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    // Kullanım Şartları Dialog
    if (showTermsDialog) {
        TermsOfServiceDialog(onDismiss = { showTermsDialog = false })
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DoziTurquoise.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DoziTurquoise,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Gizlilik Politikası",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PolicySection(
                    title = "1. Veri Sorumlusu",
                    content = "Dozi uygulaması, Bardino Technology tarafından geliştirilmiş ve işletilmektedir. Kişisel verilerinizin korunması bizim için önceliktir ve 6698 sayılı Kişisel Verilerin Korunması Kanunu'na (KVKK) uygun hareket ediyoruz."
                )

                PolicySection(
                    title = "2. Toplanan Veriler",
                    content = """
                        • Kimlik Bilgileri: Ad, soyad, e-posta adresi
                        • İletişim Bilgileri: Telefon numarası (opsiyonel)
                        • Sağlık Verileri: İlaç isimleri, dozaj bilgileri, kullanım saatleri, stok durumu
                        • Cihaz Bilgileri: Cihaz modeli, işletim sistemi versiyonu, uygulama versiyonu
                        • Kullanım Verileri: Uygulama içi aktiviteler, hata logları
                    """.trimIndent()
                )

                PolicySection(
                    title = "3. Verilerin Kullanım Amaçları",
                    content = """
                        • İlaç hatırlatma bildirimleri göndermek
                        • Kullanıcı hesabınızı oluşturmak ve yönetmek
                        • İlaç takibi ve raporlama hizmetleri sunmak
                        • Aile üyeleri arasında bilgi paylaşımını sağlamak (izniniz dahilinde)
                        • Uygulama performansını iyileştirmek ve hataları gidermek
                        • İstatistiksel analizler yapmak
                        • Yasal yükümlülükleri yerine getirmek
                    """.trimIndent()
                )

                PolicySection(
                    title = "4. Veri Güvenliği",
                    content = """
                        • Tüm verileriniz Google Firebase'de saklanır
                        • SSL/TLS protokolleri ile güvenli veri iletimi sağlanır
                        • Düzenli güvenlik testleri ve güncellemeleri yapılır
                        • Yetkisiz erişimlere karşı çok katmanlı güvenlik önlemleri alınır
                    """.trimIndent()
                )

                PolicySection(
                    title = "5. Veri Paylaşımı",
                    content = "Verileriniz kesinlikle üçüncü şahıslarla satılmaz, kiralanmaz veya paylaşılmaz. Sadece aşağıdaki durumlarda veri paylaşımı yapılabilir:\n\n• Yasal zorunluluklar (mahkeme kararı, savcılık talebi)\n• Firebase ve Google servisleri (altyapı hizmeti sağlayıcısı olarak)\n• Açık izniniz dahilinde aile üyeleriyle"
                )

                PolicySection(
                    title = "6. Haklarınız",
                    content = """
                        KVKK kapsamında aşağıdaki haklara sahipsiniz:
                        • Verilerinizin işlenip işlenmediğini öğrenme
                        • İşlenme amacını ve amacına uygun kullanılıp kullanılmadığını öğrenme
                        • Yurt içinde veya yurt dışında aktarıldığı 3. kişileri bilme
                        • Eksik veya yanlış işlenmişse düzeltilmesini isteme
                        • Verilerin silinmesini veya yok edilmesini talep etme
                        • Otomatik sistemler ile analiz edilmesi sonucu aleyhinize bir sonuç doğması halinde itiraz etme
                    """.trimIndent()
                )

                PolicySection(
                    title = "7. Çerezler ve İzleme",
                    content = "Uygulama performansını ölçmek için Google Analytics ve Firebase Analytics kullanılmaktadır. Bu servisler anonim kullanım verileri toplar. İsterseniz cihaz ayarlarınızdan bu izlemeleri kapatabilirsiniz."
                )

                PolicySection(
                    title = "8. Çocukların Gizliliği",
                    content = "Dozi, 18 yaşından küçük kullanıcıların verilerini bilerek toplamaz. Ebeveyn veya vasi gözetiminde kullanılması önerilir."
                )

                PolicySection(
                    title = "9. Veri Saklama Süresi",
                    content = "Verileriniz, hizmet sunumu için gerekli olduğu sürece veya yasal saklama yükümlülükleri nedeniyle saklanır. Hesabınızı sildiğinizde tüm verileriniz 30 gün içinde kalıcı olarak silinir."
                )

                PolicySection(
                    title = "10. İletişim",
                    content = """
                        Gizlilik politikası ile ilgili sorularınız için:
                        
                        E-posta: privacy@dozi.app
                        Web: www.dozi.app/gizlilik
                        Adres: Bardino Technology
                        
                        Son Güncelleme: Ocak 2025
                    """.trimIndent()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = DoziTurquoise)
            }
        }
    )
}

@Composable
private fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Kullanım Şartları",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PolicySection(
                    title = "1. Sözleşmenin Konusu",
                    content = "İşbu kullanım şartları, Bardino Technology tarafından geliştirilen Dozi mobil uygulamasının kullanımına ilişkin hüküm ve koşulları düzenler. Uygulamayı kullanarak bu şartları kabul etmiş sayılırsınız."
                )

                PolicySection(
                    title = "2. Tanımlar",
                    content = """
                        • Uygulama: Dozi mobil sağlık asistan uygulaması
                        • Kullanıcı: Uygulamayı kullanan gerçek kişi
                        • Hizmet: Uygulama üzerinden sunulan tüm özellikler ve içerikler
                        • Premium: Ücretli abonelik planı
                        • İçerik: Uygulama içinde yer alan tüm bilgi, veri ve görsel materyaller
                    """.trimIndent()
                )

                PolicySection(
                    title = "3. Hizmetin Kapsamı",
                    content = """
                        Dozi aşağıdaki hizmetleri ve daha fazlasını sunar:
                        
                        • İlaç takibi ve hatırlatma sistemi
                        • Doz ve kullanım zamanı yönetimi
                        • İlaç stok takibi
                        • Aile üyeleri takip sistemi
                        • Bulut yedekleme
                    """.trimIndent()
                )

                PolicySection(
                    title = "4. Kullanıcı Sorumlulukları",
                    content = """
                        Kullanıcı olarak aşağıdaki hususları kabul edersiniz:
                        
                        • Girdiğiniz ilaç bilgilerinin doğruluğundan siz sorumlusunuz
                        • Hatırlatmaları kontrol etmek sizin sorumluluğunuzdadır
                        • Hesap bilgilerinizi güvenli tutmakla yükümlüsünüz
                        • Uygulamayı yasalara uygun kullanacağınızı taahhüt edersiniz
                        • Başkalarının hesap bilgilerini kullanmayacağınızı kabul edersiniz
                        • 18 yaşından küçükseniz, ebeveyn veya vasi gözetiminde kullanacağınızı taahhüt edersiniz
                    """.trimIndent()
                )

                PolicySection(
                    title = "5. Sorumluluk Reddi - ÖNEMLİ",
                    content = """
                        DİKKAT: Dozi bir hatırlatma ve takip uygulamasıdır, tıbbi bir araç veya sağlık danışmanlığı hizmeti DEĞİLDİR.
                        
                        • Dozi tıbbi tavsiye, teşhis veya tedavi sağlamaz
                        • İlaç kullanımına ilişkin kararlar doktorunuzla alınmalıdır
                        • Uygulama, reçete yazmaz veya ilaç önerisinde bulunmaz
                        • Acil durumlarda 112'yi arayın, uygulamaya güvenmeyin
                        • İlaç etkileşimleri bilgilendirme amaçlıdır, mutlaka doktorunuza danışın
                        • Uygulama kaynaklı herhangi bir zarardan Bardino Technology sorumlu tutulamaz
                    """.trimIndent()
                )

                PolicySection(
                    title = "6. Premium Abonelik",
                    content = """
                        • Abonelik bedelleri uygulama içinde belirtilmiştir
                        • Ödemeler Google Play Store üzerinden gerçekleşir
                        • Abonelik otomatik olarak yenilenir
                        • İptal işlemi Google Play Store'dan yapılmalıdır
                        • İptal sonrası mevcut dönem sonuna kadar hizmet devam eder
                        • Kısmi iade yapılmaz
                        • Ücretsiz deneme süresi sonunda otomatik ücretlendirme başlar
                    """.trimIndent()
                )

                PolicySection(
                    title = "7. Fikri Mülkiyet Hakları",
                    content = "Dozi uygulaması, logosu, tasarımı ve içeriği Bardino Technology'nin fikri mülkiyetidir. İzinsiz kullanım, kopyalama, değiştirme veya dağıtım yasaktır."
                )

                PolicySection(
                    title = "8. Hizmet Kesintileri",
                    content = "Bardino Technology, teknik nedenler, bakım çalışmaları veya mücbir sebepler nedeniyle hizmeti geçici olarak durdurma hakkını saklı tutar. Bu durumlarda kullanıcılara bildirim yapılmaya çalışılır."
                )

                PolicySection(
                    title = "9. Hesap Silme",
                    content = """
                        • Hesabınızı istediğiniz zaman silebilirsiniz
                        • Silme işlemi ayarlar > hesap > hesabı sil sekmesinden yapılır
                        • Silinen hesaplar 30 gün içinde kalıcı olarak silinir
                        • Bu süre içinde geri alabilirsiniz
                        • Silinen veriler geri getirilemez
                    """.trimIndent()
                )

                PolicySection(
                    title = "10. Değişiklikler",
                    content = "Bardino Technology, kullanım şartlarını önceden bildirmeksizin değiştirme hakkını saklı tutar. Önemli değişiklikler uygulama içi bildirim ile duyurulur."
                )

                PolicySection(
                    title = "11. Uygulanacak Hukuk",
                    content = "İşbu sözleşme Türkiye Cumhuriyeti yasalarına tabidir. Uyuşmazlıklar İstanbul Mahkemeleri ve İcra Dairelerinde çözümlenir."
                )

                PolicySection(
                    title = "12. İletişim",
                    content = """
                        Kullanım şartları ile ilgili sorularınız için:
                        
                        E-posta: support@dozi.app
                        Web: www.dozi.app/sartlar
                        
                        Son Güncelleme: Ocak 2025
                    """.trimIndent()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = DoziTurquoise)
            }
        }
    )
}

@Composable
private fun PolicySection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
        )
    }
}