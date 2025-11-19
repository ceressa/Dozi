package com.bardino.dozi.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.state.GlanceStateDefinition
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

/**
 * Widget'ta gösterilecek tek bir hatırlatma
 */
data class WidgetReminder(
    val medicineId: String,
    val medicineName: String,
    val dosage: String,
    val unit: String,
    val time: String,
    val icon: String,
    val colorHex: String
)

/**
 * Widget state için preference keys
 */
object ReminderWidgetKeys {
    val CURRENT_INDEX = intPreferencesKey("current_index")
    val TOTAL_COUNT = intPreferencesKey("total_count")
    val REMINDERS_JSON = stringPreferencesKey("reminders_json")
}

/**
 * Widget için Glance state definition
 */
object ReminderWidgetStateDefinition : GlanceStateDefinition<Preferences> {
    private const val DATA_STORE_FILENAME = "reminder_widget_prefs"

    private val Context.dataStore by preferencesDataStore(name = DATA_STORE_FILENAME)

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Preferences> {
        return context.dataStore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, "datastore/$DATA_STORE_FILENAME.preferences_pb")
    }
}
