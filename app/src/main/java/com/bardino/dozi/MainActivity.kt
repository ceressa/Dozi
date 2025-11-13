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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.bardino.dozi.core.data.IlacRepository
import com.bardino.dozi.core.data.OnboardingPreferences
import com.bardino.dozi.navigation.NavGraph
import com.bardino.dozi.notifications.NotificationHelper
import com.bardino.dozi.core.ui.theme.DoziAppTheme
import com.bardino.dozi.navigation.Screen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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

                                // ✅ Firestore kullanıcı kaydı
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val userRef = db.collection("users").document(user.uid)

                                userRef.get().addOnSuccessListener { snapshot ->
                                    if (!snapshot.exists()) {
                                        val userData = mapOf(
                                            "uid" to user.uid,
                                            "name" to (user.displayName ?: "Bilinmeyen Kullanıcı"),
                                            "email" to (user.email ?: ""),
                                            "photoUrl" to (user.photoUrl?.toString() ?: ""),
                                            "createdAt" to System.currentTimeMillis()
                                        )

                                        userRef.set(userData)
                                            .addOnSuccessListener {
                                                Log.d(
                                                    "GOOGLE_AUTH",
                                                    "Firestore kullanıcı oluşturuldu: ${user.email}"
                                                )
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e(
                                                    "GOOGLE_AUTH",
                                                    "Firestore kaydı başarısız: ${e.localizedMessage}"
                                                )
                                            }
                                    } else {
                                        Log.d(
                                            "GOOGLE_AUTH",
                                            "Kullanıcı zaten kayıtlı: ${user.email}"
                                        )
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

        setContent {
            DoziAppTheme {
                val navController = rememberNavController()

                NavGraph(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    onGoogleSignInClick = { signInWithGoogle() } // 🔹 artık burada tanımlı
                )
            }
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
}