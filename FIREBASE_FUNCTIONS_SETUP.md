# 🔥 Firebase Functions Kurulum - Adım Adım

## ⚠️ ÖNEMLİ NOT

Firebase Functions kullanmak için **Blaze Plan** (ödeme planı) gereklidir.
Eğer şu an ücretsiz plan kullanıyorsanız, **Functions'sız da çalışabilir**!

---

## 🎯 İki Seçeneğiniz Var

### Seçenek 1: Functions İLE (Önerilen - Tam Özellikler) 💰
- ✅ Otomatik buddy bildirimleri
- ✅ İlaç kaçırma kontrolü (15 dk aralıkla)
- ✅ Buddy isteği otomatik bildirimleri
- ❌ Firebase Blaze Plan gerekli (kullandıkça öde)

### Seçenek 2: Functions OLMADAN (Ücretsiz) 🆓
- ✅ Tüm buddy özellikleri çalışır
- ✅ Manuel bildirimler (app içinden)
- ✅ Firestore real-time updates
- ❌ Otomatik server-side bildirimler yok

---

## 🚀 SEÇENEK 1: Cloud Functions Kurulumu

### Adım 1: Firebase CLI Kurulumu

```bash
# Node.js kurulu olmalı (v18+)
node --version  # Kontrol et

# Firebase CLI'yi global kur
npm install -g firebase-tools

# Firebase'e login ol
firebase login
```

### Adım 2: Functions Başlatma

```bash
# Proje klasörüne git
cd C:\Users\Ufuk\AndroidStudioProjects\Dozi

# Firebase Functions'ı başlat
firebase init functions

# Sorular:
# ? Please select an option: Use an existing project
# ? Select a default Firebase project: dozi-cd7cc
# ? What language would you like to use: TypeScript
# ? Do you want to use ESLint: Yes
# ? Do you want to install dependencies now: Yes
```

### Adım 3: Functions Kodunu Ekle

**`firebase-functions/functions/src/index.ts`** dosyasını açın ve aşağıdaki kodu yapıştırın:

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
 * İlaç alındığında buddy'lere bildirim gönder
 */
export const onMedicationTaken = functions.firestore
  .document("medication_logs/{logId}")
  .onCreate(async (snap, context) => {
    const log = snap.data();

    // Sadece "taken" status için bildirim gönder
    if (log.status !== "TAKEN") {
      return null;
    }

    const userId = log.userId;

    // Kullanıcının buddy'lerini al
    const buddiesSnapshot = await db
      .collection("buddies")
      .where("userId", "==", userId)
      .where("status", "==", "ACTIVE")
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

    const { medicineId, medicineName, dosage, time } = data;
    const userId = context.auth.uid;

    // Kullanıcının buddy'lerini al
    const buddiesSnapshot = await db
      .collection("buddies")
      .where("userId", "==", userId)
      .where("status", "==", "ACTIVE")
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
          medicineId: medicineId,
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
```

### Adım 4: Deploy

```bash
cd firebase-functions/functions

# Dependencies kur (ilk kez)
npm install

# TypeScript compile et
npm run build

# Deploy
firebase deploy --only functions
```

### Adım 5: Firebase Console'da Kontrol

1. https://console.firebase.google.com
2. Projeniz: dozi-cd7cc
3. Functions sekmesi
4. 3 function görmelisiniz:
   - onBuddyRequestCreated
   - onMedicationTaken
   - sendMedicationReminderToBuddies

---

## 🆓 SEÇENEK 2: Functions Olmadan Kullanım (Önerilen Başlangıç için)

Functions olmadan da **tüm temel özellikler çalışır**! Sadece bazı otomatik bildirimler manuel olur.

### Çalışan Özellikler:
- ✅ Buddy ekleme/silme
- ✅ Buddy istekleri
- ✅ İlaç geçmişi görüntüleme
- ✅ Real-time updates (Firestore)
- ✅ In-app bildirimler

### Manuel Olacak:
- 📱 Buddy'ye bildirim göndermek için app içinden butona tıklamanız gerekir
- 📱 İlaç hatırlatmalar sadece kendi cihazınızda çalışır

### Nasıl Çalışır?

Android app'iniz zaten tüm gerekli kodu içeriyor:

```kotlin
// Buddy'lere manuel bildirim gönderme
viewModel.sendMedicationReminderToBuddies(
    medicineId = medicineId,
    medicineName = medicineName,
    dosage = dosage,
    time = time
)
```

**Şu an için bu seçenek yeterli!** Daha sonra Blaze Plan'e geçip Functions ekleyebilirsiniz.

---

## 📋 Hangisini Seçmeliyim?

### Hemen Başlamak İstiyorsanız: Seçenek 2 (Functions Olmadan) ✅
- Ücretsiz
- Hemen test edebilirsiniz
- Tüm temel özellikler çalışır

### Production İçin: Seçenek 1 (Functions İle)
- Profesyonel
- Otomatik bildirimler
- Ölçeklenebilir

---

## 🎯 ŞİMDİ NE YAPMALI?

### 1. Firestore Kurulumu (Her İki Seçenek İçin Gerekli)

```bash
# Firebase Console → Firestore Database → Create Database
# Mode: Production
# Location: europe-west1
```

### 2. Security Rules Ekle

Firebase Console → Firestore → Rules:

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

    // Users
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow write: if isOwner(userId);
    }

    // Buddies
    match /buddies/{buddyId} {
      allow read: if isAuthenticated() && (
        resource.data.userId == request.auth.uid ||
        resource.data.buddyUserId == request.auth.uid
      );
      allow write: if isAuthenticated();
    }

    // Buddy requests
    match /buddy_requests/{requestId} {
      allow read: if isAuthenticated() && (
        resource.data.fromUserId == request.auth.uid ||
        resource.data.toUserId == request.auth.uid
      );
      allow write: if isAuthenticated();
    }

    // Medication logs
    match /medication_logs/{logId} {
      allow read, write: if isAuthenticated() && isOwner(resource.data.userId);
    }

    // Notifications
    match /notifications/{notificationId} {
      allow read, write: if isAuthenticated() && isOwner(resource.data.userId);
    }
  }
}
```

### 3. Navigation Ekle

`QUICK_START.md` dosyasındaki 3 adımı takip edin.

---

## ❓ SSS

**S: Functions olmadan buddy sistemi çalışır mı?**
C: Evet! Tüm temel özellikler çalışır. Sadece otomatik server-side bildirimler olmaz.

**S: Functions kurulumu zorunlu mu?**
C: Hayır! Başlangıç için Functions olmadan devam edebilirsiniz.

**S: Blaze Plan ne kadar?**
C: Kullandıkça öde. İlk 2 milyon çağrı ücretsiz. Çoğu küçük app için ayda $0-5 arası.

**S: Functions'ı sonradan ekleyebilir miyim?**
C: Evet! İstediğiniz zaman ekleyebilirsiniz.

---

## 🚀 ÖNERİ: Şu an Functions'sız başlayın!

1. ✅ Firestore kurulumunu yapın
2. ✅ Security Rules ekleyin
3. ✅ Navigation entegrasyonunu tamamlayın
4. ✅ Test edin
5. ⏳ Daha sonra Functions ekleyin

**Başlayalım!** 🎉
