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