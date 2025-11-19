package com.bardino.dozi.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.bardino.dozi.MainActivity
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dozi Hatırlatma Widget'ı
 * Sıradaki ilaç hatırlatmalarını gösterir, slide ile gezinme ve aksiyon butonları
 */
class ReminderWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = ReminderWidgetStateDefinition

    override val sizeMode = SizeMode.Exact

    @Composable
    override fun Content() {
        val prefs = currentState<Preferences>()
        val currentIndex = prefs[ReminderWidgetKeys.CURRENT_INDEX] ?: 0
        val totalCount = prefs[ReminderWidgetKeys.TOTAL_COUNT] ?: 0
        val remindersJson = prefs[ReminderWidgetKeys.REMINDERS_JSON] ?: "[]"

        val reminders = try {
            val type = object : TypeToken<List<WidgetReminder>>() {}.type
            Gson().fromJson<List<WidgetReminder>>(remindersJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val currentReminder = reminders.getOrNull(currentIndex)

        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .background(ColorProvider(day = WidgetColors.BackgroundLight, night = WidgetColors.BackgroundDark))
                    .cornerRadius(16.dp)
            ) {
                if (currentReminder != null) {
                    ReminderContent(
                        reminder = currentReminder,
                        currentIndex = currentIndex,
                        totalCount = totalCount,
                        hasPrevious = currentIndex > 0,
                        hasNext = currentIndex < totalCount - 1
                    )
                } else {
                    EmptyContent()
                }
            }
        }
    }
}

@Composable
private fun ReminderContent(
    reminder: WidgetReminder,
    currentIndex: Int,
    totalCount: Int,
    hasPrevious: Boolean,
    hasNext: Boolean
) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Üst kısım - Sayaç
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${currentIndex + 1}/$totalCount",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.TextSecondary),
                    fontSize = 12.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Orta kısım - İlaç bilgisi ve navigasyon
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sol ok - Önceki
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .cornerRadius(18.dp)
                    .background(
                        if (hasPrevious) ColorProvider(WidgetColors.Primary)
                        else ColorProvider(WidgetColors.Disabled)
                    )
                    .clickable(
                        onClick = if (hasPrevious) actionRunCallback<PreviousReminderAction>()
                        else actionRunCallback<NoOpAction>()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "<",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.White),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // İlaç bilgisi
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // İkon ve isim
                Text(
                    text = reminder.icon,
                    style = TextStyle(fontSize = 28.sp)
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    text = reminder.medicineName,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.TextPrimary),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                // Saat
                Text(
                    text = reminder.time,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.Primary),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                // Doz
                Text(
                    text = "${reminder.dosage} ${reminder.unit}",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.TextSecondary),
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Sağ ok - Sonraki
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .cornerRadius(18.dp)
                    .background(
                        if (hasNext) ColorProvider(WidgetColors.Primary)
                        else ColorProvider(WidgetColors.Disabled)
                    )
                    .clickable(
                        onClick = if (hasNext) actionRunCallback<NextReminderAction>()
                        else actionRunCallback<NoOpAction>()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ">",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.White),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Alt kısım - Aksiyon butonları
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Aldım butonu
            Box(
                modifier = GlanceModifier
                    .width(80.dp)
                    .height(32.dp)
                    .cornerRadius(16.dp)
                    .background(ColorProvider(WidgetColors.Success))
                    .clickable(
                        onClick = actionRunCallback<TakeMedicineAction>(
                            parameters = actionParametersOf(
                                ActionParamKeys.MEDICINE_ID to reminder.medicineId,
                                ActionParamKeys.MEDICINE_NAME to reminder.medicineName,
                                ActionParamKeys.DOSAGE to reminder.dosage,
                                ActionParamKeys.TIME to reminder.time
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aldım",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            // Atla butonu
            Box(
                modifier = GlanceModifier
                    .width(80.dp)
                    .height(32.dp)
                    .cornerRadius(16.dp)
                    .background(ColorProvider(WidgetColors.Secondary))
                    .clickable(
                        onClick = actionRunCallback<SkipMedicineAction>(
                            parameters = actionParametersOf(
                                ActionParamKeys.MEDICINE_ID to reminder.medicineId,
                                ActionParamKeys.MEDICINE_NAME to reminder.medicineName,
                                ActionParamKeys.DOSAGE to reminder.dosage,
                                ActionParamKeys.TIME to reminder.time
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Atla",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable(onClick = actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💊",
            style = TextStyle(fontSize = 32.sp)
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        Text(
            text = "Bugün ilaç yok",
            style = TextStyle(
                color = ColorProvider(WidgetColors.TextSecondary),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = "Dokunarak aç",
            style = TextStyle(
                color = ColorProvider(WidgetColors.Primary),
                fontSize = 12.sp
            )
        )
    }
}

/**
 * Widget renkleri - Dozi temasına uyumlu
 */
object WidgetColors {
    val Primary = android.graphics.Color.parseColor("#A78BFA")      // Lavender
    val Secondary = android.graphics.Color.parseColor("#FDA4AF")    // Coral
    val Success = android.graphics.Color.parseColor("#6EE7B7")      // Mint green
    val Disabled = android.graphics.Color.parseColor("#E0E0E0")     // Gray 300
    val White = android.graphics.Color.WHITE
    val TextPrimary = android.graphics.Color.parseColor("#212121")  // Gray 900
    val TextSecondary = android.graphics.Color.parseColor("#757575") // Gray 600
    val BackgroundLight = android.graphics.Color.parseColor("#F3E8FF") // Purple 100
    val BackgroundDark = android.graphics.Color.parseColor("#1E1E1E")
}

/**
 * Action parameter keys
 */
object ActionParamKeys {
    val MEDICINE_ID = ActionParameters.Key<String>("medicine_id")
    val MEDICINE_NAME = ActionParameters.Key<String>("medicine_name")
    val DOSAGE = ActionParameters.Key<String>("dosage")
    val TIME = ActionParameters.Key<String>("time")
}

/**
 * No-op action for disabled buttons
 */
class NoOpAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Do nothing
    }
}

/**
 * Önceki hatırlatmaya geç
 */
class PreviousReminderAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, ReminderWidgetStateDefinition, glanceId) { prefs ->
            val currentIndex = prefs[ReminderWidgetKeys.CURRENT_INDEX] ?: 0
            prefs.toMutablePreferences().apply {
                this[ReminderWidgetKeys.CURRENT_INDEX] = (currentIndex - 1).coerceAtLeast(0)
            }
        }
        ReminderWidget().update(context, glanceId)
    }
}

/**
 * Sonraki hatırlatmaya geç
 */
class NextReminderAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, ReminderWidgetStateDefinition, glanceId) { prefs ->
            val currentIndex = prefs[ReminderWidgetKeys.CURRENT_INDEX] ?: 0
            val totalCount = prefs[ReminderWidgetKeys.TOTAL_COUNT] ?: 0
            prefs.toMutablePreferences().apply {
                this[ReminderWidgetKeys.CURRENT_INDEX] = (currentIndex + 1).coerceAtMost(totalCount - 1)
            }
        }
        ReminderWidget().update(context, glanceId)
    }
}

/**
 * İlacı aldım aksiyonu
 */
class TakeMedicineAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val medicineId = parameters[ActionParamKeys.MEDICINE_ID] ?: return
        val medicineName = parameters[ActionParamKeys.MEDICINE_NAME] ?: return
        val dosage = parameters[ActionParamKeys.DOSAGE] ?: return
        val time = parameters[ActionParamKeys.TIME] ?: return

        // SharedPreferences'a kaydet (offline support)
        saveMedicineStatus(context, medicineId, time, "taken")

        // Widget'ı güncelle
        ReminderWidgetUpdater.updateWidgets(context)
    }

    private fun saveMedicineStatus(context: Context, medicineId: String, time: String, status: String) {
        val prefs = context.getSharedPreferences("medicine_status", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
        val key = "dose_${medicineId}_${today}_${time}"
        prefs.edit().putString(key, status).apply()
    }
}

/**
 * İlacı atla aksiyonu
 */
class SkipMedicineAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val medicineId = parameters[ActionParamKeys.MEDICINE_ID] ?: return
        val medicineName = parameters[ActionParamKeys.MEDICINE_NAME] ?: return
        val dosage = parameters[ActionParamKeys.DOSAGE] ?: return
        val time = parameters[ActionParamKeys.TIME] ?: return

        // SharedPreferences'a kaydet
        saveMedicineStatus(context, medicineId, time, "skipped")

        // Widget'ı güncelle
        ReminderWidgetUpdater.updateWidgets(context)
    }

    private fun saveMedicineStatus(context: Context, medicineId: String, time: String, status: String) {
        val prefs = context.getSharedPreferences("medicine_status", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
        val key = "dose_${medicineId}_${today}_${time}"
        prefs.edit().putString(key, status).apply()
    }
}

/**
 * Widget güncelleyici - Repository'den veri çeker ve widget state'ini günceller
 */
object ReminderWidgetUpdater {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateWidgets(context: Context) {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(ReminderWidget::class.java)

        if (glanceIds.isEmpty()) return

        // Repository'den bugünkü ilaçları çek
        val repository = MedicineRepository()
        val upcomingMedicines = withContext(Dispatchers.IO) {
            try {
                repository.getUpcomingMedicines(context)
            } catch (e: Exception) {
                android.util.Log.e("ReminderWidget", "Error fetching medicines", e)
                emptyList()
            }
        }

        // WidgetReminder listesine dönüştür
        val reminders = upcomingMedicines.map { (medicine, time) ->
            WidgetReminder(
                medicineId = medicine.id,
                medicineName = medicine.name,
                dosage = medicine.dosage,
                unit = medicine.unit,
                time = time,
                icon = medicine.icon,
                colorHex = medicine.color.hexColor
            )
        }

        val remindersJson = Gson().toJson(reminders)

        // Tüm widget instance'larını güncelle
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, ReminderWidgetStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    val currentIndex = this[ReminderWidgetKeys.CURRENT_INDEX] ?: 0
                    this[ReminderWidgetKeys.TOTAL_COUNT] = reminders.size
                    this[ReminderWidgetKeys.REMINDERS_JSON] = remindersJson
                    // Index'i geçerli aralıkta tut
                    if (currentIndex >= reminders.size) {
                        this[ReminderWidgetKeys.CURRENT_INDEX] = (reminders.size - 1).coerceAtLeast(0)
                    }
                }
            }
            ReminderWidget().update(context, glanceId)
        }
    }
}
