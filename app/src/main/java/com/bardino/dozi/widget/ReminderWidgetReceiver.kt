package com.bardino.dozi.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dozi Hatırlatma Widget Receiver
 * Widget lifecycle olaylarını yönetir ve güncellemeleri tetikler
 */
class ReminderWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ReminderWidget()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // Widget güncellendiğinde verileri yenile
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            coroutineScope.launch {
                ReminderWidgetUpdater.updateWidgets(context)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Özel intent'leri işle
        when (intent.action) {
            ACTION_REFRESH_WIDGET -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    coroutineScope.launch {
                        ReminderWidgetUpdater.updateWidgets(context)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.bardino.dozi.REFRESH_WIDGET"

        /**
         * Widget'ı dışarıdan güncellemek için helper fonksiyon
         */
        fun refreshWidget(context: Context) {
            val intent = Intent(context, ReminderWidgetReceiver::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}
