# 🚀 Buddy Sistemi - Hızlı Başlangıç

## ✅ Eklenen Dependency'ler

```kotlin
// build.gradle.kts - TAMAMLANDI ✅
implementation("com.google.firebase:firebase-functions-ktx")  // Cloud Functions
implementation("io.coil-kt:coil-compose:2.5.0")                // Coil Image Loading
```

## ✅ Oluşturulan Hilt Modules

```kotlin
// di/FirebaseModule.kt - TAMAMLANDI ✅
// di/RepositoryModule.kt - TAMAMLANDI ✅
```

---

## 🔧 Yapmanız Gerekenler (3 Adım)

### 1️⃣ Navigation Route'larını Ekleyin

**`app/src/main/java/com/bardino/dozi/navigation/Screen.kt`**

```kotlin
sealed class Screen(val route: String) {
    // Mevcut screen'ler...
    object Home : Screen("home")
    object Settings : Screen("settings")
    // ... diğerleri

    // 🆕 BUNLARI EKLEYİN:
    object BuddyList : Screen("buddy_list")
    object AddBuddy : Screen("add_buddy")
    object BuddyMedicationTracking : Screen("buddy_medication_tracking/{buddyId}") {
        fun createRoute(buddyId: String) = "buddy_medication_tracking/$buddyId"
    }
    object Notifications : Screen("notifications")
}
```

---

### 2️⃣ NavGraph'a Composable'ları Ekleyin

**`app/src/main/java/com/bardino/dozi/navigation/NavGraph.kt`**

```kotlin
import androidx.navigation.NavType
import androidx.navigation.navArgument
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

        // 🆕 BUDDY NAVIGATION
        composable(Screen.BuddyList.route) {
            BuddyListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddBuddy = {
                    navController.navigate(Screen.AddBuddy.route)
                },
                onNavigateToBuddyDetail = { buddyId ->
                    navController.navigate(
                        Screen.BuddyMedicationTracking.createRoute(buddyId)
                    )
                }
            )
        }

        composable(Screen.AddBuddy.route) {
            AddBuddyScreen(
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

        // 🆕 NOTIFICATIONS
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

---

### 3️⃣ Ana Ekrana Buddy Butonu Ekleyin

**Seçenek A: Bottom Navigation Bar**

```kotlin
// MainActivity veya HomeScreen'de
BottomNavigationBar(
    items = listOf(
        BottomNavItem(
            icon = Icons.Default.Home,
            label = "Ana Sayfa",
            route = Screen.Home.route
        ),
        // 🆕 BUDDY BUTONU
        BottomNavItem(
            icon = Icons.Default.People,
            label = "Buddy'ler",
            route = Screen.BuddyList.route
        ),
        BottomNavItem(
            icon = Icons.Default.Settings,
            label = "Ayarlar",
            route = Screen.Settings.route
        )
    )
)
```

**Seçenek B: Drawer Menu**

```kotlin
NavigationDrawerItem(
    icon = { Icon(Icons.Default.People, null) },
    label = { Text("Buddy'lerim") },
    selected = currentRoute == Screen.BuddyList.route,
    onClick = { navController.navigate(Screen.BuddyList.route) }
)
```

**Seçenek C: HomeScreen Card**

```kotlin
// HomeScreen.kt içinde
Card(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { navController.navigate(Screen.BuddyList.route) }
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.People, null, modifier = Modifier.size(32.dp))
        Column {
            Text("Buddy'lerim", style = MaterialTheme.typography.titleMedium)
            Text("Sevdiklerinizi ekleyin", style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

---

## 🔔 Bildirim Badge Ekleyin (Opsiyonel)

**TopAppBar'a bildirim ikonu:**

```kotlin
@Composable
fun TopAppBar() {
    val viewModel: NotificationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    TopAppBar(
        title = { Text("Dozi") },
        actions = {
            // 🆕 BİLDİRİM İKONU
            IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                BadgedBox(
                    badge = {
                        if (uiState.unreadCount > 0) {
                            Badge { Text(uiState.unreadCount.toString()) }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, "Bildirimler")
                }
            }
        }
    )
}
```

---

## 🔥 Firebase Kurulumu

### 1. Firebase Console'da:

1. https://console.firebase.google.com
2. Projeniz: `dozi-cd7cc`
3. **Firestore Database** → Create Database → Production Mode → europe-west1
4. **Cloud Functions** → Get Started

### 2. Cloud Functions Deploy:

```bash
# Proje dizininde
mkdir firebase-functions
cd firebase-functions

# Firebase CLI
npm install -g firebase-tools
firebase login
firebase init functions

# TypeScript seçin
# Proje: dozi-cd7cc

# FIREBASE_SETUP.md'deki Cloud Functions kodunu kopyalayın
# functions/src/index.ts

# Deploy
npm run deploy
```

### 3. Security Rules:

Firebase Console → Firestore → Rules → `FIREBASE_SETUP.md`'deki rules'u yapıştırın

### 4. Indexes:

Firestore Console → Indexes → `FIREBASE_SETUP.md`'deki index'leri oluşturun

---

## 🧪 Test Etme

### 1. FCM Token Test:

```kotlin
// MainActivity onCreate içinde
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        Log.d("FCM", "Token: $token")
        Toast.makeText(this, "FCM Token alındı", Toast.LENGTH_SHORT).show()
    }
}
```

### 2. Buddy Ekleme Test:

1. Uygulamayı çalıştırın
2. "Buddy Ekle" → "Kodumu Göster"
3. Kodu kopyalayın
4. Başka bir cihazdan/hesaptan kodu girin
5. İstek gönderildi mi kontrol edin

### 3. Bildirim Test:

Firebase Console → Cloud Messaging → "Send test message"

---

## 📦 Oluşturulan Dosyalar

### Data Layer ✅
- ✅ `Buddy.kt`, `BuddyRequest.kt`, `Reminder.kt`, `MedicationLog.kt`, `DoziNotification.kt`
- ✅ `BuddyRepository.kt`, `MedicationLogRepository.kt`, `NotificationRepository.kt`

### Presentation Layer ✅
- ✅ `BuddyViewModel.kt`, `NotificationViewModel.kt`
- ✅ `BuddyListScreen.kt`, `AddBuddyScreen.kt`, `BuddyMedicationTrackingScreen.kt`, `NotificationsScreen.kt`

### DI ✅
- ✅ `di/FirebaseModule.kt`, `di/RepositoryModule.kt`

### Dependencies ✅
- ✅ Coil (Image Loading)
- ✅ Firebase Functions

---

## ⚠️ Sorun Giderme

### "Unresolved reference: BuddyListScreen"
→ File > Sync Project with Gradle Files

### "Cannot resolve symbol BuddyViewModel"
→ Build > Rebuild Project

### Coil AsyncImage çalışmıyor
→ Build.gradle.kts'de `io.coil-kt:coil-compose:2.5.0` olduğundan emin olun

### Hilt injection hatası
→ `@HiltAndroidApp` Application class'ta var mı kontrol edin (DoziApplication.kt)

### FCM bildirimi gelmiyor
→ `POST_NOTIFICATIONS` permission verildi mi?
→ FCM token kaydedildi mi? (Firestore users koleksiyonunda kontrol edin)

---

## 📚 Detaylı Dokümantasyon

- **Firebase Kurulumu**: `FIREBASE_SETUP.md`
- **Entegrasyon Rehberi**: `BUDDY_SYSTEM_INTEGRATION.md`
- **Hızlı Özet**: `BUDDY_SYSTEM_README.md`

---

## ✨ Tamamlandı!

Yukarıdaki 3 adımı tamamladığınızda buddy sistemi kullanıma hazır! 🎉

**Sonraki adımlar:**
1. ✅ Dependencies eklendi
2. ✅ Hilt modules oluşturuldu
3. ⏳ Navigation ekleyin (yukarıdaki kod örnekleri)
4. ⏳ Firebase kurulumu yapın
5. ⏳ Test edin

---

**Yardıma ihtiyacınız olursa:** `BUDDY_SYSTEM_INTEGRATION.md`
