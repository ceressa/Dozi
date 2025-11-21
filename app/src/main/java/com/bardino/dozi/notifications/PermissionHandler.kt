package com.bardino.dozi.notifications

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHandler {

    const val REQUEST_CODE_NOTIFICATION = 1001
    const val REQUEST_CODE_OVERLAY = 1002
    const val REQUEST_CODE_EXACT_ALARM = 1003

    // Çin menşeli telefon üreticileri
    private val CHINESE_MANUFACTURERS = listOf(
        "xiaomi", "redmi", "poco",
        "huawei", "honor",
        "oppo", "realme", "oneplus",
        "vivo", "iqoo",
        "meizu", "zte", "nubia",
        "lenovo", "motorola"
    )

    /**
     * Çin menşeli telefon mu kontrol eder
     */
    fun isChineseManufacturer(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return CHINESE_MANUFACTURERS.any { manufacturer.contains(it) }
    }

    /**
     * Üretici adını döndürür
     */
    fun getManufacturerDisplayName(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> "Xiaomi"
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> "Huawei"
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> "OPPO/Realme"
            manufacturer.contains("oneplus") -> "OnePlus"
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> "Vivo"
            manufacturer.contains("meizu") -> "Meizu"
            manufacturer.contains("samsung") -> "Samsung"
            else -> Build.MANUFACTURER
        }
    }

    /**
     * Pil optimizasyonu ayarlarını açar (üreticiye özel)
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()

        val intent = when {
            // Xiaomi / MIUI
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }
            }
            // Huawei / EMUI
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                }
            }
            // OPPO / ColorOS
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                }
            }
            // OnePlus / OxygenOS
            manufacturer.contains("oneplus") -> {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                    )
                }
            }
            // Vivo / FuntouchOS
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                }
            }
            // Samsung
            manufacturer.contains("samsung") -> {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.battery.ui.BatteryActivity"
                    )
                }
            }
            // Fallback - Genel pil optimizasyonu ayarları
            else -> {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Üreticiye özel ayar bulunamazsa genel ayarlara git
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                // Son çare: Uygulama ayarlarını aç
                openAppSettings(context)
            }
        }
    }

    /**
     * Çin telefonu için talimat metni
     */
    fun getBatteryOptimizationInstructions(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()

        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ->
                "Ayarlar > Uygulamalar > Dozi > Pil tasarrufu > 'Kısıtlama yok' seçin.\n\nAyrıca: Güvenlik > Otomatik başlatma > Dozi'yi etkinleştirin."

            manufacturer.contains("huawei") || manufacturer.contains("honor") ->
                "Ayarlar > Pil > Uygulama başlatma > Dozi > 'Manuel olarak yönet' seçin ve tüm seçenekleri açın."

            manufacturer.contains("oppo") || manufacturer.contains("realme") ->
                "Ayarlar > Pil > Daha fazla pil ayarı > Uygulama hızlı dondurma > Dozi'yi kapatın."

            manufacturer.contains("oneplus") ->
                "Ayarlar > Pil > Pil optimizasyonu > Dozi > 'Optimize etme' seçin."

            manufacturer.contains("vivo") || manufacturer.contains("iqoo") ->
                "Ayarlar > Pil > Arka plan güç tüketimi > Dozi > 'Kısıtlama' seçin."

            manufacturer.contains("samsung") ->
                "Ayarlar > Pil > Arka plan kullanım sınırları > Hiçbir zaman uyku moduna alma > Dozi'yi ekleyin."

            else ->
                "Ayarlar > Pil > Pil optimizasyonu > Dozi > 'Optimize etme' seçin."
        }
    }

    /**
     * Tüm gerekli izinleri kontrol eder
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasNotificationPermission(context) &&
                hasOverlayPermission(context) &&
                hasExactAlarmPermission(context)
    }

    /**
     * Bildirim iznini kontrol eder (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 altında otomatik verilir
        }
    }

    /**
     * Overlay izni (Dialog göstermek için)
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Exact Alarm izni (Android 12+)
     */
    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        } else {
            true
        }
    }

    /**
     * Bildirim izni ister (Android 13+)
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATION
            )
        }
    }

    /**
     * Overlay izni ister
     */
    fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY)
        }
    }

    /**
     * Exact Alarm izni ister (Android 12+)
     */
    fun requestExactAlarmPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.data = Uri.parse("package:${activity.packageName}")
            activity.startActivityForResult(intent, REQUEST_CODE_EXACT_ALARM)
        }
    }

    /**
     * Tüm izinleri ister
     */
    fun requestAllPermissions(activity: Activity) {
        // 1. Bildirim izni
        if (!hasNotificationPermission(activity)) {
            requestNotificationPermission(activity)
        }

        // 2. Overlay izni
        if (!hasOverlayPermission(activity)) {
            requestOverlayPermission(activity)
        }

        // 3. Exact Alarm izni
        if (!hasExactAlarmPermission(activity)) {
            requestExactAlarmPermission(activity)
        }
    }

    /**
     * İzin sonuçlarını işle
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        when (requestCode) {
            REQUEST_CODE_NOTIFICATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
        }
    }

    /**
     * Ayarlara gitmek için Intent
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}