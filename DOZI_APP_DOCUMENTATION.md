# Dozi - Medication Reminder & Health Management App

## Genel Bakış

**Dozi**, kullanıcıların ilaçlarını yönetmelerine, uyumluluklarını takip etmelerine ve aile üyelerinin sağlık yönetimini desteklemelerine yardımcı olmak için tasarlanmış kapsamlı bir **Android/Kotlin** ilaç hatırlatma ve sağlık takip uygulamasıdır.

### Temel İstatistikler
- **122 Kotlin (.kt) kaynak dosyası**
- **Teknoloji Yığını:** Kotlin, Android Compose (UI), Firebase (backend), Room Database (local), Jetpack components
- **Mimari:** Hilt Dependency Injection ile MVVM + Jetpack Compose
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35 (Android 15)

---

## Ana Özellikler

### 1. İlaç Yönetimi
- Türk ilaç veritabanından veya özel giriş ile ilaç ekleme
- Dozaj, frekans ve zaman programı takibi
- İlaç formları (tablet, kapsül, şurup, damla vb.)
- Dozaj birimleri (hap, doz, mg, ml, ünite, damla, kaşık)
- Düşük stok uyarıları ile stok/envanter takibi
- Geçici ilaçlar için başlangıç/bitiş tarihleri
- Görsel tanımlama için ilaç renkleri ve emoji ikonları
- Kritiklik seviyeleri (rutin, önemli, kritik)
- Barkod/QR kod desteği (ML Kit ile OCR)

### 2. Akıllı Hatırlatmalar
- Günlük, haftalık veya özel frekans zamanlama
- AlarmManager tabanlı bildirimler (kesin alarm desteği)
- İlaç başına birden fazla zaman dilimi
- İlaç eylem ekranlarına deep link desteği
- Kaçırılan ilaçlar için yükseltme sistemi (3 seviye: bildirim → ses → kritik)
- Kalıplara dayalı akıllı hatırlatma önerileri
- Overlay aktivitesi ile erteleme fonksiyonu
- Kritik ilaç bypass ile Rahatsız Etmeyin (DND) desteği
- Uyarlanabilir zamanlama (akıllı zaman önerileri)

### 3. İlaç Takibi ve Uyumluluk
- İlaçların alındığı/atlandığı/kaçırıldığı zaman kaydı
- Gerçek zamanlı uyumluluk oranı hesaplama
- Saatlik uyumluluk analitiği
- Ertelenmiş ilaç takibi
- Konum tabanlı takip (geofencing)
- Ruh hali ve yan etki kaydı
- 30 günlük uyumluluk trendleri ve istatistikleri

### 4. Oyunlaştırma ve Başarılar
- **Seri Sistemi:** Mevcut ve en uzun seriler
- **13 Başarı Türü:**
  - Seri başarıları (7, 30, 100, 365 gün)
  - Mükemmel hafta/ay
  - İlk ilaç ve ilk doz
  - İlaç koleksiyoncusu (5, 10 farklı ilaç)
  - Toplam doz başarıları (50, 100, 365, 1000 doz)
- Profilde rozet gösterimi
- İlerleme takibi

### 5. Badi Sistemi (Arkadaş/Bakıcı)
- **Badi Rolleri:**
  - Görüntüleyici (salt okunur)
  - Yardımcı (ilaçları alındı olarak işaretleme)
  - Bakıcı (hatırlatmaları düzenleme)
  - Yönetici (tam kontrol)
- E-posta veya benzersiz badi kodu ile arkadaş ekleme
- Badi isteklerini kabul etme/reddetme (7 gün süre)
- Ayrıntılı izin yönetimi
- Badi'ye özel bildirim tercihleri
- Badi'nin ilaç uyumluluğunu takip etme

### 6. Aile Paketi
- Organizatör plan oluşturur (organizatör dahil max 4 üye)
- Aile üyelerini kod ile davet etme
- Paylaşılan premium özellikler
- Organizatör aile üyelerini yönetir
- Plan durumu takibi

### 7. Premium Özellikler (Dozi Ekstra / Dozi Aile)
- **Bireysel Planlar:**
  - Ekstra Aylık / Ekstra Yıllık
- **Aile Planları:**
  - Aile Aylık / Aile Yıllık (4 üye)
- **Limitler:**
  - Ücretsiz: 1 ilaç, 2 zaman dilimi
  - Premium: Sınırsız ilaç ve hatırlatma
  - Ekstra: 1 badi (bakıcı)
  - Aile: Birden fazla aile üyesi
- **Deneme Sistemi:** Yeni kullanıcılar için 3 günlük ücretsiz deneme
- **Premium ile Açılan Özellikler:**
  - Sınırsız ilaçlar
  - Özel bildirim sesleri
  - Gelişmiş analitik
  - Aile planı
  - Birden fazla badi/bakıcı

### 8. Bildirimler ve İletişim
- **Bildirim Türleri:** Hatırlatmalar, badi istekleri, ilaç uyarıları, stok uyarıları, sistem bildirimleri
- **Kanallar:** Hatırlatma Kanalı (YÜKSEK), Badi Kanalı (VARSAYILAN), Kritik Kanal
- **FCM Entegrasyonu:** Firebase Cloud Messaging ile push bildirimleri
- **Bildirim Eylemleri:** Bildirimde al, ertele, atla eylemleri
- **Rahatsız Etmeyin (DND):** Özelleştirilebilir sessiz saatler (varsayılan 22:00-08:00)
- **Yükseltme:** Kaçırılan kritik ilaçlar için 3 seviyeli yükseltme

### 9. Ses Özellikleri
- Ses komutu ayrıştırma
- Ses asistanı (Erkek/Kadın sesleri)
- Ses tabanlı ilaç kaydı

### 10. Konum Özellikleri
- Eczane/hastane konumlarını kaydetme
- Geofence tabanlı hatırlatmalar
- Konum tabanlı ilaç takibi
- Google Maps entegrasyonu
- Places API entegrasyonu

### 11. Analitik ve İstatistikler
- Günlük uyumluluk oranı
- Mevcut/en uzun seri
- En iyi/en kötü uyumluluk saatleri
- Toplam alınan/kaçırılan/atlanan ilaçlar
- Grafiklerle 30 günlük trendler
- Saatlik uyumluluk ısı haritası
- Yönetici analitik panosu

### 12. Özelleştirme ve Ayarlar
- **Tema:** Açık, Koyu, Sistem
- **Dil:** Türkçe (tr) genişletilebilir destek ile
- **Saat Dilimi:** Varsayılan Europe/Istanbul
- **Bildirimler:** Ses, titreşim, öncelik ayarları
- **Kullanıcı Tercihleri:** Titreşim etkin/devre dışı, ses cinsiyeti seçimi
- **DND Ayarları:** Özel sessiz saatler
- **Uyarlanabilir Zamanlama:** Akıllı zaman önerileri
- **Başlangıç Tamamlama Takibi**

### 13. Veritabanı ve Senkronizasyon
- **Yerel Depolama (Room):**
  - MedicationLog
  - SyncQueue (çevrimdışı-önce desteği)
- **Bulut Depolama (Firestore):**
  - Kullanıcı profilleri
  - İlaçlar
  - Hatırlatmalar
  - İlaç kayıtları
  - Başarılar
  - BadiBuddy ilişkileri
  - Aile planları
  - Bildirimler
- **Çevrimdışı Destek:** Senkronizasyon kuyruğu, periyodik WorkManager senkronizasyonu (30 dakika aralık)
- **Firestore Güvenlik Kuralları:** Belge seviyesi ve alt koleksiyon erişim kontrolü
- **Çevrimdışı Kalıcılık:** Anında sorgu sonuçları için etkin

### 14. Kimlik Doğrulama
- Google Sign-In entegrasyonu
- Firebase Authentication
- E-posta tabanlı kimlik doğrulama
- Cihaz ID takibi
- Yeniden deneme mantığı ile FCM token yönetimi
- Reaktif giriş için Auth state listeners

### 15. Faturalandırma
- Google Play Billing Library entegrasyonu
- Plan yönetimi (ücretsiz, deneme, bireysel, aile)
- Son kullanma tarihi takibi ve doğrulama
- Plan türü normalleştirme (eski + yeni formatlar)
- Premium durum hesaplama

### 16. Widget
- Ana ekran ilaç hatırlatma widget'ı
- İlaç alma/atlama için hızlı eylem widget'ı
- Gerçek zamanlı widget güncellemeleri
- Glance widget framework

### 17. Türk İlaç Veritabanı
- Yerleşik Türk ilaç arama (ilaclar.json)
- Hızlı aramalar için bellek içi veritabanı
- İlaç bilgisi: Etken madde, üretici, barkod
- Özel ilaç girişine geri dönüş

### 18. Hata İşleme ve Kayıt
- Özel Dozi hata türleri
- Uygulama genelinde kayıt
- Backend hataları için Firebase Functions entegrasyonu
- İşlemlerden önce izin doğrulama

### 19. Yönetici Özellikleri
- Analitik panosu
- Kullanıcı yönetimi
- Yasaklama sistemi
- Premium analitik takibi
- Günlük analitik anlık görüntüsü

### 20. Erişilebilirlik ve UX
- Material Design 3
- Jetpack Compose UI framework
- Durum koruma ile alt gezinme
- Duyarlı düzenler
- Boş durum işleme
- Yükleme durumları
- Hata mesajları
- 48dp minimum dokunma hedefi

---

## Mimari Kalıpları

### Teknoloji Yığını:
- **Dil:** Kotlin
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Mimari:** Jetpack bileşenleri ile MVVM
- **Dependency Injection:** Hilt
- **Veritabanı:**
  - **Yerel:** Room (DAO'lar ile)
  - **Bulut:** Firestore
- **Kimlik Doğrulama:** Firebase Auth + Google Sign-In
- **Backend Servisleri:**
  - Firebase Functions (Firestore tetikleyicileri)
  - Firebase Cloud Messaging (FCM)
  - Google Play Billing
- **Ağ:** Firebase REST API, Google APIs
- **İzinler:** Runtime izin işleme
- **Alarmlar:** AlarmManager (kesin alarm)
- **Arka Plan Görevleri:** WorkManager (periyodik senkronizasyon)
- **Konum:** Google Play Services, Google Maps
- **ML:** ML Kit (metin tanıma/OCR, barkod tarama)
- **Navigasyon:** Compose ile Jetpack Navigation

### Dependency Injection (Hilt):
- **DatabaseModule:** Room veritabanı, DAO'lar
- **FirebaseModule:** Firebase servisleri (Auth, Firestore, Messaging, Functions)
- **RepositoryModule:** Singleton olarak tüm repository'ler

### Veri Akışı:
1. **UI Katmanı (Compose):** Ekranlar ve bileşenler
2. **ViewModel Katmanı:** Durum yönetimi ve iş mantığı
3. **Repository Katmanı:** Veri erişim soyutlama
4. **Veri Kaynakları:** Firestore, Room, SharedPreferences, Firebase Servisleri

---

## Proje Yapısı

```
com.bardino.dozi/
├── core/                          # Çekirdek işlevsellik
│   ├── billing/                   # Uygulama içi satın almalar
│   ├── common/                    # Sabitler, sonuç sarmalayıcıları
│   ├── data/                      # Veri katmanı
│   │   ├── local/                 # Room veritabanı
│   │   ├── model/                 # Veri modelleri (18 dosya)
│   │   └── repository/            # 13 repository
│   ├── error/                     # Hata türleri
│   ├── premium/                   # Premium yönetimi
│   ├── sync/                      # Çevrimdışı-önce senkronizasyon
│   ├── ui/                        # UI bileşenleri ve ekranlar
│   │   ├── components/            # Yeniden kullanılabilir bileşenler
│   │   ├── screens/               # Özellik ekranları
│   │   │   ├── admin/             # Yönetici panosu
│   │   │   ├── badi/              # Badi sistemi (5 ekran)
│   │   │   ├── family/            # Aile yönetimi
│   │   │   ├── home/              # Ana sayfa
│   │   │   ├── login/             # Giriş
│   │   │   ├── medication/        # İlaç eylemleri
│   │   │   ├── medicine/          # İlaç yönetimi (5 ekran)
│   │   │   ├── notifications/     # Bildirimler
│   │   │   ├── premium/           # Premium
│   │   │   ├── profile/           # Kullanıcı profili
│   │   │   ├── reminder/          # Hatırlatma yönetimi
│   │   │   ├── settings/          # Ayarlar
│   │   │   ├── stats/             # İstatistikler
│   │   │   └── support/           # Destek
│   │   ├── state/                 # UI olayları ve durumları
│   │   ├── theme/                 # Material 3 tasarım sistemi
│   │   └── viewmodel/             # ViewModel'ler
│   ├── utils/                     # Yardımcı sınıflar
│   └── voice/                     # Ses özellikleri
├── di/                            # Dependency injection
├── geofence/                      # Konum tabanlı özellikler
├── navigation/                    # Navigasyon grafiği ve rotalar
├── notifications/                 # Hatırlatma ve FCM sistemi
├── widget/                        # Ana ekran widget'ı
├── DoziApplication.kt
├── MainActivity.kt
└── SplashActivity.kt
```

---

## Temel İstatistikler

| Metrik | Sayı |
|--------|------|
| Toplam Kotlin Dosyası | 122 |
| Veri Modelleri | 18 |
| Repository'ler | 13 |
| Ekranlar/UI Bileşenleri | 35+ |
| Veritabanı Entity'leri | 2 (MedicationLog, SyncQueue) |
| Başarı Türleri | 13 |
| Badi Rolleri | 4 |
| Bildirim Türleri | 12 |
| Premium Planları | 5 (Ücretsiz, Deneme, Ekstra Aylık/Yıllık, Aile Aylık/Yıllık) |
| Tanımlı Sabitler | 24+ |

---

## Ekran Listesi

1. **SplashActivity** - Açılış/Yükleme ekranı
2. **LoginScreen** - Google Sign-In ekranı
3. **HomeScreen** - Bugünün ilaçları, seri, istatistikler ile ana panel
4. **MedicineListScreen** - Tüm ilaçlar listesi
5. **MedicineDetailScreen** - İlaç detayları ve geçmişi
6. **MedicineEditScreen** - İlaç bilgilerini düzenleme
7. **MedicineLookupScreen** - Türk ilaç veritabanı arama
8. **CustomMedicineAddScreen** - Özel ilaç ekleme
9. **ReminderListScreen** - Tüm hatırlatmalar listesi
10. **AddReminderScreen** - Hatırlatma programı oluşturma/düzenleme
11. **MedicationActionScreen** - İlaç alma/atlama hızlı eylem ekranı
12. **StatsScreen** - İlaç uyumluluk istatistikleri
13. **BadiListScreen** - Bakıcılar/arkadaşlar listesi
14. **AddBadiScreen** - E-posta/kod ile arkadaş ekleme
15. **BadiDetailScreen** - Arkadaş detayları ve izinleri
16. **BadiPermissionsScreen** - Arkadaş izinlerini yapılandırma
17. **BadiMedicationTrackingScreen** - Arkadaşın ilaçlarını takip
18. **ProfileScreen** - Kullanıcı profili ve hesap yönetimi
19. **LocationsScreen** - Kayıtlı konumlar (eczaneler)
20. **SettingsScreen** - Uygulama ayarları
21. **NotificationSettingsScreen** - Bildirim tercihleri
22. **AdvancedNotificationSettingsScreen** - Gelişmiş bildirim ayarları
23. **NotificationSoundSettingsScreen** - Ses özelleştirme
24. **AboutScreen** - Hakkında/Yardım ekranı
25. **PremiumScreen** - Premium özellikler ve abonelik
26. **PremiumIntroScreen** - Premium tanıtım/satış ekranı
27. **FamilyManagementScreen** - Aile planı yönetimi
28. **NotificationsScreen** - Bildirim geçmişi
29. **SupportScreen** - Yardım ve destek
30. **AnalyticsDashboardScreen** - Yönetici analitik panosu

---

## Veri Modelleri

1. **User** - Kullanıcı profili, premium durumu, aile planı, DND ayarları
2. **Medicine** - İlaç detayları, dozaj, frekans, stok takibi
3. **Reminder** - Frekans ve program ile ilaç hatırlatmaları
4. **MedicationLog** - Zaman damgaları ile alınan/atlanan/kaçırılan ilaç geçmişi
5. **UserStats** - Kullanıcı istatistikleri (seriler, uyumluluk oranı, başarılar)
6. **Achievement** - 13 başarı türü ile oyunlaştırma sistemi (rozetler)
7. **Premium** - Premium abonelik planları ve analitik
8. **Badi** - Aile sağlık yönetimi için arkadaş/bakıcı sistemi
9. **FamilyPlan** - Aile planı aboneliği (4 üyeye kadar)
10. **BadiRequest** - Süresi dolan arkadaş istek sistemi
11. **DoziNotification** - Uygulama içi ve push bildirimleri
12. **PricingConfig** - Firestore'dan dinamik fiyatlandırma yapılandırması
13. **FAQ** - SSS soruları ve kategorileri

---

## Repository'ler

1. **UserRepository** - Kullanıcı CRUD ve Firebase Auth entegrasyonu
2. **MedicineRepository** - İlaç yönetimi
3. **MedicationLogRepository** - İlaç geçmişi ve uyumluluk takibi
4. **BadiRepository** - Arkadaş/bakıcı yönetimi ve izinleri
5. **FamilyPlanRepository** - Aile planı abonelikleri
6. **PremiumRepository** - Premium özellikler ve deneme yönetimi
7. **PricingRepository** - Fiyatlandırma bilgileri
8. **NotificationRepository** - Bildirim yönetimi
9. **AchievementRepository** - Başarı/rozet sistemi
10. **FAQRepository** - SSS getirme
11. **LocationRepository** - Kayıtlı konumlar (eczane, hastane)
12. **UserStatsRepository** - Kullanıcı istatistikleri ve analitik
13. **UserPreferencesRepository** - Kullanıcı tercihleri (tema, dil, ses)

---

## Özet

Dozi şunları içeren **kapsamlı bir ilaç yönetimi ve aile sağlık uygulamasıdır:**

- Yükseltme ile akıllı hatırlatma sistemi
- Aile sağlık izleme için arkadaş/bakıcı desteği
- Başarı sistemi ile oyunlaştırma
- Aile planları ile premium abonelik modeli
- Bulut senkronizasyonu ile çevrimdışı-önce mimari
- Gelişmiş analitik ve uyumluluk takibi
- Konum tabanlı özellikler
- Türk ilaç veritabanı entegrasyonu
- Birden fazla özelleştirme seçeneği
- Modern Jetpack Compose UI

Kod tabanı, temiz mimari, dependency injection, reaktif programlama ve kapsamlı özellik uygulaması ile profesyonel Android geliştirme pratiklerini göstermektedir.
