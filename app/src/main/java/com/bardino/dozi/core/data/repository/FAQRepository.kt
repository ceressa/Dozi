package com.bardino.dozi.core.data.repository

import android.util.Log
import com.bardino.dozi.core.data.model.FAQ
import com.bardino.dozi.core.data.model.FAQCategory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FAQRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Tum SSS'leri kategoriye gore gruplu getir
     * Firestore bossa seed data yukler
     */
    suspend fun getAllFAQs(): List<FAQ> {
        return try {
            val snapshot = db.collection("faqs")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val faqs = snapshot.documents
                .mapNotNull { it.toObject(FAQ::class.java)?.copy(id = it.id) }
                .sortedBy { it.order }

            // Firestore bossa seed data yukle
            if (faqs.isEmpty()) {
                seedFAQs()
                return getDefaultFAQs()
            }

            faqs
        } catch (e: Exception) {
            Log.e("FAQRepository", "Error fetching FAQs: ${e.message}")
            // Hata durumunda varsayilan verileri don
            getDefaultFAQs()
        }
    }

    /**
     * Firestore'a seed data yukle
     */
    private suspend fun seedFAQs() {
        try {
            val batch = db.batch()
            getDefaultFAQs().forEach { faq ->
                val docRef = db.collection("faqs").document(faq.id)
                batch.set(docRef, faq)
            }
            batch.commit().await()
            Log.d("FAQRepository", "Seed data yuklendi: ${getDefaultFAQs().size} soru")
        } catch (e: Exception) {
            Log.e("FAQRepository", "Seed data yuklenemedi: ${e.message}")
        }
    }

    /**
     * Kategorileri getir
     */
    suspend fun getCategories(): List<FAQCategory> {
        return try {
            val snapshot = db.collection("faq_categories")
                .get()
                .await()

            snapshot.documents
                .mapNotNull { it.toObject(FAQCategory::class.java)?.copy(id = it.id) }
                .sortedBy { it.order }
        } catch (e: Exception) {
            Log.e("FAQRepository", "Error fetching categories: ${e.message}")
            emptyList()
        }
    }

    /**
     * Belirli bir kategorideki SSS'leri getir
     */
    suspend fun getFAQsByCategory(category: String): List<FAQ> {
        return try {
            val snapshot = db.collection("faqs")
                .whereEqualTo("category", category)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { it.toObject(FAQ::class.java)?.copy(id = it.id) }
                .sortedBy { it.order }
        } catch (e: Exception) {
            Log.e("FAQRepository", "Error fetching FAQs by category: ${e.message}")
            emptyList()
        }
    }

    /**
     * SSS'lerde arama yap
     */
    suspend fun searchFAQs(query: String): List<FAQ> {
        val allFaqs = getAllFAQs()
        val lowerQuery = query.lowercase()

        return allFaqs.filter { faq ->
            faq.question.lowercase().contains(lowerQuery) ||
            faq.answer.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Varsayilan SSS verileri
     */
    private fun getDefaultFAQs(): List<FAQ> = listOf(
        // Ilac Yonetimi
        FAQ(
            id = "faq_1",
            category = "Ilac Yonetimi",
            question = "Nasil yeni ilac eklerim?",
            answer = "Ana sayfada '+' butonuna tiklayin veya Ilaclarim sekmesinden 'Ilac Ekle' butonuna basin. Ilac adini arayarak veritabanindan secebilir ya da 'Ozel Ilac Ekle' ile manuel olarak girebilirsiniz.",
            order = 1,
            isActive = true
        ),
        FAQ(
            id = "faq_2",
            category = "Ilac Yonetimi",
            question = "Ilac bilgilerini nasil duzenlerim?",
            answer = "Ilaclarim sekmesinden duzenlemek istediginiz ilaca tiklayin. Acilan detay sayfasinda sag ustteki kalem ikonuna basarak ilac adini, dozajini, stok miktarini ve diger bilgileri guncelleyebilirsiniz.",
            order = 2,
            isActive = true
        ),
        FAQ(
            id = "faq_3",
            category = "Ilac Yonetimi",
            question = "Ilaci nasil silerim?",
            answer = "Ilac detay sayfasina gidin ve sag ustteki cop kutusu ikonuna basin. Onay dialogunda 'Sil' butonuna basarak ilaci kalici olarak silebilirsiniz. Bu islem geri alinamaz.",
            order = 3,
            isActive = true
        ),
        FAQ(
            id = "faq_4",
            category = "Ilac Yonetimi",
            question = "Ilac stok takibi nasil calisir?",
            answer = "Ilac eklerken veya duzenlerken stok miktarini girin. Her ilac aldiginizda stok otomatik duser. Stok azaldiginda size bildirim gondeririz. Stok bittiginde ilaci yeniden temin etmeniz gerektigini hatirlatiriz.",
            order = 4,
            isActive = true
        ),

        // Hatirlaticilar
        FAQ(
            id = "faq_5",
            category = "Hatirlaticilar",
            question = "Nasil hatirlatici eklerim?",
            answer = "Ilac ekledikten sonra otomatik olarak hatirlatici ekleme ekranina yonlendirilirsiniz. Alternatif olarak Hatirlaticilar sekmesinden '+' butonuna basarak da yeni hatirlatici olusturabilirsiniz.",
            order = 5,
            isActive = true
        ),
        FAQ(
            id = "faq_6",
            category = "Hatirlaticilar",
            question = "Hatirlatici saatlerini nasil degistiririm?",
            answer = "Hatirlaticilar sekmesinden duzenlemek istediginiz hatirlaticinizi secin. Saat, siklik (her gun, gun asiri, vb.) ve bitis tarihini guncelleyebilirsiniz. Degisiklikler kaydedildiginde alarmlar otomatik guncellenir.",
            order = 6,
            isActive = true
        ),
        FAQ(
            id = "faq_7",
            category = "Hatirlaticilar",
            question = "Hatirlatiyi nasil silerim?",
            answer = "Hatirlatici duzenleme ekraninda en altta 'Hatirlatiyi Sil' butonuna basin. Bu islem sadece hatirlatiyi siler, ilac kaydiniz korunur.",
            order = 7,
            isActive = true
        ),
        FAQ(
            id = "faq_8",
            category = "Hatirlaticilar",
            question = "Bildirimler neden gelmiyor?",
            answer = "1) Telefon ayarlarindan Dozi icin bildirim iznini kontrol edin.\n2) Pil tasarrufu modunda Dozi'yi istisnalara ekleyin.\n3) Tam alarm izni verildiginden emin olun.\n4) Rahatsiz Etme modunun kapali oldugunu kontrol edin.",
            order = 8,
            isActive = true
        ),
        FAQ(
            id = "faq_9",
            category = "Hatirlaticilar",
            question = "Sesli hatirlatici nasil aktif edilir?",
            answer = "Sesli hatirlaticilar Dozi Ekstra ozelligidir. Premium'a yukselttiginizde Profil > Bildirim Ayarlari > Sesli Hatirlatici bolumunden erkek veya kadin sesi secebilirsiniz.",
            order = 9,
            isActive = true
        ),

        // Premium
        FAQ(
            id = "faq_10",
            category = "Premium",
            question = "Dozi Ekstra nedir?",
            answer = "Dozi Ekstra premium aboneliktir. Sinirsiz ilac ve hatirlatici, bulut yedekleme, sesli hatirlaticilar, detayli istatistikler, tema ozellestirme ve oncelikli destek icermektedir.",
            order = 10,
            isActive = true
        ),
        FAQ(
            id = "faq_11",
            category = "Premium",
            question = "Ucretsiz planda kac ilac ekleyebilirim?",
            answer = "Ucretsiz planda 1 ilac ve 2 hatirlatma saati ekleyebilirsiniz. Daha fazlasi icin Dozi Ekstra'ya yukseltmeniz gerekir.",
            order = 11,
            isActive = true
        ),
        FAQ(
            id = "faq_12",
            category = "Premium",
            question = "Aboneligimi nasil iptal ederim?",
            answer = "Google Play Store > Abonelikler bolumunden Dozi aboneliginizi iptal edebilirsiniz. Iptal ettiginizde mevcut donem sonuna kadar premium ozellikler devam eder.",
            order = 12,
            isActive = true
        ),
        FAQ(
            id = "faq_13",
            category = "Premium",
            question = "Aile plani nedir?",
            answer = "Aile plani ile 4 kisiye kadar aile uyelerinin ilac takibini yapabilirsiniz. Her uye kendi ilaclari ve hatirlaticilarina sahip olur. Organizator tum aileyi gorebilir.",
            order = 13,
            isActive = true
        ),

        // Hesap
        FAQ(
            id = "faq_14",
            category = "Hesap",
            question = "Verilerim guvenli mi?",
            answer = "Evet! Verileriniz Google Firebase altyapisinda sifrelenerek saklanir. Google hesabinizla giris yaptiginizda verileriniz bulutta yedeklenir ve tum cihazlarinizda senkronize olur.",
            order = 14,
            isActive = true
        ),
        FAQ(
            id = "faq_15",
            category = "Hesap",
            question = "Cihaz degistirdigimde verilerim kaybolur mu?",
            answer = "Hayir! Google hesabinizla giris yaptiginiz surece verileriniz bulutta saklanir. Yeni cihazinizda ayni Google hesabiyla giris yaptiginizda tum ilac ve hatirlaticilariniz otomatik yuklenir.",
            order = 15,
            isActive = true
        ),
        FAQ(
            id = "faq_16",
            category = "Hesap",
            question = "Hesabimi nasil silerim?",
            answer = "Hesabinizi silmek icin info@dozi.app adresine e-posta gonderin. Hesap silme islemi tum verilerinizin kalici olarak silinmesine neden olur ve geri alinamaz.",
            order = 16,
            isActive = true
        ),

        // Badi Sistemi
        FAQ(
            id = "faq_17",
            category = "Badi Sistemi",
            question = "Badi nedir?",
            answer = "Badi, ilac takibini yaptiginiz yakinlarinizdir (anne, baba, vb.). Badi'nin ilac saati geldiginde hem siz hem de badi bildirim alir. Badi'nin ilaclari sizin uygulamanizda gorunur.",
            order = 17,
            isActive = true
        ),
        FAQ(
            id = "faq_18",
            category = "Badi Sistemi",
            question = "Nasil Badi eklerim?",
            answer = "Alt menuden Badi sekmesine gidin ve '+' butonuna basin. Badi'nin ismini girin ve olusturulan 6 haneli kodu badi'nin telefonundaki Dozi uygulamasinda giriniz.",
            order = 18,
            isActive = true
        ),

        // Genel
        FAQ(
            id = "faq_19",
            category = "Genel",
            question = "Uygulama cokuyorsa ne yapmaliyim?",
            answer = "1) Uygulamayi kapatip tekrar acin.\n2) Telefonu yeniden baslatin.\n3) Uygulamayi guncelleyin.\n4) Onbellegi temizleyin (Ayarlar > Uygulamalar > Dozi > Onbellegi Temizle).\n\nSorun devam ederse info@dozi.app adresine yazin.",
            order = 19,
            isActive = true
        ),
        FAQ(
            id = "faq_20",
            category = "Genel",
            question = "Uygulamayi nasil guncellerim?",
            answer = "Google Play Store'da Dozi'yi arayin ve 'Guncelle' butonuna basin. Otomatik guncelleme icin Play Store ayarlarindan otomatik guncellemeyi aktif edin.",
            order = 20,
            isActive = true
        )
    )
}
