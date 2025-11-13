package com.bardino.dozi.geofence

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.bardino.dozi.notifications.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val geofence = event.triggeringGeofences?.firstOrNull()
            val placeName = geofence?.requestId ?: "Tanımsız Konum"
            NotificationHelper.showMedicationNotification(
                context,
                "💊 ${placeName} konumuna vardın! İlacını almayı unutma."
            )
        }
    }
}
