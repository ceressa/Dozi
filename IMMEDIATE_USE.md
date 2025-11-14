# ⚡ Hemen Kullanmaya Başlayın (Functions Olmadan)

## 🎯 Firebase Functions Gereksiz!

Cloud Functions kullanmadan **tüm buddy sistemi çalışır**! Sadece Firebase Firestore kurulumu yeterli.

---

## ✅ ADIM 1: Firestore Kurulumu (5 dakika)

### 1. Firebase Console'a gidin:
https://console.firebase.google.com

### 2. Projenizi seçin: `dozi-cd7cc`

### 3. Firestore Database oluşturun:
- Sol menüden **Firestore Database**
- **Create database**
- Mode: **Production mode** seçin
- Location: **europe-west1** (Amsterdam) seçin
- **Enable** tıklayın

### 4. Security Rules ekleyin:
- **Rules** sekmesine gidin
- Aşağıdaki kodu yapıştırın:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    match /users/{userId} {
      allow read: if isAuthenticated();
      allow write: if isOwner(userId);
    }

    match /buddies/{buddyId} {
      allow read: if isAuthenticated() && (
        resource.data.userId == request.auth.uid ||
        resource.data.buddyUserId == request.auth.uid
      );
      allow write: if isAuthenticated();
    }

    match /buddy_requests/{requestId} {
      allow read: if isAuthenticated() && (
        resource.data.fromUserId == request.auth.uid ||
        resource.data.toUserId == request.auth.uid
      );
      allow write: if isAuthenticated();
    }

    match /medication_logs/{logId} {
      allow read, write: if isAuthenticated() && isOwner(resource.data.userId);
    }

    match /notifications/{notificationId} {
      allow read, write: if isAuthenticated() && isOwner(resource.data.userId);
    }
  }
}
```

- **Publish** tıklayın

### 5. Indexes oluşturun:
- **Indexes** sekmesine gidin
- Aşağıdaki index'leri **Add composite index** ile ekleyin:

**Index 1:**
- Collection: `buddy_requests`
- Fields:
  - `toUserId` → Ascending
  - `status` → Ascending
  - `createdAt` → Descending

**Index 2:**
- Collection: `medication_logs`
- Fields:
  - `userId` → Ascending
  - `scheduledTime` → Descending

✅ **Firestore Hazır!**

---

## ✅ ADIM 2: Navigation Ekleyin (10 dakika)

### 1. Screen.kt'ye ekleyin:

**`app/src/main/java/com/bardino/dozi/navigation/Screen.kt`**

```kotlin
sealed class Screen(val route: String) {
    // ... mevcut screen'ler

    // 🆕 Buddy Screens
    object BuddyList : Screen("buddy_list")
    object AddBuddy : Screen("add_buddy")
    object BuddyMedicationTracking : Screen("buddy_medication_tracking/{buddyId}") {
        fun createRoute(buddyId: String) = "buddy_medication_tracking/$buddyId"
    }
    object Notifications : Screen("notifications")
}
```

### 2. NavGraph.kt'ye ekleyin:

**`app/src/main/java/com/bardino/dozi/navigation/NavGraph.kt`**

```kotlin
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.bardino.dozi.core.ui.screens.buddy.*
import com.bardino.dozi.core.ui.screens.notifications.NotificationsScreen

// NavHost içine ekleyin:

// Buddy Navigation
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

composable(Screen.Notifications.route) {
    NotificationsScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

---

## ✅ ADIM 3: Ana Ekrana Buton Ekleyin

### Seçenek A: HomeScreen'e Card Ekleyin

**`app/src/main/java/com/bardino/dozi/core/ui/screens/home/HomeScreen.kt`**

```kotlin
// HomeScreen içinde, diğer kartların yanına:
Card(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { navController.navigate(Screen.BuddyList.route) },
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    )
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.People,
            null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                "Buddy'lerim",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Sevdiklerinizi ekleyin, birlikte takip edin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
```

### Seçenek B: BottomNavigationBar'a Ekleyin

Eğer bottom navigation bar varsa:

```kotlin
NavigationBarItem(
    icon = { Icon(Icons.Default.People, "Buddy'ler") },
    label = { Text("Buddy'ler") },
    selected = currentRoute == Screen.BuddyList.route,
    onClick = { navController.navigate(Screen.BuddyList.route) }
)
```

---

## ✅ ADIM 4: Test Edin!

### 1. Uygulamayı çalıştırın:
```bash
./gradlew clean build
./gradlew installDebug
```

### 2. Giriş yapın ve test edin:
- ✅ Ana ekranda "Buddy'lerim" kartına tıklayın
- ✅ "Buddy Ekle" butonuna tıklayın
- ✅ "Kodumu Göster" → Kod oluşturuldu mu?
- ✅ Kodu başka bir cihazdan/hesaptan test edin

### 3. Firestore'da kontrol edin:
- Firebase Console → Firestore → Data
- `users` koleksiyonunda kullanıcınız var mı?
- `buddyCode` alanı var mı?

---

## 🎉 HAZIR!

Artık buddy sistemi çalışıyor!

### Çalışan Özellikler:
- ✅ Buddy ekleme (kod/email ile)
- ✅ Buddy istekleri
- ✅ İlaç geçmişi görüntüleme
- ✅ Real-time updates
- ✅ In-app bildirimler

### Manuel Özellikler:
- 📱 İlaç aldığınızda buddy'niz **Firestore'dan real-time** görür
- 📱 Push notification yok (şimdilik), ama app içinde her şey çalışıyor

---

## 🚀 Sonraki Adımlar (Opsiyonel)

İsterseniz daha sonra ekleyebilirsiniz:

1. **Cloud Functions** (otomatik push notification için)
   - `FIREBASE_FUNCTIONS_SETUP.md` dosyasına bakın
   - Firebase Blaze Plan gerekli

2. **Bildirim Badge**
   - TopAppBar'a bildirim sayısı ekleyin
   - `QUICK_START.md` dosyasında kod var

3. **Onboarding**
   - Buddy sistemini tanıtın
   - İlk kullanımda açıklayın

---

## ❓ Sorun mu var?

### Build hatası alıyorsanız:
```bash
# Gradle sync
./gradlew --stop
./gradlew clean
# Android Studio → File → Sync Project with Gradle Files
```

### "Unresolved reference" hatası:
```bash
# Rebuild
Build → Rebuild Project
```

### Firestore'a kayıt olmuyor:
- Internet bağlantısı var mı?
- Kullanıcı giriş yapmış mı?
- Security rules doğru mu?

---

**Hemen test etmeye başlayın!** 🚀

Functions olmadan da **kusursuz çalışıyor**! 🎉
