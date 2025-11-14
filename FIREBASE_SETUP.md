# 🔥 Firebase Buddy Sistemi ve Bildirim Kurulumu

## 📋 İçindekiler
1. [Firestore Database Yapısı](#firestore-database-yapısı)
2. [Firebase Console Adımları](#firebase-console-adımları)
3. [Cloud Functions Kurulumu](#cloud-functions-kurulumu)
4. [Security Rules](#security-rules)
5. [FCM Token Yönetimi](#fcm-token-yönetimi)

---

## 🗄️ Firestore Database Yapısı

### 1. `users` Koleksiyonu
Kullanıcı bilgilerini saklar.

```
users/{userId}
  ├── uid: String
  ├── name: String
  ├── email: String
  ├── photoUrl: String
  ├── createdAt: Timestamp
  ├── planType: String ("free" | "premium")
  ├── timezone: String
  ├── language: String
  ├── vibration: Boolean
  ├── theme: String
  ├── voiceGender: String
  ├── onboardingCompleted: Boolean
  ├── fcmToken: String (Push notification için)
  └── buddyCode: String (Unique 6-digit kod)
```

### 2. `buddies` Koleksiyonu
Kullanıcılar arası buddy ilişkileri.

```
buddies/{buddyId}
  ├── userId: String (İsteği gönderen)
  ├── buddyUserId: String (Kabul eden)
  ├── status: String ("active" | "paused" | "removed")
  ├── createdAt: Timestamp
  ├── nickname: String? (Buddy için özel isim)
  ├── permissions: Map
  │   ├── canViewReminders: Boolean
  │   ├── canReceiveNotifications: Boolean
  │   ├── canEditReminders: Boolean
  │   └── canViewMedicationHistory: Boolean
  ├── notificationPreferences: Map
  │   ├── onMedicationTime: Boolean
  │   ├── onMedicationTaken: Boolean
  │   ├── onMedicationSkipped: Boolean
  │   └── onMedicationMissed: Boolean
  └── lastInteraction: Timestamp
```

### 3. `buddy_requests` Koleksiyonu
Bekleyen buddy istekleri.

```
buddy_requests/{requestId}
  ├── fromUserId: String
  ├── toUserId: String
  ├── fromUserName: String
  ├── fromUserPhoto: String
  ├── toUserEmail: String? (Email ile gönderildiyse)
  ├── toBuddyCode: String? (Kod ile gönderildiyse)
  ├── status: String ("pending" | "accepted" | "rejected" | "expired")
  ├── message: String?
  ├── createdAt: Timestamp
  ├── expiresAt: Timestamp (7 gün sonra)
  └── respondedAt: Timestamp?
```

### 4. `reminders` Koleksiyonu
İlaç hatırlatmaları (kullanıcı bazlı).

```
reminders/{reminderId}
  ├── userId: String
  ├── medicineId: String
  ├── medicineName: String
  ├── dosage: String
  ├── frequency: String ("daily" | "weekly" | "as_needed")
  ├── times: Array<String> (["08:00", "20:00"])
  ├── days: Array<Int>? (Weekly için: [1,2,3,4,5])
  ├── startDate: Timestamp
  ├── endDate: Timestamp?
  ├── isActive: Boolean
  ├── isMuted: Boolean
  ├── reminderSound: String
  ├── vibrationPattern: String
  ├── notes: String?
  ├── createdAt: Timestamp
  ├── updatedAt: Timestamp
  └── sharedWithBuddies: Array<String> (Buddy userId'leri)
```

### 5. `medication_logs` Koleksiyonu
İlaç alma geçmişi.

```
medication_logs/{logId}
  ├── userId: String
  ├── reminderId: String
  ├── medicineName: String
  ├── dosage: String
  ├── scheduledTime: Timestamp
  ├── takenAt: Timestamp?
  ├── status: String ("taken" | "skipped" | "missed" | "snoozed")
  ├── notes: String?
  ├── sideEffects: Array<String>?
  ├── mood: String?
  ├── location: GeoPoint?
  └── createdAt: Timestamp
```

### 6. `notifications` Koleksiyonu
Bildirim geçmişi ve buddy bildirimleri.

```
notifications/{notificationId}
  ├── userId: String (Bildirimi alan)
  ├── type: String ("buddy_request" | "medication_reminder" | "buddy_alert" | "medication_taken")
  ├── title: String
  ├── body: String
  ├── data: Map (Ekstra veriler)
  ├── isRead: Boolean
  ├── isSent: Boolean
  ├── sentAt: Timestamp?
  ├── readAt: Timestamp?
  ├── createdAt: Timestamp
  ├── priority: String ("high" | "normal" | "low")
  └── actionUrl: String? (Deep link)
```

---

## 🚀 Firebase Console Adımları

### Adım 1: Firestore Database Oluşturma

1. **Firebase Console'a git**: https://console.firebase.google.com
2. Projenizi seçin: `dozi-cd7cc`
3. Sol menüden **Firestore Database** seçin
4. **Create Database** butonuna tıklayın
5. **Production mode** seçin (Security rules'u sonra ekleyeceğiz)
6. Location: `europe-west1` (Amsterdam) seçin (GDPR uyumluluğu için)
7. **Enable** butonuna tıklayın

### Adım 2: Koleksiyonları Oluşturma

Firestore otomatik olarak koleksiyonları oluşturacak, ancak test için:

1. **Start collection** butonuna tıklayın
2. Collection ID: `users` yazın
3. İlk dokümanı manuel ekleyin (test için)

### Adım 3: Indexes Oluşturma

Firestore Console > **Indexes** sekmesinde:

#### Composite Index 1: Buddy Requests
```
Collection: buddy_requests
Fields:
  - toUserId (Ascending)
  - status (Ascending)
  - createdAt (Descending)
```

#### Composite Index 2: Reminders
```
Collection: reminders
Fields:
  - userId (Ascending)
  - isActive (Ascending)
  - startDate (Ascending)
```

#### Composite Index 3: Medication Logs
```
Collection: medication_logs
Fields:
  - userId (Ascending)
  - scheduledTime (Descending)
  - status (Ascending)
```

#### Composite Index 4: Notifications
```
Collection: notifications
Fields:
  - userId (Ascending)
  - isRead (Ascending)
  - createdAt (Descending)
```

### Adım 4: Firebase Cloud Messaging Kurulumu

1. Sol menüden **Project Settings** (⚙️) > **Cloud Messaging** sekmesi
2. **Cloud Messaging API (Legacy)** aktif olmalı
3. **Server key** notunu alın (Cloud Functions için gerekli)

---

## ⚡ Cloud Functions Kurulumu

### Önkoşullar
```bash
# Node.js ve npm kurulu olmalı
node --version  # v18+ önerilir
npm --version

# Firebase CLI'yi global olarak kurun
npm install -g firebase-tools

# Firebase'e login olun
firebase login
```

### Adım 1: Functions Dizini Oluşturma
```bash
# Proje dizininde
mkdir firebase-functions
cd firebase-functions

# Firebase Functions'ı başlat
firebase init functions

# Sorulara cevaplar:
# - Use existing project: dozi-cd7cc
# - Language: TypeScript
# - ESLint: Yes
# - Install dependencies: Yes
```

### Adım 2: Cloud Functions Kodları

`functions/src/index.ts` dosyasını aşağıdaki gibi düzenleyin:

```typescript
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Buddy isteği gönderildiğinde bildirim gönder
 */
export const onBuddyRequestCreated = functions.firestore
  .document("buddy_requests/{requestId}")
  .onCreate(async (snap, context) => {
    const request = snap.data();

    // Alıcının FCM token'ını al
    const toUserDoc = await db.collection("users").doc(request.toUserId).get();
    const toUser = toUserDoc.data();

    if (!toUser || !toUser.fcmToken) {
      console.log("Kullanıcı FCM token'ı yok:", request.toUserId);
      return null;
    }

    // Bildirim gönder
    const message = {
      token: toUser.fcmToken,
      notification: {
        title: "🤝 Yeni Buddy İsteği",
        body: `${request.fromUserName} seni buddy olarak eklemek istiyor!`,
      },
      data: {
        type: "buddy_request",
        requestId: context.params.requestId,
        fromUserId: request.fromUserId,
      },
      android: {
        priority: "high" as const,
        notification: {
          sound: "default",
          channelId: "dozi_med_channel",
        },
      },
    };

    try {
      await messaging.send(message);
      console.log("Buddy isteği bildirimi gönderildi:", request.toUserId);

      // Notification kaydı oluştur
      await db.collection("notifications").add({
        userId: request.toUserId,
        type: "buddy_request",
        title: message.notification.title,
        body: message.notification.body,
        data: message.data,
        isRead: false,
        isSent: true,
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        priority: "high",
      });
    } catch (error) {
      console.error("Bildirim gönderilemedi:", error);
    }

    return null;
  });

/**
 * İlaç hatırlatma zamanı geldiğinde buddy'lere bildirim gönder
 */
export const sendMedicationReminderToBuddies = functions.https.onCall(
  async (data, context) => {
    // Auth kontrolü
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Kullanıcı giriş yapmamış"
      );
    }

    const { reminderId, medicineName, dosage, time } = data;
    const userId = context.auth.uid;

    // Kullanıcının buddy'lerini al
    const buddiesSnapshot = await db
      .collection("buddies")
      .where("userId", "==", userId)
      .where("status", "==", "active")
      .get();

    const promises: Promise<any>[] = [];

    for (const buddyDoc of buddiesSnapshot.docs) {
      const buddy = buddyDoc.data();

      // Buddy'nin bildirim almak isteyip istemediğini kontrol et
      if (!buddy.notificationPreferences?.onMedicationTime) {
        continue;
      }

      // Buddy'nin FCM token'ını al
      const buddyUserDoc = await db.collection("users").doc(buddy.buddyUserId).get();
      const buddyUser = buddyUserDoc.data();

      if (!buddyUser || !buddyUser.fcmToken) {
        continue;
      }

      // Kullanıcı bilgisini al
      const userDoc = await db.collection("users").doc(userId).get();
      const user = userDoc.data();

      const message = {
        token: buddyUser.fcmToken,
        notification: {
          title: "💊 Buddy İlaç Hatırlatması",
          body: `${user?.name || "Buddy'niz"} - ${medicineName} ${dosage} alma zamanı (${time})`,
        },
        data: {
          type: "buddy_medication_reminder",
          userId: userId,
          reminderId: reminderId,
          medicineName: medicineName,
        },
        android: {
          priority: "high" as const,
          notification: {
            sound: "default",
            channelId: "dozi_med_channel",
          },
        },
      };

      promises.push(
        messaging.send(message).then(async () => {
          // Notification kaydı oluştur
          await db.collection("notifications").add({
            userId: buddy.buddyUserId,
            type: "buddy_alert",
            title: message.notification.title,
            body: message.notification.body,
            data: message.data,
            isRead: false,
            isSent: true,
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            priority: "high",
          });
        })
      );
    }

    await Promise.all(promises);
    return { success: true, sentCount: promises.length };
  }
);

/**
 * İlaç alındığında buddy'lere bildirim gönder
 */
export const onMedicationTaken = functions.firestore
  .document("medication_logs/{logId}")
  .onCreate(async (snap, context) => {
    const log = snap.data();

    // Sadece "taken" status için bildirim gönder
    if (log.status !== "taken") {
      return null;
    }

    const userId = log.userId;

    // Kullanıcının buddy'lerini al
    const buddiesSnapshot = await db
      .collection("buddies")
      .where("userId", "==", userId)
      .where("status", "==", "active")
      .get();

    const promises: Promise<any>[] = [];

    for (const buddyDoc of buddiesSnapshot.docs) {
      const buddy = buddyDoc.data();

      // Buddy'nin bildirim almak isteyip istemediğini kontrol et
      if (!buddy.notificationPreferences?.onMedicationTaken) {
        continue;
      }

      // Buddy'nin FCM token'ını al
      const buddyUserDoc = await db.collection("users").doc(buddy.buddyUserId).get();
      const buddyUser = buddyUserDoc.data();

      if (!buddyUser || !buddyUser.fcmToken) {
        continue;
      }

      // Kullanıcı bilgisini al
      const userDoc = await db.collection("users").doc(userId).get();
      const user = userDoc.data();

      const message = {
        token: buddyUser.fcmToken,
        notification: {
          title: "✅ İlaç Alındı",
          body: `${user?.name || "Buddy'niz"} ${log.medicineName} ilacını aldı`,
        },
        data: {
          type: "medication_taken",
          userId: userId,
          logId: context.params.logId,
          medicineName: log.medicineName,
        },
        android: {
          priority: "default" as const,
          notification: {
            sound: "default",
            channelId: "dozi_med_channel",
          },
        },
      };

      promises.push(
        messaging.send(message).then(async () => {
          // Notification kaydı oluştur
          await db.collection("notifications").add({
            userId: buddy.buddyUserId,
            type: "medication_taken",
            title: message.notification.title,
            body: message.notification.body,
            data: message.data,
            isRead: false,
            isSent: true,
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            priority: "normal",
          });
        })
      );
    }

    await Promise.all(promises);
    return null;
  });

/**
 * İlaç kaçırıldığında buddy'lere bildirim gönder
 */
export const checkMissedMedications = functions.pubsub
  .schedule("every 15 minutes")
  .onRun(async (context) => {
    const now = admin.firestore.Timestamp.now();
    const fifteenMinutesAgo = new Date(now.toMillis() - 15 * 60 * 1000);

    // Son 15 dakikada kaçırılan ilaçları bul
    const missedLogsSnapshot = await db
      .collection("medication_logs")
      .where("status", "==", "missed")
      .where("scheduledTime", ">", fifteenMinutesAgo)
      .get();

    const promises: Promise<any>[] = [];

    for (const logDoc of missedLogsSnapshot.docs) {
      const log = logDoc.data();
      const userId = log.userId;

      // Kullanıcının buddy'lerini al
      const buddiesSnapshot = await db
        .collection("buddies")
        .where("userId", "==", userId)
        .where("status", "==", "active")
        .get();

      for (const buddyDoc of buddiesSnapshot.docs) {
        const buddy = buddyDoc.data();

        // Buddy'nin bildirim almak isteyip istemediğini kontrol et
        if (!buddy.notificationPreferences?.onMedicationMissed) {
          continue;
        }

        // Buddy'nin FCM token'ını al
        const buddyUserDoc = await db.collection("users").doc(buddy.buddyUserId).get();
        const buddyUser = buddyUserDoc.data();

        if (!buddyUser || !buddyUser.fcmToken) {
          continue;
        }

        // Kullanıcı bilgisini al
        const userDoc = await db.collection("users").doc(userId).get();
        const user = userDoc.data();

        const message = {
          token: buddyUser.fcmToken,
          notification: {
            title: "⚠️ İlaç Kaçırıldı",
            body: `${user?.name || "Buddy'niz"} ${log.medicineName} ilacını kaçırdı`,
          },
          data: {
            type: "medication_missed",
            userId: userId,
            logId: logDoc.id,
            medicineName: log.medicineName,
          },
          android: {
            priority: "high" as const,
            notification: {
              sound: "default",
              channelId: "dozi_med_channel",
            },
          },
        };

        promises.push(messaging.send(message));
      }
    }

    await Promise.all(promises);
    console.log(`${promises.length} buddy bildirimi gönderildi`);
    return null;
  });
```

### Adım 3: Deploy Cloud Functions

```bash
# Firebase Functions'ı deploy et
cd firebase-functions
npm run deploy

# Veya sadece functions'ları deploy et
firebase deploy --only functions
```

---

## 🔒 Security Rules

Firestore Security Rules'u güncelleyin:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    function isBuddy(userId) {
      return isAuthenticated() && exists(
        /databases/$(database)/documents/buddies/$(request.auth.uid + '_' + userId)
      );
    }

    // Users collection
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && request.auth.uid == userId;
      allow update: if isOwner(userId);
      allow delete: if isOwner(userId);
    }

    // Buddies collection
    match /buddies/{buddyId} {
      allow read: if isAuthenticated() && (
        resource.data.userId == request.auth.uid ||
        resource.data.buddyUserId == request.auth.uid
      );
      allow create: if isAuthenticated();
      allow update: if isAuthenticated() && (
        resource.data.userId == request.auth.uid ||
        resource.data.buddyUserId == request.auth.uid
      );
      allow delete: if isAuthenticated() && (
        resource.data.userId == request.auth.uid ||
        resource.data.buddyUserId == request.auth.uid
      );
    }

    // Buddy requests collection
    match /buddy_requests/{requestId} {
      allow read: if isAuthenticated() && (
        resource.data.fromUserId == request.auth.uid ||
        resource.data.toUserId == request.auth.uid
      );
      allow create: if isAuthenticated() && request.auth.uid == request.resource.data.fromUserId;
      allow update: if isAuthenticated() && (
        resource.data.fromUserId == request.auth.uid ||
        resource.data.toUserId == request.auth.uid
      );
      allow delete: if isAuthenticated() && resource.data.fromUserId == request.auth.uid;
    }

    // Reminders collection
    match /reminders/{reminderId} {
      allow read: if isAuthenticated() && (
        isOwner(resource.data.userId) ||
        isBuddy(resource.data.userId)
      );
      allow write: if isAuthenticated() && isOwner(resource.data.userId);
    }

    // Medication logs collection
    match /medication_logs/{logId} {
      allow read: if isAuthenticated() && (
        isOwner(resource.data.userId) ||
        isBuddy(resource.data.userId)
      );
      allow create: if isAuthenticated() && isOwner(request.resource.data.userId);
      allow update: if isAuthenticated() && isOwner(resource.data.userId);
      allow delete: if isAuthenticated() && isOwner(resource.data.userId);
    }

    // Notifications collection
    match /notifications/{notificationId} {
      allow read: if isAuthenticated() && isOwner(resource.data.userId);
      allow create: if isAuthenticated();
      allow update: if isAuthenticated() && isOwner(resource.data.userId);
      allow delete: if isAuthenticated() && isOwner(resource.data.userId);
    }
  }
}
```

Firebase Console'da **Firestore Database** > **Rules** sekmesinde bu kuralları yapıştırın ve **Publish** edin.

---

## 📱 FCM Token Yönetimi

### Android Tarafında FCM Token Kaydetme

`DoziMessagingService.kt` dosyanızda `onNewToken` fonksiyonu zaten mevcut ve FCM token'ı Firestore'a kaydediyor. ✅

### Test Etme

1. **FCM Token Kontrolü:**
```kotlin
// MainActivity veya LoginScreen'de
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        Log.d("FCM", "Token: $token")
    }
}
```

2. **Test Bildirimi Gönderme:**
Firebase Console > Cloud Messaging > Send your first message

---

## ✅ Kurulum Kontrol Listesi

- [ ] Firestore Database oluşturuldu
- [ ] Koleksiyonlar hazır
- [ ] Indexes oluşturuldu
- [ ] Cloud Functions kuruldu ve deploy edildi
- [ ] Security Rules güncellendi ve publish edildi
- [ ] FCM token kayıt sistemi çalışıyor
- [ ] Test bildirimi gönderildi ve alındı

---

## 🎯 Sonraki Adımlar

1. Android uygulamasında data modellerini oluştur
2. Repository ve ViewModel katmanlarını ekle
3. UI ekranlarını tasarla
4. Bildirim sistemi entegrasyonunu tamamla
5. Test senaryolarını çalıştır

---

## 🆘 Sorun Giderme

### Cloud Functions çalışmıyor
```bash
# Logs kontrol et
firebase functions:log

# Yeniden deploy et
firebase deploy --only functions --force
```

### FCM bildirimi gelmiyor
1. FCM token doğru kaydedilmiş mi kontrol et
2. Notification izinleri verilmiş mi?
3. Doğru notification channel kullanılıyor mu?
4. Security rules doğru mu?

### Firestore hataları
1. Security rules'u kontrol et
2. Index'ler oluşturulmuş mu?
3. Network bağlantısı var mı?

---

**Not:** Bu kurulum GDPR ve veri güvenliği için optimize edilmiştir. Production'a geçmeden önce güvenlik testlerini mutlaka yapın.
