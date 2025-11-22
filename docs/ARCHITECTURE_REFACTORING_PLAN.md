# Dozi Mimari Geliştirme Planı

**Tarih:** 2025-11-22
**Versiyon:** 1.0
**Mimari:** MVVM + Jetpack Compose + Firebase + Room

---

## Özet

Bu doküman, Dozi uygulamasının mevcut kod tabanı analizi sonucunda hazırlanmış 12 maddelik teknik geliştirme planını içerir. Her madde, mevcut kod yapısına referanslar, yapılacak değişiklikler ve beklenen teknik çıktıları detaylandırır.

---

## 1. Reminder Engine'in Merkezileştirilmesi

### Mevcut Durum

Hatırlatma mantığı şu dosyalara dağılmış durumda:

| Dosya | Satır | Sorumluluk |
|-------|-------|------------|
| `notifications/ReminderScheduler.kt` | 339 | AlarmManager kurulumu, frekans hesaplaması |
| `notifications/NotificationActionReceiver.kt` | 807 | Action handling, escalation triggers |
| `notifications/NotificationHelper.kt` | 795 | Notification oluşturma, DND bypass |
| `notifications/EscalationManager.kt` | 150+ | Buddy bildirimleri, escalation logic |

**Problem:** Zamanlama hesapları birden fazla yerde tekrarlanıyor. Debug ve test zorlaşıyor.

### Yapılacak Değişiklikler

#### 1.1 Yeni Sınıf Oluşturma

```
core/notifications/ReminderEngine.kt
```

```kotlin
@Singleton
class ReminderEngine @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val notificationHelper: NotificationHelper,
    private val escalationManager: EscalationManager
) {
    // === Alarm Yönetimi ===
    fun scheduleReminder(medicine: Medicine, time: String)
    fun cancelReminder(medicine: Medicine, time: String)
    fun rescheduleAllReminders(medicines: List<Medicine>)

    // === Frekans Hesaplama ===
    fun calculateNextAlarmTime(medicine: Medicine, time: String): Long
    fun shouldShowOnDate(medicine: Medicine, date: LocalDate): Boolean

    // === Escalation ===
    fun scheduleEscalation(medicine: Medicine, time: String, level: Int)
    fun cancelAllEscalations(medicine: Medicine, time: String)

    // === PendingIntent Factory ===
    fun createReminderIntent(medicine: Medicine, time: String): PendingIntent
    fun createActionIntent(action: String, medicine: Medicine, time: String): PendingIntent

    // === DND & Criticality ===
    fun shouldBypassDND(medicine: Medicine): Boolean
    fun getNotificationChannel(medicine: Medicine): String
}
```

#### 1.2 Taşınacak Kodlar

**ReminderScheduler.kt'den:**
- `scheduleReminder()` (satır 47-120) → `ReminderEngine.scheduleReminder()`
- `calculateNextAlarmTime()` (satır 150-280) → `ReminderEngine.calculateNextAlarmTime()`
- `shouldMedicineShowOnDate()` → `ReminderEngine.shouldShowOnDate()`

**NotificationActionReceiver.kt'den:**
- `scheduleEscalations()` (satır 300-400) → `ReminderEngine.scheduleEscalation()`
- `cancelAllEscalations()` → `ReminderEngine.cancelAllEscalations()`

**NotificationHelper.kt'den:**
- `shouldBypassDND()` logic → `ReminderEngine.shouldBypassDND()`
- Channel selection logic → `ReminderEngine.getNotificationChannel()`

#### 1.3 Repository Bağlantısı

```kotlin
// MedicineRepository içinde
class MedicineRepository @Inject constructor(
    private val reminderEngine: ReminderEngine,
    // ...
) {
    suspend fun addMedicine(medicine: Medicine) {
        // Firestore'a ekle
        // ...

        // Engine üzerinden hatırlatma kur
        if (medicine.reminderEnabled) {
            medicine.times.forEach { time ->
                reminderEngine.scheduleReminder(medicine, time)
            }
        }
    }
}
```

### Beklenen Çıktı

- Tüm hatırlatmalar tek merkezden yaratılır ve yönetilir
- Debug için tek breakpoint noktası
- Unit test yazımı kolaylaşır
- Dağınık zamanlama bug'ları ortadan kalkar

---

## 2. Firestore Senkronizasyon Kuyruğuna Monitoring Eklenmesi

### Mevcut Durum

**SyncQueueEntity.kt** (34 satır):
```kotlin
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val dataJson: String,
    val userId: String,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,  // ✅ Zaten var
    val errorMessage: String? = null
)
```

**SyncWorker.kt** (122 satır): Monitoring yok, sadece `Result.success()` veya `Result.retry()` döndürüyor.

### Yapılacak Değişiklikler

#### 2.1 SyncQueueEntity Güncellemesi

Mevcut entity'de `lastAttemptAt` zaten var. Ek olarak:

```kotlin
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    // ... mevcut alanlar ...
    val failureCount: Int = 0,  // Toplam başarısız deneme
    val lastErrorCode: String? = null  // "NETWORK", "AUTH", "FIRESTORE"
)
```

**Migration:**
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE sync_queue ADD COLUMN failureCount INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE sync_queue ADD COLUMN lastErrorCode TEXT")
    }
}
```

#### 2.2 SyncMonitor Sınıfı

```
core/sync/SyncMonitor.kt
```

```kotlin
@Singleton
class SyncMonitor @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    data class SyncMetrics(
        val pendingCount: Int,
        val failedLast24h: Int,
        val averageDelayMs: Long,
        val oldestPendingAge: Long
    )

    suspend fun getMetrics(): SyncMetrics {
        val pending = syncQueueDao.getPendingCount()
        val failed = syncQueueDao.getFailedCountLast24Hours()
        val avgDelay = syncQueueDao.getAverageProcessingDelay()
        val oldest = syncQueueDao.getOldestPendingTimestamp()

        return SyncMetrics(
            pendingCount = pending,
            failedLast24h = failed,
            averageDelayMs = avgDelay,
            oldestPendingAge = System.currentTimeMillis() - oldest
        )
    }

    suspend fun logSyncEvent(
        itemId: Long,
        success: Boolean,
        errorCode: String? = null,
        durationMs: Long
    ) {
        // Local log
        if (!success) {
            syncQueueDao.incrementFailureCount(itemId, errorCode)
        }

        // Firestore analytics (opsiyonel)
        if (shouldLogToFirestore()) {
            logToFirestoreAnalytics(itemId, success, errorCode, durationMs)
        }
    }
}
```

#### 2.3 SyncQueueDao Güncellemesi

```kotlin
@Dao
interface SyncQueueDao {
    // Mevcut metodlar...

    @Query("SELECT COUNT(*) FROM sync_queue WHERE retryCount >= 5 AND lastAttemptAt > :since")
    suspend fun getFailedCountLast24Hours(since: Long = System.currentTimeMillis() - 86400000): Int

    @Query("SELECT AVG(:now - createdAt) FROM sync_queue WHERE lastAttemptAt IS NOT NULL")
    suspend fun getAverageProcessingDelay(now: Long = System.currentTimeMillis()): Long

    @Query("SELECT MIN(createdAt) FROM sync_queue WHERE retryCount < 5")
    suspend fun getOldestPendingTimestamp(): Long

    @Query("UPDATE sync_queue SET failureCount = failureCount + 1, lastErrorCode = :errorCode WHERE id = :id")
    suspend fun incrementFailureCount(id: Long, errorCode: String?)
}
```

#### 2.4 Admin Dashboard Paneli

**AnalyticsDashboardScreen.kt'ye eklenecek:**

```kotlin
@Composable
fun SyncMonitorPanel(syncMonitor: SyncMonitor) {
    var metrics by remember { mutableStateOf<SyncMetrics?>(null) }

    LaunchedEffect(Unit) {
        metrics = syncMonitor.getMetrics()
    }

    Card {
        Column {
            Text("Senkronizasyon Durumu", style = MaterialTheme.typography.titleMedium)

            metrics?.let { m ->
                Row {
                    MetricItem("Bekleyen", m.pendingCount.toString())
                    MetricItem("Başarısız (24s)", m.failedLast24h.toString())
                    MetricItem("Ort. Gecikme", "${m.averageDelayMs / 1000}s")
                }

                if (m.failedLast24h > 10) {
                    Text(
                        "⚠️ Yüksek hata oranı tespit edildi",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
```

### Beklenen Çıktı

- Senkron gecikmeleri tespit edilir
- Veri kaybı durumları erken fark edilir
- Offline-first mimari güçlenir

---

## 3. ViewModel İş Yükünün Ayrıştırılması

### Mevcut Durum

| ViewModel | Satır | Problem |
|-----------|-------|---------|
| HomeViewModel | 848 | Frekans hesaplama, polling, escalation, snooze state |
| BadiViewModel | 378 | Cleanup logic, duplicate detection, search |

**HomeViewModel** örnek problem (satır 282-303):
```kotlin
// Polling kullanılıyor ama aynı zamanda Flow da var
private fun startPollingData() {
    viewModelScope.launch {
        while (true) {
            delay(2000) // Her 2 saniyede polling
            loadTodaysMedicines()
        }
    }
}
```

### Yapılacak Değişiklikler

#### 3.1 UseCase Katmanı Oluşturma

```
core/domain/usecase/
├── medicine/
│   ├── GetTodaysMedicinesUseCase.kt
│   ├── FilterMedicinesByDateUseCase.kt
│   └── CalculateMedicineFrequencyUseCase.kt
├── reminder/
│   ├── ScheduleReminderUseCase.kt
│   └── ProcessSnoozeUseCase.kt
├── badi/
│   ├── CleanupDuplicateBadisUseCase.kt
│   └── SearchBadiUserUseCase.kt
└── stats/
    └── GenerateInsightsUseCase.kt
```

#### 3.2 Örnek UseCase Implementasyonu

```kotlin
// GetTodaysMedicinesUseCase.kt
class GetTodaysMedicinesUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(): Flow<List<Medicine>> {
        return medicineRepository.getMedicinesFlow()
            .map { medicines ->
                val today = LocalDate.now()
                medicines.filter { medicine ->
                    medicine.reminderEnabled &&
                    isInDateRange(medicine, today) &&
                    shouldShowOnDate(medicine, today)
                }
            }
    }

    private fun isInDateRange(medicine: Medicine, date: LocalDate): Boolean {
        // ... mevcut HomeViewModel'deki logic taşınacak
    }

    private fun shouldShowOnDate(medicine: Medicine, date: LocalDate): Boolean {
        // ... ReminderScheduler'daki shouldMedicineShowOnDate() taşınacak
    }
}
```

#### 3.3 Refactored HomeViewModel

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodaysMedicinesUseCase: GetTodaysMedicinesUseCase,
    private val processSnoozeUseCase: ProcessSnoozeUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    // State
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Events
    sealed class HomeEvent {
        data class MedicineTaken(val medicine: Medicine, val time: String) : HomeEvent()
        data class MedicineSnoozed(val medicine: Medicine, val minutes: Int) : HomeEvent()
        object RefreshData : HomeEvent()
    }

    init {
        observeMedicines()
        loadUser()
    }

    private fun observeMedicines() {
        viewModelScope.launch {
            getTodaysMedicinesUseCase()
                .collect { medicines ->
                    _uiState.update { it.copy(
                        todaysMedicines = medicines,
                        upcomingMedicine = findUpcoming(medicines),
                        isLoading = false
                    )}
                }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.MedicineSnoozed -> {
                viewModelScope.launch {
                    processSnoozeUseCase(event.medicine, event.minutes)
                }
            }
            // ...
        }
    }
}
```

**Satır azalması:** 848 → ~200 satır

#### 3.4 Diğer ViewModel'ler İçin Aynı Pattern

**BadiViewModel refactoring:**
- `CleanupDuplicateBadisUseCase` → duplicate detection logic
- `SearchBadiUserUseCase` → user search logic
- `ProcessBadiRequestUseCase` → accept/reject logic

**PremiumIntroViewModel refactoring:**
- `GetUserSegmentUseCase` → kullanıcı tipi belirleme
- `GetPersonalizedOffersUseCase` → dinamik öneriler

### Beklenen Çıktı

- ViewModel'ler 200-400 satıra düşer
- Her ekranın sorumluluğu netleşir
- UseCase'ler bağımsız test edilebilir
- Kod tekrarı azalır

---

## 4. Uygulama Başlangıç Optimizasyonu (Lazy Initialization)

### Mevcut Durum

**DoziApplication.kt** (89 satır) - onCreate() sırası:

1. Places.initialize() ✅ Gerekli
2. Firestore offline persistence ✅ Gerekli
3. MedicineLookupRepository.initialize() ⚠️ Synchronous, blocking
4. createNotificationChannels() ✅ Gerekli
5. SyncWorker.schedulePeriodicSync() ✅ Background

**Eksik ama erken yüklenen:**
- PricingRepository → Screen'lerde lazy load ✅ (Zaten doğru)
- FCM token → Firebase SDK auto ✅ (Zaten doğru)

### Yapılacak Değişiklikler

#### 4.1 MedicineLookupRepository Lazy Loading

Mevcut `initialize()` synchronous ve main thread'de:

```kotlin
// Mevcut (DoziApplication.kt satır ~40)
MedicineLookupRepository.initialize(context)
```

**Değişiklik:**

```kotlin
// DoziApplication.kt
override fun onCreate() {
    super.onCreate()

    // Kritik init'ler
    setupFirestore()
    createNotificationChannels()
    SyncWorker.schedulePeriodicSync(this)

    // Lazy init - 3 saniye sonra
    lifecycleScope.launch {
        delay(3000)
        MedicineLookupRepository.initialize(applicationContext)
    }
}
```

#### 4.2 PricingRepository Pre-cache (Opsiyonel)

Mevcut durumda lazy load doğru çalışıyor. İyileştirme olarak background'da pre-fetch:

```kotlin
// DoziApplication.kt
lifecycleScope.launch {
    delay(5000) // 5 saniye sonra
    try {
        pricingRepository.loadPricing()
    } catch (e: Exception) {
        // DefaultPricing kullanılır, hata loglanır
        Log.w("Dozi", "Pricing pre-fetch failed", e)
    }
}
```

#### 4.3 Splash Screen Optimizasyonu

**SplashActivity** veya **MainActivity** başlangıcı:

```kotlin
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    // Splash'i hemen kaldır
    installSplashScreen()

    super.onCreate(savedInstanceState)

    // Navigation kararı minimum veri ile
    lifecycleScope.launch {
        val isLoggedIn = auth.currentUser != null
        val route = if (isLoggedIn) Screen.Home.route else Screen.Login.route
        navigateTo(route)
    }
}
```

**Yapılmaması gereken:**
- Splash'te Firestore'dan user profile fetch
- Splash'te ilaç listesi yükleme
- Splash'te pricing kontrolü

### Beklenen Çıktı

- Soğuk açılış süresi: ~2-3 saniye → ~1 saniye
- Main thread blocking azalır
- Kullanıcı uygulamaya hızlı girer

---

## 5. UI Component Library

### Mevcut Durum

**`core/ui/components/`** dizini:
- DoziBottomBar.kt (500 satır)
- MedicineCard.kt (420 satır)
- PremiumComponents.kt (700 satır)
- EmptyState.kt (100 satır)
- DoziCharacter.kt (150 satır)
- DoziTopBar.kt (80 satır)
- PinDialog.kt (250 satır)

**Eksik:** Genel amaçlı Button, Card, Input, ListItem bileşenleri yok. Her ekran kendi Material bileşenlerini kullanıyor.

### Yapılacak Değişiklikler

#### 5.1 Yeni Component Dosyaları

```
core/ui/components/
├── base/
│   ├── DoziButton.kt
│   ├── DoziCard.kt
│   ├── DoziInput.kt
│   ├── DoziListItem.kt
│   └── DoziHeader.kt
├── theme/
│   └── DoziTheme.kt
└── ... (mevcut dosyalar)
```

#### 5.2 DoziButton Implementasyonu

```kotlin
// DoziButton.kt
@Composable
fun DoziButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DoziButtonVariant = DoziButtonVariant.Primary,
    size: DoziButtonSize = DoziButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val colors = when (variant) {
        DoziButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = DoziColors.Primary,
            contentColor = Color.White
        )
        DoziButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = DoziColors.Secondary,
            contentColor = DoziColors.OnSecondary
        )
        DoziButtonVariant.Outline -> ButtonDefaults.outlinedButtonColors()
        DoziButtonVariant.Text -> ButtonDefaults.textButtonColors()
    }

    val height = when (size) {
        DoziButtonSize.Small -> 36.dp
        DoziButtonSize.Medium -> 48.dp
        DoziButtonSize.Large -> 56.dp
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled && !loading,
        colors = colors,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            icon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = DoziTypography.button)
        }
    }
}

enum class DoziButtonVariant { Primary, Secondary, Outline, Text }
enum class DoziButtonSize { Small, Medium, Large }
```

#### 5.3 DoziCard Implementasyonu

```kotlin
// DoziCard.kt
@Composable
fun DoziCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = DoziColors.Surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
```

#### 5.4 DoziTheme Consolidation

```kotlin
// DoziTheme.kt
object DoziColors {
    val Primary = Color(0xFF26C6DA)      // Cyan
    val PrimaryDark = Color(0xFF0095A8)
    val Secondary = Color(0xFF7C4DFF)    // Purple
    val Surface = Color(0xFFFAFAFA)
    val Error = Color(0xFFD32F2F)
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFA000)

    // Medicine criticality
    val Routine = Color(0xFF26C6DA)
    val Important = Color(0xFFFFA000)
    val Critical = Color(0xFFD32F2F)
}

object DoziTypography {
    val h1 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    )
    val h2 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    )
    val body1 = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
    val button = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 1.25.sp
    )
    val caption = TextStyle(
        fontSize = 12.sp,
        color = Color.Gray
    )
}

object DoziSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
```

#### 5.5 Mevcut Ekranlarda Değişim

**Önce:**
```kotlin
// HomeScreen.kt
Button(
    onClick = { /* ... */ },
    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26C6DA))
) {
    Text("İlaç Ekle")
}
```

**Sonra:**
```kotlin
// HomeScreen.kt
DoziButton(
    text = "İlaç Ekle",
    onClick = { /* ... */ },
    icon = Icons.Default.Add
)
```

### Beklenen Çıktı

- Tüm ekranlar tutarlı marka görünümüne sahip
- Yeni ekran oluşturmak hızlanır
- Design system değişiklikleri tek noktadan yapılır

---

## 6. Yeni Onboarding Akışı (Wizard)

### Mevcut Durum

**NavGraph.kt** navigasyon akışı:
- LoginScreen → ProfileScreen → HomeScreen

**Eksik:** İlk kullanıcı deneyimi yok. Kullanıcı direkt HomeScreen'e düşüyor.

### Yapılacak Değişiklikler

#### 6.1 Yeni Ekranlar

```
core/ui/screens/onboarding/
├── WelcomeScreen.kt
├── FirstMedicineWizardScreen.kt
└── PremiumBenefitsScreen.kt
```

#### 6.2 Navigation Güncellemesi

```kotlin
// Screen.kt
sealed class Screen(val route: String) {
    // Mevcut ekranlar...

    // Yeni onboarding ekranları
    object Welcome : Screen("welcome")
    object FirstMedicineWizard : Screen("first_medicine_wizard")
    object PremiumBenefits : Screen("premium_benefits")
}

// NavGraph.kt
composable(Screen.Welcome.route) {
    WelcomeScreen(
        onContinue = { navController.navigate(Screen.FirstMedicineWizard.route) }
    )
}

composable(Screen.FirstMedicineWizard.route) {
    FirstMedicineWizardScreen(
        onComplete = { navController.navigate(Screen.PremiumBenefits.route) },
        onSkip = { navController.navigate(Screen.Home.route) }
    )
}

composable(Screen.PremiumBenefits.route) {
    PremiumBenefitsScreen(
        onContinue = {
            markOnboardingComplete()
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Welcome.route) { inclusive = true }
            }
        }
    )
}
```

#### 6.3 Onboarding State Yönetimi

```kotlin
// OnboardingManager.kt
@Singleton
class OnboardingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("completed", false)
    }

    fun markCompleted() {
        prefs.edit().putBoolean("completed", true).apply()
    }

    fun shouldShowOnboarding(user: User?): Boolean {
        return user != null && !isOnboardingCompleted()
    }
}
```

#### 6.4 WelcomeScreen Örneği

```kotlin
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DoziSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DoziCharacter(
            animation = DoziAnimation.Wave,
            size = 200.dp
        )

        Spacer(modifier = Modifier.height(DoziSpacing.xl))

        Text(
            "Dozi'ye Hoş Geldiniz!",
            style = DoziTypography.h1
        )

        Text(
            "İlaçlarınızı asla unutmayın",
            style = DoziTypography.body1,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(DoziSpacing.xl))

        DoziButton(
            text = "Başlayalım",
            onClick = onContinue,
            size = DoziButtonSize.Large,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

### Beklenen Çıktı

- Kullanıcı uygulamaya daha hızlı bağlanır
- İlk ilaç ekleme oranı artar
- Premium tanıtımı erken yapılır

---

## 7. İstatistik Ekranlarının Yorumlayıcı Hale Getirilmesi

### Mevcut Durum

**StatsViewModel.kt** (99 satır): Sadece ham veri gösteriyor, yorum yok.

### Yapılacak Değişiklikler

#### 7.1 InsightGenerator Sınıfı

```kotlin
// core/domain/usecase/stats/GenerateInsightsUseCase.kt
class GenerateInsightsUseCase @Inject constructor(
    private val medicationLogRepository: MedicationLogRepository
) {
    data class Insight(
        val type: InsightType,
        val title: String,
        val description: String,
        val severity: InsightSeverity,
        val recommendation: String?
    )

    enum class InsightType {
        TREND_IMPROVING,
        TREND_DECLINING,
        TIME_PATTERN,
        STREAK_MILESTONE,
        RISK_ALERT
    }

    enum class InsightSeverity { INFO, WARNING, CRITICAL }

    suspend operator fun invoke(userId: String): List<Insight> {
        val logs = medicationLogRepository.getLogsLast30Days(userId)
        val insights = mutableListOf<Insight>()

        // Haftalık trend analizi
        val weeklyCompliance = calculateWeeklyCompliance(logs)
        if (weeklyCompliance.last() < weeklyCompliance.first() - 10) {
            insights.add(Insight(
                type = InsightType.TREND_DECLINING,
                title = "Uyumluluk Düşüşü",
                description = "Son hafta uyumluluğunuz %${weeklyCompliance.first() - weeklyCompliance.last()} azaldı",
                severity = InsightSeverity.WARNING,
                recommendation = "Hatırlatma saatlerinizi gözden geçirin"
            ))
        }

        // Sabah/akşam pattern analizi
        val morningRate = calculateTimeSlotCompliance(logs, 6..12)
        val eveningRate = calculateTimeSlotCompliance(logs, 18..22)
        if (morningRate < eveningRate - 20) {
            insights.add(Insight(
                type = InsightType.TIME_PATTERN,
                title = "Sabah Dozları Risk Altında",
                description = "Sabah ilaçlarınızı akşama göre %${eveningRate - morningRate} daha az alıyorsunuz",
                severity = InsightSeverity.WARNING,
                recommendation = "Sabah rutininize ilaç almayı ekleyin"
            ))
        }

        // En çok kaçırılan saat
        val missedByHour = groupMissedByHour(logs)
        val peakMissedHour = missedByHour.maxByOrNull { it.value }
        peakMissedHour?.let {
            if (it.value > 5) {
                insights.add(Insight(
                    type = InsightType.TIME_PATTERN,
                    title = "Kritik Saat: ${it.key}:00",
                    description = "Bu saatte ${it.value} kez ilaç kaçırdınız",
                    severity = InsightSeverity.INFO,
                    recommendation = "Bu saat için ek hatırlatma ekleyin"
                ))
            }
        }

        return insights
    }
}
```

#### 7.2 DoziInsightCard Bileşeni

```kotlin
// core/ui/components/DoziInsightCard.kt
@Composable
fun DoziInsightCard(insight: Insight) {
    val backgroundColor = when (insight.severity) {
        InsightSeverity.INFO -> DoziColors.Surface
        InsightSeverity.WARNING -> Color(0xFFFFF3E0)
        InsightSeverity.CRITICAL -> Color(0xFFFFEBEE)
    }

    val iconTint = when (insight.severity) {
        InsightSeverity.INFO -> DoziColors.Primary
        InsightSeverity.WARNING -> DoziColors.Warning
        InsightSeverity.CRITICAL -> DoziColors.Error
    }

    DoziCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = when (insight.type) {
                    InsightType.TREND_DECLINING -> Icons.Default.TrendingDown
                    InsightType.TREND_IMPROVING -> Icons.Default.TrendingUp
                    InsightType.TIME_PATTERN -> Icons.Default.Schedule
                    else -> Icons.Default.Lightbulb
                },
                contentDescription = null,
                tint = iconTint
            )

            Spacer(Modifier.width(DoziSpacing.md))

            Column {
                Text(insight.title, style = DoziTypography.h2)
                Text(insight.description, style = DoziTypography.body1)

                insight.recommendation?.let {
                    Spacer(Modifier.height(DoziSpacing.sm))
                    Text(
                        "💡 $it",
                        style = DoziTypography.caption,
                        color = DoziColors.Primary
                    )
                }
            }
        }
    }
}
```

### Beklenen Çıktı

- Kullanıcı sadece grafik değil "anlam" görür
- Actionable öneriler alır
- Engagement artar

---

## 8. Badi Sistemi İçin Anlatıcı Katman

### Mevcut Durum

Badi (buddy) sistemi güçlü ama kullanıcılar ne işe yaradığını anlamıyor.

**Mevcut ekranlar:**
- AddBadiScreen.kt
- BadiDetailScreen.kt
- BadiListScreen.kt
- BadiPermissionsScreen.kt

### Yapılacak Değişiklikler

#### 8.1 Bilgi Modalları

```kotlin
// BadiInfoBottomSheet.kt
@Composable
fun BadiInfoBottomSheet(
    onDismiss: () -> Unit
) {
    BottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(DoziSpacing.lg)) {
            Text("Badi Sistemi Nedir?", style = DoziTypography.h1)

            Spacer(Modifier.height(DoziSpacing.md))

            FeatureExplanation(
                icon = Icons.Default.People,
                title = "İlaç Takip Ortağı",
                description = "Güvendiğiniz kişileri Badi olarak ekleyin. İlacınızı kaçırdığınızda onlara bildirim gider."
            )

            FeatureExplanation(
                icon = Icons.Default.Notifications,
                title = "Kritik İlaç Uyarısı",
                description = "Kritik ilaçlar için 3 kademe yükseltme sistemi. 60 dakika içinde alınmazsa Badi'leriniz bilgilendirilir."
            )

            FeatureExplanation(
                icon = Icons.Default.Lock,
                title = "Gizlilik Kontrolü",
                description = "Hangi ilaçlarınızı paylaşacağınızı siz seçin. Badi'ler sadece izin verdiğiniz ilaçları görür."
            )

            Spacer(Modifier.height(DoziSpacing.lg))

            DoziButton(
                text = "Anladım",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

#### 8.2 İlk Kullanım Rehberi

**FirstTimeBadiGuide.kt:**

```kotlin
@Composable
fun FirstTimeBadiGuide(onComplete: () -> Unit) {
    val pagerState = rememberPagerState()

    HorizontalPager(
        count = 3,
        state = pagerState
    ) { page ->
        when (page) {
            0 -> GuidePage(
                animation = "badi_connect.json", // Lottie
                title = "Badi Ekleyin",
                description = "Aile üyesi veya arkadaşınızı Badi olarak davet edin"
            )
            1 -> GuidePage(
                animation = "badi_notify.json",
                title = "Otomatik Bildirim",
                description = "İlacınızı kaçırdığınızda Badi'leriniz haberdar olur"
            )
            2 -> GuidePage(
                animation = "badi_safe.json",
                title = "Her Zaman Güvende",
                description = "Kritik ilaçlarınız için ekstra koruma"
            )
        }
    }

    // Page indicators ve Continue butonu
}
```

#### 8.3 Lottie Animasyon Desteği

**build.gradle (app):**
```gradle
dependencies {
    implementation "com.airbnb.android:lottie-compose:6.0.0"
}
```

**Kullanım:**
```kotlin
@Composable
fun LottieAnimation(
    animationRes: String,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset(animationRes)
    )

    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier
    )
}
```

### Beklenen Çıktı

- Kullanıcı Badi sistemini hızlı anlar
- Badi adoption oranı artar
- Support ticket sayısı azalır

---

## 9. Premium Ekranının Kişiselleştirilmesi

### Mevcut Durum

**PremiumIntroScreen.kt:** Tüm kullanıcılara aynı mesajlar gösteriliyor.

### Yapılacak Değişiklikler

#### 9.1 User Segment Detection

```kotlin
// GetUserSegmentUseCase.kt
class GetUserSegmentUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val medicationLogRepository: MedicationLogRepository,
    private val badiRepository: BadiRepository
) {
    enum class UserSegment {
        HIGH_FREQUENCY,    // 5+ ilaç veya 10+ günlük hatırlatma
        FAMILY_USER,       // 2+ aktif badi
        CHRONIC_USER,      // Uzun süreli ilaç (90+ gün)
        VITAMIN_USER,      // Sadece takviye/vitamin
        NEW_USER           // <7 gün kullanım
    }

    suspend operator fun invoke(userId: String): UserSegment {
        val medicines = medicineRepository.getMedicines(userId)
        val badis = badiRepository.getActiveBadis(userId)
        val daysSinceFirstLog = medicationLogRepository.getDaysSinceFirstLog(userId)

        return when {
            daysSinceFirstLog < 7 -> UserSegment.NEW_USER
            badis.size >= 2 -> UserSegment.FAMILY_USER
            medicines.any { it.endDate == null ||
                (it.endDate - it.startDate) > 90 * 24 * 60 * 60 * 1000 } -> UserSegment.CHRONIC_USER
            medicines.sumOf { it.times.size } >= 10 -> UserSegment.HIGH_FREQUENCY
            medicines.all { it.name.contains("vitamin", ignoreCase = true) ||
                it.name.contains("takviye", ignoreCase = true) } -> UserSegment.VITAMIN_USER
            else -> UserSegment.NEW_USER
        }
    }
}
```

#### 9.2 Personalized Messaging

```kotlin
// PremiumIntroViewModel.kt
@HiltViewModel
class PremiumIntroViewModel @Inject constructor(
    private val getUserSegmentUseCase: GetUserSegmentUseCase,
    private val medicineRepository: MedicineRepository,
    private val medicationLogRepository: MedicationLogRepository
) : ViewModel() {

    data class PersonalizedOffer(
        val headline: String,
        val subheadline: String,
        val highlightedFeatures: List<String>,
        val ctaText: String
    )

    suspend fun getPersonalizedOffer(userId: String): PersonalizedOffer {
        val segment = getUserSegmentUseCase(userId)
        val medicineCount = medicineRepository.getMedicineCount(userId)
        val missedCount = medicationLogRepository.getMissedCount7Days(userId)

        return when (segment) {
            UserSegment.HIGH_FREQUENCY -> PersonalizedOffer(
                headline = "$medicineCount ilacınız için Premium koruma",
                subheadline = "Sınırsız hatırlatma ve gelişmiş takvim",
                highlightedFeatures = listOf(
                    "Sınırsız ilaç ve hatırlatma",
                    "Gelişmiş takvim görünümü",
                    "Çoklu zaman dilimi desteği"
                ),
                ctaText = "Tüm ilaçlarımı yönet"
            )

            UserSegment.FAMILY_USER -> PersonalizedOffer(
                headline = "Aileniz için Premium",
                subheadline = "Tüm aileyi tek hesaptan yönetin",
                highlightedFeatures = listOf(
                    "Aile planı (5 kişiye kadar)",
                    "Gelişmiş Badi özellikleri",
                    "Paylaşılan ilaç takibi"
                ),
                ctaText = "Aile planını başlat"
            )

            UserSegment.CHRONIC_USER -> PersonalizedOffer(
                headline = "Uzun vadeli tedaviniz için",
                subheadline = "Aylık raporlar ve trend analizi",
                highlightedFeatures = listOf(
                    "Haftalık sağlık raporu",
                    "Doktor paylaşım özelliği",
                    "Detaylı uyumluluk analizi"
                ),
                ctaText = "Tedavimi optimize et"
            )

            else -> PersonalizedOffer(
                headline = if (missedCount > 0)
                    "Son 7 günde $missedCount doz kaçırdınız"
                    else "İlaçlarınızı hiç kaçırmayın",
                subheadline = "Premium ile uyumluluğunuzu artırın",
                highlightedFeatures = listOf(
                    "Gelişmiş bildirimler",
                    "Stok takibi",
                    "İstatistikler"
                ),
                ctaText = "Premium'a geç"
            )
        }
    }
}
```

#### 9.3 Screen Güncellemesi

```kotlin
// PremiumIntroScreen.kt
@Composable
fun PremiumIntroScreen(viewModel: PremiumIntroViewModel = hiltViewModel()) {
    val offer by viewModel.personalizedOffer.collectAsState()

    Column {
        Text(offer.headline, style = DoziTypography.h1)
        Text(offer.subheadline, style = DoziTypography.body1)

        offer.highlightedFeatures.forEach { feature ->
            FeatureRow(feature)
        }

        DoziButton(
            text = offer.ctaText,
            onClick = { /* Purchase flow */ }
        )
    }
}
```

### Beklenen Çıktı

- Premium dönüşüm oranı artar
- Kullanıcı "bana özel" hisseder
- ARPU yükselir

---

## 10. Paylaşılabilir Başarı Kartları

### Yapılacak Değişiklikler

#### 10.1 ShareCardGenerator

```kotlin
// utils/ShareCardGenerator.kt
class ShareCardGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun generateAchievementCard(
        achievement: Achievement,
        userStats: UserStats
    ): Bitmap {
        return withContext(Dispatchers.Default) {
            val width = 1080
            val height = 1080

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background gradient
            val gradient = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    android.graphics.Color.parseColor("#26C6DA"),
                    android.graphics.Color.parseColor("#7C4DFF")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            val paint = Paint().apply { shader = gradient }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Achievement icon
            drawAchievementIcon(canvas, achievement, width / 2f, 300f)

            // Title
            drawText(canvas, achievement.title, width / 2f, 500f, 64f, true)

            // Stats
            drawText(canvas, "${userStats.currentStreak} gün seri", width / 2f, 600f, 48f)
            drawText(canvas, "%${userStats.complianceRate} uyumluluk", width / 2f, 680f, 48f)

            // Dozi branding
            drawText(canvas, "Dozi ile takip ediyorum", width / 2f, 950f, 32f)

            bitmap
        }
    }

    fun shareCard(bitmap: Bitmap, context: Context) {
        val uri = saveBitmapToCache(bitmap, context)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Dozi ile ilaçlarımı takip ediyorum! #Dozi")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Paylaş"))
    }

    private fun saveBitmapToCache(bitmap: Bitmap, context: Context): Uri {
        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs()

        val file = File(cachePath, "dozi_achievement.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
```

#### 10.2 Compose-to-Bitmap Alternatifi

```kotlin
@Composable
fun ShareableAchievementCard(
    achievement: Achievement,
    stats: UserStats,
    onShare: (Bitmap) -> Unit
) {
    val view = LocalView.current

    Box(
        modifier = Modifier
            .size(1080.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(DoziColors.Primary, DoziColors.Secondary)
                )
            )
            .drawWithContent {
                drawContent()
                // Capture to bitmap when needed
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Achievement content
        }
    }
}
```

### Beklenen Çıktı

- Ücretsiz viral büyüme
- Kullanıcı bağlılığı artar
- Sosyal medya görünürlüğü

---

## 11. Segment Bazlı Premium Teklifleri

Bu madde **Madde 9** ile birleştirildi. `GetUserSegmentUseCase` ve `PersonalizedOffer` sistemi her iki ihtiyacı karşılıyor.

---

## 12. Haftalık Sağlık Özeti

### Yapılacak Değişiklikler

#### 12.1 WeeklyReportGenerator

```kotlin
// utils/reporting/WeeklyReportGenerator.kt
class WeeklyReportGenerator @Inject constructor(
    private val medicationLogRepository: MedicationLogRepository,
    private val medicineRepository: MedicineRepository,
    @ApplicationContext private val context: Context
) {
    data class WeeklyReport(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val overallCompliance: Int,
        val totalDoses: Int,
        val takenDoses: Int,
        val missedDoses: Int,
        val bestDay: String,
        val worstDay: String,
        val mostMissedTime: String?,
        val recommendations: List<String>
    )

    suspend fun generateReport(userId: String): WeeklyReport {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)

        val logs = medicationLogRepository.getLogsBetween(userId, startDate, endDate)
        val medicines = medicineRepository.getMedicines(userId)

        // Calculate metrics
        val totalDoses = calculateExpectedDoses(medicines, startDate, endDate)
        val takenDoses = logs.count { it.status == LogStatus.TAKEN }
        val missedDoses = logs.count { it.status == LogStatus.MISSED }

        val complianceByDay = groupByDay(logs)
        val bestDay = complianceByDay.maxByOrNull { it.value }?.key ?: "N/A"
        val worstDay = complianceByDay.minByOrNull { it.value }?.key ?: "N/A"

        val missedByHour = logs
            .filter { it.status == LogStatus.MISSED }
            .groupingBy { it.time.substringBefore(":").toInt() }
            .eachCount()
        val mostMissedTime = missedByHour.maxByOrNull { it.value }?.let { "${it.key}:00" }

        val recommendations = generateRecommendations(
            overallCompliance = (takenDoses * 100) / totalDoses,
            mostMissedTime = mostMissedTime,
            worstDay = worstDay
        )

        return WeeklyReport(
            startDate = startDate,
            endDate = endDate,
            overallCompliance = (takenDoses * 100) / totalDoses,
            totalDoses = totalDoses,
            takenDoses = takenDoses,
            missedDoses = missedDoses,
            bestDay = bestDay,
            worstDay = worstDay,
            mostMissedTime = mostMissedTime,
            recommendations = recommendations
        )
    }

    suspend fun generatePDF(report: WeeklyReport): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Draw report content
        drawHeader(canvas, report)
        drawComplianceChart(canvas, report)
        drawMetrics(canvas, report)
        drawRecommendations(canvas, report)

        document.finishPage(page)

        val file = File(context.cacheDir, "weekly_report_${report.endDate}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }
}
```

#### 12.2 Haftalık Bildirim

```kotlin
// notifications/WeeklyReportNotificationWorker.kt
@HiltWorker
class WeeklyReportNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reportGenerator: WeeklyReportGenerator,
    private val notificationHelper: NotificationHelper,
    private val auth: FirebaseAuth
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.failure()

        val report = reportGenerator.generateReport(userId)

        notificationHelper.showWeeklyReportNotification(
            compliance = report.overallCompliance,
            takenCount = report.takenDoses,
            missedCount = report.missedDoses
        )

        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            // Her Pazartesi saat 09:00
            val request = PeriodicWorkRequestBuilder<WeeklyReportNotificationWorker>(
                7, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(calculateDelayToNextMonday(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "weekly_report",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
```

### Beklenen Çıktı

- Kullanıcı haftalık ilerlemeyi görür
- Retention artar
- Premium değer algısı güçlenir

---

## Öncelik ve Bağımlılıklar

### Faz 1: Temel Mimari (Hafta 1-2)
1. **Madde 1** - ReminderEngine (diğer tüm bildirim işleri buna bağlı)
2. **Madde 3** - UseCase katmanı (tüm yeni özellikler buna bağlı)
3. **Madde 5** - UI Component Library (tüm yeni ekranlar buna bağlı)

### Faz 2: İyileştirmeler (Hafta 3-4)
4. **Madde 4** - Startup optimizasyonu
5. **Madde 2** - Sync monitoring

### Faz 3: Kullanıcı Deneyimi (Hafta 5-6)
6. **Madde 6** - Onboarding
7. **Madde 8** - Badi anlatıcı katman
8. **Madde 7** - İstatistik insights

### Faz 4: Gelir Optimizasyonu (Hafta 7-8)
9. **Madde 9** - Premium personalization
10. **Madde 12** - Haftalık rapor
11. **Madde 10** - Paylaşılabilir kartlar

---

## Test Stratejisi

### Unit Tests
- `ReminderEngineTest` - Tüm zamanlama senaryoları
- `GetTodaysMedicinesUseCaseTest` - Frekans filtreleme
- `GetUserSegmentUseCaseTest` - Segment belirleme
- `GenerateInsightsUseCaseTest` - Insight üretimi

### Integration Tests
- `SyncWorkerTest` - Offline-first senkronizasyon
- `WeeklyReportGeneratorTest` - Rapor üretimi

### UI Tests
- Onboarding flow
- Premium conversion flow
- Badi guide flow

---

## Dosya Yapısı Özeti

```
app/src/main/java/com/bardino/dozi/
├── core/
│   ├── notifications/
│   │   ├── ReminderEngine.kt          [YENİ]
│   │   ├── ReminderScheduler.kt       [REFACTOR]
│   │   ├── NotificationHelper.kt
│   │   └── ...
│   ├── sync/
│   │   ├── SyncMonitor.kt             [YENİ]
│   │   ├── SyncWorker.kt              [GÜNCELLE]
│   │   └── SyncManager.kt
│   ├── domain/
│   │   └── usecase/                   [YENİ DİZİN]
│   │       ├── medicine/
│   │       ├── reminder/
│   │       ├── badi/
│   │       └── stats/
│   ├── data/
│   │   ├── local/
│   │   │   └── dao/
│   │   │       └── SyncQueueDao.kt    [GÜNCELLE]
│   │   └── repository/
│   └── ui/
│       ├── components/
│       │   ├── base/                  [YENİ DİZİN]
│       │   │   ├── DoziButton.kt
│       │   │   ├── DoziCard.kt
│       │   │   └── ...
│       │   └── theme/
│       │       └── DoziTheme.kt       [YENİ]
│       ├── screens/
│       │   ├── onboarding/            [YENİ DİZİN]
│       │   │   ├── WelcomeScreen.kt
│       │   │   └── ...
│       │   └── ...
│       └── viewmodel/
├── utils/
│   ├── ShareCardGenerator.kt          [YENİ]
│   └── reporting/
│       └── WeeklyReportGenerator.kt   [YENİ]
└── ...
```

---

## Sonuç

Bu plan, Dozi uygulamasının mevcut güçlü yönlerini koruyarak mimari kaliteyi artırmayı hedefliyor. Her madde bağımsız olarak implement edilebilir, ancak önerilen sıralama en az sürtünme ile ilerlemeyi sağlar.

Kritik başarı faktörleri:
1. ReminderEngine merkezileştirmesi tüm bildirim sisteminin temelidir
2. UseCase katmanı olmadan yeni özellikler ViewModel'leri şişirir
3. UI Component Library olmadan yeni ekranlar tutarsız olur

Her fazın sonunda code review ve test coverage kontrolü yapılmalıdır.
