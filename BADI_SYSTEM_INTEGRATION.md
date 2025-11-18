# 🤝 Buddy Sistemi Entegrasyon Rehberi

## 📋 Genel Bakış

Buddy sistemi, kullanıcıların sevdiklerini ekleyip ilaç takibini birlikte yönetmelerini sağlayan kapsamlı bir özelliktir. Bu rehber, sistemi uygulamanıza entegre etmeniz için gerekli tüm adımları içerir.

---

## ✅ Tamamlanan Bileşenler

### 1. Data Layer ✅
- **Models**:
  - `Buddy.kt` - Buddy ilişkisi ve izinler
  - `BuddyRequest.kt` - Buddy istekleri
  - `Reminder.kt` - İlaç hatırlatmaları
  - `MedicationLog.kt` - İlaç alma geçmişi
  - `DoziNotification.kt` - Bildirim sistemi
  - `Medicine.kt` - Güncellenmiş (buddy alanları eklendi)
  - `User.kt` - Güncellenmiş (FCM token ve buddy kodu)

- **Repositories**:
  - `BuddyRepository.kt` - Buddy CRUD işlemleri
  - `MedicationLogRepository.kt` - İlaç geçmişi yönetimi
  - `NotificationRepository.kt` - Bildirim yönetimi

### 2. Presentation Layer ✅
- **ViewModels**:
  - `BuddyViewModel.kt` - Buddy yönetimi
  - `NotificationViewModel.kt` - Bildirim yönetimi

- **UI Screens**:
  - `BuddyListScreen.kt` - Buddy listesi ve bekleyen istekler
  - `AddBuddyScreen.kt` - Buddy ekleme (kod/email ile)
  - `BuddyMedicationTrackingScreen.kt` - Buddy ilaç takibi
  - `NotificationsScreen.kt` - Bildirim merkezi

### 3. Services ✅
- **Notifications**:
  - `DoziMessagingService.kt` - Güncellenmiş FCM service
  - `NotificationHelper.kt` - Mevcut (buddy bildirimleri için hazır)

### 4. Backend ✅
- **Firebase Setup**:
  - Firestore veri yapısı
  - Security rules
  - Cloud Functions (kod örnekleri)
  - FCM entegrasyonu

---

## 🚀 Entegrasyon Adımları

### Adım 1: Navigation Route'larını Ekleyin

`Screen.kt` dosyasına yeni route'lar ekleyin:

```kotlin
// app/src/main/java/com/bardino/dozi/navigation/Screen.kt

sealed class Screen(val route: String) {
    // Mevcut screen'ler...

    // 🤝 Buddy Screens
    object BuddyList : Screen("buddy_list")
    object AddBuddy : Screen("add_buddy")
    object BuddyDetail : Screen("buddy_detail/{buddyId}") {
        fun createRoute(buddyId: String) = "buddy_detail/$buddyId"
    }
    object BuddyMedicationTracking : Screen("buddy_medication_tracking/{buddyId}") {
        fun createRoute(buddyId: String) = "buddy_medication_tracking/$buddyId"
    }

    // 🔔 Notification Screen
    object Notifications : Screen("notifications")
}
```

### Adım 2: Navigation Graph'ı Güncelleyin

`NavGraph.kt` dosyasına yeni composable'ları ekleyin:

```kotlin
// app/src/main/java/com/bardino/dozi/navigation/NavGraph.kt

import com.bardino.dozi.core.ui.screens.buddy.*
import com.bardino.dozi.core.ui.screens.notifications.NotificationsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Mevcut composable'lar...

        // 🤝 Buddy Navigation
        composable(Screen.BuddyList.route) {
            BuddyListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddBuddy = {
                    navController.navigate(Screen.AddBuddy.route)
                },
                onNavigateToBuddyDetail = { buddyId ->
                    navController.navigate(Screen.BuddyDetail.createRoute(buddyId))
                }
            )
        }

        composable(Screen.AddBuddy.route) {
            AddBuddyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.BuddyDetail.route,
            arguments = listOf(
                navArgument("buddyId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val buddyId = backStackEntry.arguments?.getString("buddyId") ?: return@composable
            BadiDetailScreen(
                badiId = buddyId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.BuddyMedicationTracking.route,
            arguments = listOf(
                navArgument("buddyId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val buddyId = backStackEntry.arguments?.getString("buddyId") ?: return@composable
            BuddyMedicationTrackingScreen(
                buddyId = buddyId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 🔔 Notifications
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

### Adım 3: Ana Menüye Buddy Sekmesi Ekleyin

`HomeScreen.kt` veya ana navigasyon menünüze buddy bölümü ekleyin:

```kotlin
// BottomNavigationBar veya Drawer'a ekleyin
NavigationBarItem(
    icon = { Icon(Icons.Default.People, "Buddy'ler") },
    label = { Text("Buddy'ler") },
    selected = currentRoute == Screen.BuddyList.route,
    onClick = { navController.navigate(Screen.BuddyList.route) }
)
```

### Adım 4: Bildirim Badge'ini Ekleyin

```kotlin
// TopAppBar'da bildirim ikonu
IconButton(
    onClick = { navController.navigate(Screen.Notifications.route) }
) {
    BadgedBox(
        badge = {
            if (unreadCount > 0) {
                Badge { Text(unreadCount.toString()) }
            }
        }
    ) {
        Icon(Icons.Default.Notifications, "Bildirimler")
    }
}
```

### Adım 5: Hilt Dependency Injection Ayarları

Repository'lerin Hilt ile inject edilmesi için module oluşturun:

```kotlin
// app/src/main/java/com/bardino/dozi/di/RepositoryModule.kt

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideBuddyRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): BuddyRepository {
        return BuddyRepository(auth, firestore)
    }

    @Provides
    @Singleton
    fun provideMedicationLogRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): MedicationLogRepository {
        return MedicationLogRepository(auth, firestore)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        functions: FirebaseFunctions,
        messaging: FirebaseMessaging
    ): NotificationRepository {
        return NotificationRepository(auth, firestore, functions, messaging)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}
```

### Adım 6: İlaç Hatırlatıcılarına Buddy Bildirimi Ekleyin

Mevcut hatırlatma sisteminize buddy bildirimlerini entegre edin:

```kotlin
// İlaç hatırlatması oluşturulduğunda
viewModelScope.launch {
    // Local notification göster
    NotificationHelper.showMedicationNotification(context, medicineName, dosage, time)

    // Buddy'lere de bildirim gönder
    notificationRepository.sendMedicationReminderToBuddies(
        medicineId = medicineId,
        medicineName = medicineName,
        dosage = dosage,
        time = time
    )
}
```

### Adım 7: İlaç Alındığında Log Kaydet

```kotlin
// İlaç alındığında MedicationLog oluştur
viewModelScope.launch {
    val log = MedicationLog(
        medicineId = medicineId,
        medicineName = medicineName,
        dosage = dosage,
        scheduledTime = Timestamp.now(),
        takenAt = Timestamp.now(),
        status = MedicationStatus.TAKEN
    )

    medicationLogRepository.createMedicationLog(log)
    // Cloud Function otomatik olarak buddy'lere bildirim gönderecek
}
```

---

## 🎨 UI/UX İyileştirme Önerileri

### 1. Ana Ekrana Buddy Widget Ekleyin

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    onClick = { navController.navigate(Screen.BuddyList.route) }
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.People, null)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Buddy'lerim", fontWeight = FontWeight.Bold)
            Text("${buddyCount} buddy takip ediyor", style = Typography.bodySmall)
        }
    }
}
```

### 2. İlaç Kartlarına Buddy Göstergesi

```kotlin
// Medicine card'da
if (medicine.sharedWithBuddies.isNotEmpty()) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp))
        Text(
            "${medicine.sharedWithBuddies.size} buddy ile paylaşıldı",
            style = Typography.labelSmall
        )
    }
}
```

### 3. Onboarding'e Buddy Tanıtımı Ekleyin

```kotlin
OnboardingPage(
    icon = Icons.Default.People,
    title = "Buddy Sistemi",
    description = "Sevdiklerinizi ekleyin, ilaç takibinizi birlikte yönetin",
    emoji = "🤝"
)
```

---

## 🔔 Push Notification Test Etme

### FCM Console'dan Test Bildirimi

1. Firebase Console > Cloud Messaging
2. "Send your first message"
3. Notification text girin
4. Target: Kullanıcı FCM token'ı
5. Additional options > Custom data:
   ```json
   {
     "type": "buddy_request",
     "fromUserName": "Test User"
   }
   ```

### Postman ile Test

```bash
POST https://fcm.googleapis.com/fcm/send
Headers:
  Authorization: key=YOUR_SERVER_KEY
  Content-Type: application/json

Body:
{
  "to": "USER_FCM_TOKEN",
  "data": {
    "type": "buddy_medication_reminder",
    "buddyName": "Ahmet",
    "medicineName": "Aspirin",
    "time": "14:00"
  },
  "priority": "high"
}
```

---

## 📊 Analytics Events (Opsiyonel)

Buddy sistem kullanımını takip etmek için:

```kotlin
// Firebase Analytics
firebaseAnalytics.logEvent("buddy_request_sent") {
    param("from_user_id", currentUserId)
    param("to_user_id", toUserId)
}

firebaseAnalytics.logEvent("buddy_request_accepted") {
    param("buddy_id", buddyId)
}

firebaseAnalytics.logEvent("buddy_medication_tracked") {
    param("buddy_id", buddyId)
    param("medicine_id", medicineId)
}
```

---

## 🐛 Sorun Giderme

### Bildirim Gelmiyor
1. FCM token kaydedildi mi? → `UserRepository.updateUserField("fcmToken", token)`
2. Bildirim izni var mı? → `POST_NOTIFICATIONS` permission
3. Cloud Functions çalışıyor mu? → Firebase Console > Functions
4. Doğru channel kullanılıyor mu? → `dozi_med_channel`

### Buddy İsteği Gönderilmiyor
1. Internet bağlantısı var mı?
2. Firestore rules doğru mu?
3. Kullanıcı giriş yapmış mı?
4. Hedef kullanıcı mevcut mu?

### Repository Injection Hatası
1. `@HiltViewModel` annotation'ı var mı?
2. `@Inject constructor` kullanıldı mı?
3. Hilt module'leri eklenmiş mi?
4. `@HiltAndroidApp` Application class'ta var mı?

---

## 📚 Kullanım Örnekleri

### Buddy Ekleme Akışı

```
1. Kullanıcı "Buddy Ekle" butonuna tıklar
2. Kod veya email ile arama yapar
3. Kullanıcı bulunur
4. Mesaj yazıp istek gönderir
5. Cloud Function otomatik bildirim gönderir
6. Alıcı bildirimi görür ve kabul eder
7. İki yönlü buddy ilişkisi oluşur
```

### İlaç Takibi Akışı

```
1. Kullanıcı ilacını alır
2. "Aldım" butonuna tıklar
3. MedicationLog oluşturulur (Firestore)
4. Cloud Function tetiklenir
5. Buddy'lere bildirim gönderilir
6. Buddy'ler bildirimi görür
7. Buddy takip ekranında log görünür
```

---

## ✨ Gelecek İyileştirmeler

1. **Buddy Grupları**: Birden fazla buddy'yi grup olarak yönetme
2. **Hatırlatma Paylaşımı**: Belirli hatırlatmaları buddy ile paylaşma
3. **Video Call**: Buddy ile görüntülü görüşme
4. **Günlük Rapor**: Buddy'ye günlük özet raporu
5. **Acil Durum**: Acil durum butonu ile tüm buddy'lere bildirim
6. **Gamification**: Buddy ile uyum oranı yarışması
7. **Chat**: Buddy ile mesajlaşma

---

## 📝 Kontrol Listesi

Entegrasyon tamamlandıktan sonra kontrol edin:

- [ ] Navigation route'ları eklendi
- [ ] UI ekranları mevcut
- [ ] Hilt injection çalışıyor
- [ ] Firebase kurulumu tamamlandı
- [ ] Cloud Functions deploy edildi
- [ ] Security rules güncellendi
- [ ] FCM token kayıt sistemi çalışıyor
- [ ] Test bildirimleri alınıyor
- [ ] Buddy ekleme/silme çalışıyor
- [ ] İlaç takibi çalışıyor
- [ ] Bildirim merkezi çalışıyor
- [ ] Ana menüde buddy bölümü var
- [ ] Onboarding güncellendi
- [ ] Analytics eventleri eklendi

---

## 🎯 Sonuç

Buddy sistemi artık kullanıma hazır! Kullanıcılarınız sevdiklerini ekleyip ilaç takibini birlikte yönetebilecekler.

Sorularınız için:
- Firebase Console: https://console.firebase.google.com
- Firebase Documentation: https://firebase.google.com/docs
- GitHub Issues: [Projenizin issue sayfası]

İyi kodlamalar! 🚀
