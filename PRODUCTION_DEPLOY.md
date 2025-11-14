# 🚀 Production Deployment - Tam Kurulum

## ✅ Blaze Plan Aktif - Profesyonel Kurulum

Tüm Cloud Functions ve otomatik bildirim sistemi ile kusursuz bir production deployment.

---

## 📦 1. Firebase Functions Kurulumu

### Adım 1: Dependencies Kurulumu

```bash
# Proje kök dizininde
cd C:\Users\Ufuk\AndroidStudioProjects\Dozi\firebase-functions

# Node modules kur
npm install

# Başarılı oldu mu kontrol et
npm list firebase-functions firebase-admin
```

**Beklenen çıktı:**
```
dozi-functions@1.0.0
├── firebase-admin@12.0.0
└── firebase-functions@5.0.0
```

### Adım 2: TypeScript Compile

```bash
# TypeScript'i JavaScript'e çevir
npm run build

# lib/ klasörü oluştu mu kontrol et
dir lib
```

**Oluşması gereken:**
```
lib/
  └── index.js
  └── index.js.map
```

### Adım 3: Firebase Login

```bash
# Firebase'e login ol
firebase login

# Başarılı olursa tarayıcıda Google hesabınızı seçin
# "Firebase CLI Login Successful" görmelisiniz
```

### Adım 4: Firebase Projesini Bağla

```bash
# Proje kök dizinine dön
cd ..

# Firebase init (zaten firebase.json var, skip edebilirsiniz)
# Veya direkt deploy edebilirsiniz
```

---

## 🔥 2. Deploy İşlemi

### Functions Deploy

```bash
# Sadece Functions deploy et
firebase deploy --only functions

# Veya tüm Firebase özelliklerini deploy et
firebase deploy
```

**Beklenen çıktı:**
```
✔  Deploy complete!

Functions:
  - onBuddyRequestCreated(us-central1)
  - onMedicationTaken(us-central1)
  - sendMedicationReminderToBuddies(us-central1)
  - checkMissedMedications(us-central1)
```

### Firestore Rules & Indexes Deploy

```bash
# Rules ve Indexes'leri deploy et
firebase deploy --only firestore

# Sadece rules
firebase deploy --only firestore:rules

# Sadece indexes
firebase deploy --only firestore:indexes
```

---

## 🧪 3. Test Etme

### Test 1: Functions Çalışıyor mu?

Firebase Console'da kontrol edin:
1. https://console.firebase.google.com
2. Projeniz: `dozi-cd7cc`
3. **Functions** sekmesi
4. 4 function görmelisiniz:
   - ✅ onBuddyRequestCreated
   - ✅ onMedicationTaken
   - ✅ sendMedicationReminderToBuddies
   - ✅ checkMissedMedications

### Test 2: Buddy Request Bildirimi

Android app'ten:
1. Bir kullanıcı buddy isteği gönderin
2. Firebase Console → Functions → Logs
3. "Yeni buddy isteği" log'unu görmelisiniz
4. Alıcıya push notification gitmeli

### Test 3: İlaç Alındı Bildirimi

Android app'ten:
1. Bir ilaç alın (MedicationLog oluşturun)
2. Functions logs'da "İlaç alındı" görmelisiniz
3. Buddy'lere push notification gitmeli

### Test 4: Callable Function

Android app'ten:
```kotlin
// NotificationRepository.sendMedicationReminderToBuddies kullanın
viewModel.sendMedicationReminderToBuddies(
    medicineId = "123",
    medicineName = "Aspirin",
    dosage = "1 tablet",
    time = "14:00"
)
```

Başarılı olursa buddy'lere bildirim gider.

---

## 📊 4. Monitoring & Logs

### Real-time Logs

```bash
# Terminal'de real-time logs izle
firebase functions:log --only onBuddyRequestCreated

# Tüm functions logs
firebase functions:log

# Son 50 log
firebase functions:log --limit 50
```

### Firebase Console'da Logs

1. Firebase Console → Functions
2. Her function'a tıklayın
3. **Logs** sekmesi
4. Real-time log stream göreceksiniz

### Metrics

Firebase Console → Functions → Metrics:
- Invocation count (kaç kez çağrıldı)
- Execution time (ne kadar sürdü)
- Error rate (hata oranı)
- Memory usage (bellek kullanımı)

---

## 🎯 5. Android App Entegrasyonu

### FCM Token Kaydetme

Zaten çalışıyor! `DoziMessagingService.kt`:
```kotlin
override fun onNewToken(token: String) {
    userRepository.updateUserField("fcmToken", token)
}
```

### Buddy İsteği Gönderme

```kotlin
// BuddyViewModel kullanarak
viewModel.sendBuddyRequest(toUserId, message)

// Cloud Function otomatik tetiklenir
// Alıcıya bildirim gider
```

### İlaç Alındığında

```kotlin
// MedicationLog oluştur
val log = MedicationLog(
    medicineId = medicineId,
    medicineName = medicineName,
    dosage = dosage,
    status = MedicationStatus.TAKEN,
    takenAt = Timestamp.now()
)

medicationLogRepository.createMedicationLog(log)

// Cloud Function otomatik tetiklenir
// Buddy'lere bildirim gider
```

### Manuel Hatırlatma Gönderme

```kotlin
// İlaç hatırlatma zamanı geldiğinde
notificationViewModel.sendMedicationReminderToBuddies(
    medicineId = medicineId,
    medicineName = medicineName,
    dosage = dosage,
    time = time
)
```

---

## 🔒 6. Security & Best Practices

### Firestore Rules Kontrolü

```bash
# Rules deploy edildi mi?
firebase deploy --only firestore:rules

# Test et
firebase emulators:start --only firestore
```

### Environment Variables (Opsiyonel)

Eğer API key'ler kullanıyorsanız:

```bash
# Config set
firebase functions:config:set someservice.key="THE API KEY"

# Config get
firebase functions:config:get

# Deploy after config
firebase deploy --only functions
```

### Rate Limiting (Opsiyonel)

Çok fazla bildirim gönderilmesini engellemek için:

```typescript
// index.ts'e ekleyin
import { defineInt } from "firebase-functions/params";

const maxNotificationsPerHour = defineInt("MAX_NOTIFICATIONS_PER_HOUR", 100);

// Function içinde kontrol
if (sentCount > maxNotificationsPerHour.value()) {
  console.warn("Rate limit aşıldı");
  return;
}
```

---

## 💰 7. Maliyet Optimizasyonu

### Functions Pricing

Blaze Plan - Pay as you go:
- **İlk 2M çağrı/ay:** Ücretsiz
- **Sonrası:** $0.40 per 1M invocations
- **Compute time:** GB-second başına ücret

### Optimizasyon İpuçları

1. **Gereksiz function çağrılarını azaltın:**
   ```typescript
   // Önce kontrol et
   if (!buddy.notificationPreferences?.onMedicationTaken) {
     return; // Erken çık, bildirim gönderme
   }
   ```

2. **Batch işlemler:**
   ```typescript
   // Tek tek yerine toplu gönder
   await Promise.all(promises);
   ```

3. **Cache kullanın:**
   ```typescript
   // Sık kullanılan verileri cache'le
   const cachedUser = await cache.get(userId);
   ```

4. **Log'ları azaltın (production'da):**
   ```typescript
   if (process.env.NODE_ENV === 'production') {
     // Sadece hataları logla
     console.error(error);
   }
   ```

### Maliyet Tahmini

Ortalama kullanım (1000 aktif kullanıcı):
- Buddy request: ~50/gün = 1,500/ay
- Medication taken: ~1000/gün = 30,000/ay
- Missed checks: 15dk/96 kez/gün = 2,880/ay
- **Toplam:** ~35K invocation/ay = **ÜCRETSİZ** (2M limit altında)

---

## 📱 8. Production Checklist

### Firebase
- [x] Firestore Database oluşturuldu
- [x] Security rules deploy edildi
- [x] Indexes oluşturuldu
- [x] Cloud Functions deploy edildi
- [x] Blaze plan aktif

### Android App
- [ ] Navigation route'ları eklendi
- [ ] FCM token kayıt sistemi çalışıyor
- [ ] Hilt dependency injection çalışıyor
- [ ] Bildirim izinleri isteniyor
- [ ] Test edildi

### Testing
- [ ] Buddy request bildirimi test edildi
- [ ] Medication taken bildirimi test edildi
- [ ] Callable function test edildi
- [ ] Push notification alındı
- [ ] Firestore'a veri kaydediliyor

---

## 🆘 Sorun Giderme

### "npm: command not found"
```bash
# Node.js kur
# https://nodejs.org/en/download/
node --version  # v18+ olmalı
```

### "firebase: command not found"
```bash
npm install -g firebase-tools
firebase --version
```

### "TypeScript compilation failed"
```bash
cd firebase-functions
npm install typescript --save-dev
npm run build
```

### "Function deployment failed"
```bash
# Logs kontrol et
firebase functions:log

# Yeniden deploy
firebase deploy --only functions --force
```

### "Push notification gelmiyor"
```bash
# FCM token kontrol et
# Firestore → users → [userId] → fcmToken var mı?

# Function logs kontrol et
firebase functions:log --only onBuddyRequestCreated

# Android app logs
adb logcat | grep FCM
```

### "Firestore permission denied"
```bash
# Security rules deploy et
firebase deploy --only firestore:rules

# Rules test et
firebase emulators:start --only firestore
```

---

## 🎉 Başarı!

Tüm adımlar tamamlandıysa:
- ✅ Cloud Functions çalışıyor
- ✅ Otomatik bildirimler gidiyor
- ✅ Firestore real-time sync çalışıyor
- ✅ Production-ready sistem hazır

---

## 📚 Sonraki Adımlar

1. **Analytics ekleyin:**
   - Firebase Analytics entegre edin
   - Kullanıcı davranışlarını izleyin

2. **Performance monitoring:**
   - Firebase Performance
   - Crash reporting (Crashlytics)

3. **A/B Testing:**
   - Firebase Remote Config
   - Özellik flag'leri

4. **Backup:**
   - Firestore export
   - Otomatik backup schedule

---

**Şimdi deploy edin ve production'a geçin!** 🚀
