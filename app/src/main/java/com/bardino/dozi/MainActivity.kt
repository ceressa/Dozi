package com.bardino.dozi

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.bardino.dozi.core.data.IlacRepository
import com.bardino.dozi.core.data.OnboardingPreferences
import com.bardino.dozi.core.data.ThemePreferences
import com.bardino.dozi.core.data.repository.UserRepository
import com.bardino.dozi.core.data.repository.PremiumRepository
import com.bardino.dozi.navigation.NavGraph
import com.bardino.dozi.notifications.NotificationHelper
import com.bardino.dozi.core.ui.theme.DoziAppTheme
import com.bardino.dozi.navigation.Screen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val userRepository = UserRepository()
    private var currentIntent by mutableStateOf<Intent?>(null)
    private var navController: androidx.navigation.NavHostController? = null

    @Inject
    lateinit var profileRepository: com.bardino.dozi.core.data.repository.ProfileRepository

    // 🔹 Çoklu izin isteyici (bildirim, kamera, konum)
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionResults(permissions)
        }

    // 🔹 Overlay izni için ayrı launcher
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Sessiz kontrol
        }

    // 🔹 Exact Alarm izni için launcher
    private val exactAlarmLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Sessiz kontrol
        }

    // 🔹 Google Sign-In sonucu yakalayıcı
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnSuccessListener { authResult ->
                            val user = authResult.user
                            if (user != null) {
                                Log.d("GOOGLE_AUTH", "Firebase login başarılı: ${user.email}")
                                Toast.makeText(
                                    this,
                                    "Giriş başarılı: ${user.displayName}",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // ✅ UserRepository ile Firestore kullanıcı kaydı
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        userRepository.createUserIfNotExists()
                                        Log.d("GOOGLE_AUTH", "Kullanıcı Firestore'a kaydedildi/güncellendi")

                                        // 📱 Device ID'yi kaydet
                                        val deviceId = Settings.Secure.getString(
                                            contentResolver,
                                            Settings.Secure.ANDROID_ID
                                        )
                                        userRepository.updateUserField("deviceId", deviceId)
                                        Log.d("GOOGLE_AUTH", "Device ID kaydedildi: $deviceId")

                                        // 🎁 Onboarding tamamlandıysa 1 haftalık ücretsiz trial ver
                                        if (!OnboardingPreferences.isFirstTime(this@MainActivity)) {
                                            userRepository.activateTrialIfOnboarding()
                                            Log.d("PREMIUM_TRIAL", "1 haftalık trial aktivasyonu yapıldı")
                                        }

                                        // 👥 Firestore'dan profilleri senkronize et
                                        profileRepository.syncProfilesFromFirestore()
                                        Log.d("GOOGLE_AUTH", "Profiller Firestore'dan senkronize edildi")

                                        // ✅ FCM token'ı al ve kaydet (retry logic ile)
                                        saveFCMToken()
                                    } catch (e: Exception) {
                                        Log.e("GOOGLE_AUTH", "Firestore kaydı başarısız: ${e.localizedMessage}")
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("GOOGLE_AUTH", "Firebase login hatası: ${e.localizedMessage}")
                            Toast.makeText(this, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG)
                                .show()
                        }

                } catch (e: Exception) {
                    Log.e("GOOGLE_AUTH", "Google oturumu alınamadı: ${e.message}")
                    Toast.makeText(this, "Google oturumu alınamadı: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            } else {
                Log.w("GOOGLE_AUTH", "Google Sign-In iptal edildi veya başarısız.")
            }
        }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIntent = intent

        // ✅ Uygulama başlangıcında kullanıcı login olmuşsa profilleri sync et ve FCM token'ı kaydet
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // 📱 Device ID'yi kaydet (her açılışta kontrol et ve kaydet)
                val deviceId = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                userRepository.updateUserField("deviceId", deviceId)
                Log.d("MainActivity", "Device ID kaydedildi/güncellendi: $deviceId")

                // 👥 Firestore'dan profilleri senkronize et
                profileRepository.syncProfilesFromFirestore()
                Log.d("MainActivity", "Profiller Firestore'dan senkronize edildi")

                saveFCMToken()
            }
        }

        setContent {
            // 🎨 Tema tercihini oku (DataStore'dan Flow olarak)
            val themeMode by ThemePreferences.getThemeFlow(this).collectAsState(initial = "system")
            val systemInDarkTheme = isSystemInDarkTheme()

            // Tema tercihine göre dark mode aktif mi belirle
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemInDarkTheme // "system" veya tanımsız ise sistem ayarını kullan
            }

            DoziAppTheme(darkTheme = isDarkTheme) {
                navController = rememberNavController()

                // İlk açılışta deep link varsa handle et
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    handleDeepLink(intent, navController!!)
                }

                // Başlangıç ekranını belirle - DeviceId ve Firestore kontrolü
                var startDestination by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf<String?>(null)
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val currentUser = FirebaseAuth.getInstance().currentUser

                    startDestination = if (currentUser != null) {
                        // ✅ Kullanıcı zaten login, Firestore'dan onboarding durumunu kontrol et
                        try {
                            val userData = userRepository.getUserData()
                            if (userData?.onboardingCompleted == true) {
                                Screen.Home.route
                            } else {
                                Screen.OnboardingWelcome.route
                            }
                        } catch (e: Exception) {
                            if (OnboardingPreferences.isFirstTime(this@MainActivity)) {
                                Screen.OnboardingWelcome.route
                            } else {
                                Screen.Home.route
                            }
                        }
                    } else {
                        // 📱 Kullanıcı login değil - DeviceId ile tanıma sistemi
                        val deviceId = Settings.Secure.getString(
                            contentResolver,
                            Settings.Secure.ANDROID_ID
                        )

                        // Firestore'da bu deviceId'ye sahip kullanıcı var mı?
                        val userWithDevice = userRepository.getUserByDeviceId(deviceId)

                        if (userWithDevice != null && userWithDevice.onboardingCompleted) {
                            // ✅ DeviceId tanındı ve onboarding tamamlanmış
                            // Kullanıcıyı direkt login ekranına götür
                            Log.d("MainActivity", "📱 DeviceId tanındı: ${userWithDevice.email}, direkt login ekranına yönlendiriliyor")
                            Screen.Login.route
                        } else if (userWithDevice != null && !userWithDevice.onboardingCompleted) {
                            // DeviceId tanındı ama onboarding tamamlanmamış
                            Log.d("MainActivity", "📱 DeviceId tanındı ama onboarding tamamlanmamış, onboarding'e yönlendiriliyor")
                            Screen.OnboardingWelcome.route
                        } else {
                            // DeviceId tanınmadı - İlk kez kullanıyor
                            Log.d("MainActivity", "📱 DeviceId tanınmadı, onboarding'e yönlendiriliyor")
                            Screen.OnboardingWelcome.route
                        }
                    }
                }

                // startDestination hazır olana kadar loading göster
                if (startDestination != null) {
                    NavGraph(
                        navController = navController!!,
                        startDestination = startDestination!!,
                        onGoogleSignInClick = { signInWithGoogle() }
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("MainActivity", "onNewIntent called")

        // Bildirimden tıklanınca direkt navigation yap
        navController?.let { nav ->
            handleDeepLink(intent, nav)
        } ?: Log.w("MainActivity", "NavController is null in onNewIntent")
    }

    private fun handleDeepLink(intent: Intent?, navController: androidx.navigation.NavHostController) {
        Log.d("MainActivity", "handleDeepLink called with intent: $intent")
        Log.d("MainActivity", "Intent extras: ${intent?.extras?.keySet()?.joinToString()}")

        val navigationRoute = intent?.getStringExtra("navigation_route")
        Log.d("MainActivity", "navigationRoute from intent: $navigationRoute")

        if (!navigationRoute.isNullOrEmpty()) {
            Log.d("MainActivity", "Deep link detected: $navigationRoute")
            try {
                navController.navigate(navigationRoute) {
                    // Ana sayfayı stack'te tut
                    popUpTo(Screen.Home.route) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Navigation failed: ${e.message}")
            }
        } else {
            Log.d("MainActivity", "No navigation_route in intent extras")
        }
    }


    // 🔹 Google oturum başlatma fonksiyonu
    private fun signInWithGoogle() {
        Log.d("GOOGLE_AUTH", "Google sign-in başlatılıyor (Activity içinden)")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }



    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // 🔔 Bildirim izni (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 📷 Kamera izni
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // 📍 Konum izinleri
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            permissionsToRequest.addAll(
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        // 🔹 Standart izinleri iste
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Tüm standart izinler verilmiş, kanal oluştur
            NotificationHelper.createDoziChannel(this)

            // Şimdi özel izinleri kontrol et
            checkSpecialPermissions()
        }
    }

    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: true

        // 🔔 Bildirim izni
        if (notifGranted) {
            NotificationHelper.createDoziChannel(this)
            Toast.makeText(this, "✅ Bildirim izni verildi", Toast.LENGTH_SHORT).show()

            // Standart izinler tamam, şimdi özel izinleri kontrol et
            checkSpecialPermissions()
        } else {
            Toast.makeText(
                this,
                "💧 Dozi: Bildirim izni olmadan seni zamanında uyaramam.",
                Toast.LENGTH_LONG
            ).show()
        }

        // 📷 Kamera izni
        if (!cameraGranted) {
            Toast.makeText(
                this,
                "📷 Kamera izni verilmedi (İlaç barkod okuma çalışmayacak)",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 📍 Konum izni
        if (!locationGranted) {
            Toast.makeText(
                this,
                "📍 Konum izni verilmedi (Eczane bulma çalışmayacak)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkSpecialPermissions() {
        // 🎯 Overlay iznini sadece ilk sefer iste
        val prefs = getSharedPreferences("dozi_prefs", MODE_PRIVATE)
        val overlayAsked = prefs.getBoolean("overlay_asked", false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this) && !overlayAsked) {
                prefs.edit().putBoolean("overlay_asked", true).apply()
                // Dialog göster, direkt isteme
                showOverlayPermissionDialog()
            }
        }

        // ⏰ Exact Alarm iznini sadece ilk sefer iste
        val alarmAsked = prefs.getBoolean("alarm_asked", false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms() && !alarmAsked) {
                prefs.edit().putBoolean("alarm_asked", true).apply()
                // Dialog göster, direkt isteme
                showExactAlarmPermissionDialog()
            }
        }
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("💧 Dozi - Ek İzin")
            .setMessage("İlaç erteleme dialog'unu gösterebilmem için 'Diğer uygulamaların üzerinde gösterim' iznine ihtiyacım var.\n\nBu izin olmadan erteleme özelliği çalışmayacak.")
            .setPositiveButton("İzin Ver") { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton("Şimdi Değil", null)
            .show()
    }

    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("💧 Dozi - Alarm İzni")
            .setMessage("İlaç hatırlatmalarını tam zamanında verebilmem için alarm izni gerekiyor.\n\nBu izin olmadan hatırlatmalar gecikebilir.")
            .setPositiveButton("İzin Ver") { _, _ ->
                requestExactAlarmPermission()
            }
            .setNegativeButton("Şimdi Değil", null)
            .show()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            exactAlarmLauncher.launch(intent)
        }
    }

    /**
     * FCM token'ı al ve Firestore'a kaydet (retry logic ile)
     */
    private suspend fun saveFCMToken() {
        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
                Log.d("FCM_TOKEN", "FCM token alınıyor... (Deneme: ${retryCount + 1})")

                val fcmToken = FirebaseMessaging.getInstance().token.await()

                if (fcmToken.isNullOrEmpty()) {
                    Log.w("FCM_TOKEN", "FCM token boş geldi, tekrar deneniyor...")
                    retryCount++
                    kotlinx.coroutines.delay(2000) // 2 saniye bekle
                    continue
                }

                // Token başarıyla alındı, Firestore'a kaydet
                userRepository.updateUserField("fcmToken", fcmToken)
                Log.d("FCM_TOKEN", "✅ FCM token başarıyla kaydedildi: ${fcmToken.take(20)}...")
                return // Başarılı, fonksiyondan çık

            } catch (e: Exception) {
                Log.e("FCM_TOKEN", "FCM token alma hatası (Deneme ${retryCount + 1}): ${e.message}")
                retryCount++

                if (retryCount < maxRetries) {
                    kotlinx.coroutines.delay(2000) // 2 saniye bekle ve tekrar dene
                } else {
                    Log.e("FCM_TOKEN", "❌ FCM token kaydı başarısız (${maxRetries} deneme sonrası)")
                }
            }
        }
    }
}