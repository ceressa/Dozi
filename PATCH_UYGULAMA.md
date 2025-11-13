# Patch Dosyasını Uygulama Talimatları

## Adım 1: Patch dosyasını kopyala

`homescreen-fixes.patch` dosyası proje klasöründe hazır.

## Adım 2: Local bilgisayarında uygula

Proje klasöründe (`C:\Users\Ufuk\AndroidStudioProjects\Dozi`) şu komutu çalıştır:

```bash
git apply homescreen-fixes.patch
```

**VEYA** eğer conflict olursa:

```bash
git apply --reject homescreen-fixes.patch
```

Bu durumda `.rej` dosyaları oluşur, onları manuel olarak düzeltmen gerekir.

## Adım 3: Değişiklikleri kontrol et

```bash
git status
git diff
```

## Adım 4: Commit yap

```bash
git add app/src/main/java/com/bardino/dozi/core/ui/screens/home/HomeScreen.kt
git commit -m "Fix: HomeScreen UI improvements and data accuracy

Changes:
- Fixed MultiMedicineCard design with proper AL/ATLA buttons
- Fixed status persistence: medicines now disappear after being taken/skipped
- Fixed calendar to show real data instead of dummy data
- Added calculateDayStatus() and isMedicineValidForDate() functions
- Calendar colors now change based on actual medicine status"
```

## Adım 5: Push yap

```bash
git push origin claude/firebase-setup-011CV5kEGGpx75j8qXFq4eTG
```

## Alternatif: Manuel Uygulama

Eğer patch uygulanamıyorsa, `HOMESCREEN_CHANGES.md` dosyasındaki değişiklikleri manuel olarak yapabilirsin.

## Ne Değişti?

1. ✅ MultiMedicineCard - AL/ATLA butonları düzgün
2. ✅ Status Persistence - İlaçlar alındıktan/atlandıktan sonra kayboluyorlar
3. ✅ Calendar - Gerçek veri gösteriyor, dummy data yok
4. ✅ Calendar renkleri - Duruma göre değişiyor (yeşil/kırmızı/mavi/gri)
