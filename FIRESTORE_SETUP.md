# Firestore Index Kurulumu

Bu dosya, Firestore index hatalarını önlemek için gerekli adımları içerir.

## Sorun

Firestore'da composite (birleşik) sorgular yaparken index gerekir. Index yoksa şu hatayı alırsınız:
```
FAILED_PRECONDITION: The query requires an index.
```

## Kalıcı Çözüm

### 1. Firebase CLI Kurulumu (Tek Seferlik)

```bash
# Firebase CLI'yi global olarak yükle
npm install -g firebase-tools

# Firebase'e giriş yap
firebase login
```

### 2. Index'leri Deploy Et

```bash
# Proje dizinine git
cd /home/user/Dozi

# Index'leri Firebase'e deploy et
firebase deploy --only firestore:indexes
```

**ÖNEMLİ:** Index'lerin build olması **birkaç dakika** sürebilir. Firebase Console'dan ilerlemeyi takip edebilirsiniz:
https://console.firebase.google.com/project/dozi-cd7cc/firestore/indexes

### 3. Alternatif: Manuel Index Oluşturma

Eğer deployment çalışmazsa, hata mesajındaki URL'yi kullanarak manuel oluşturabilirsiniz:

1. Hatayı al
2. URL'yi kopyala (örn: `https://console.firebase.google.com/v1/r/project/...`)
3. Tarayıcıda aç
4. "Create Index" butonuna tıkla

## Index Listesi

Projede şu index'ler tanımlı:

### buddy_requests
1. `toUserId + status + createdAt (DESC)` - Gelen istekleri listele
2. `fromUserId + toUserId + status` - Duplicate istek kontrolü

### buddies
1. `userId + status` - Kullanıcının badilerini listele
2. `userId + buddyUserId + status` - Badi ilişkisi kontrolü

### medication_logs
1. `userId + scheduledTime (DESC)` - Kullanıcı ilaç geçmişi
2. `userId + medicineId + scheduledTime (DESC)` - Belirli ilaç geçmişi
3. `status + scheduledTime (DESC)` - Duruma göre sıralama

### notifications
1. `userId + createdAt (DESC)` - Kullanıcı bildirimleri
2. `userId + isRead + createdAt (DESC)` - Okunmamış bildirimler

## Development İpuçları

### Firestore Emulator Kullanımı (Önerilen)

Firestore Emulator kullanırsanız index'lere gerek kalmaz:

```bash
# firebase.json dosyasına ekle:
{
  "emulators": {
    "firestore": {
      "port": 8080
    }
  }
}

# Emulator'ı başlat
firebase emulators:start
```

Android kodunda emulator'a bağlan:
```kotlin
// Development build için
if (BuildConfig.DEBUG) {
    FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
}
```

### Yeni Sorgu Eklediğinizde

1. Hatayı alın
2. URL'den index tanımını kopyalayın
3. `firestore.indexes.json` dosyasına ekleyin
4. `firebase deploy --only firestore:indexes` ile deploy edin

## Troubleshooting

### "command not found: firebase"
```bash
npx firebase-tools deploy --only firestore:indexes
```

### "No currently active project"
`.firebaserc` dosyası oluştur:
```json
{
  "projects": {
    "default": "dozi-cd7cc"
  }
}
```

### Index build ediliyor hatası
Index'ler build olana kadar bekleyin (2-5 dakika). Firebase Console'dan kontrol edin.

### Google Play Services hatası
Bu hata emulator'da normaldir. Gerçek cihazda çalıştırın veya:
1. Emulator'da Play Store olan bir image kullanın
2. Google Play Services'i güncelleyin

## Production Checklist

- [ ] Tüm index'ler deploy edildi
- [ ] Firebase Console'da tüm index'ler "Enabled" durumda
- [ ] firestore.rules production için güncellendi
- [ ] Emulator kullanımı production build'de devre dışı
