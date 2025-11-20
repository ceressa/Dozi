package com.bardino.dozi.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import androidx.glance.background
import androidx.glance.color.ColorProvider

/**
 * Dozi Hatırlatma Widget'ı
 */
class ReminderWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = ReminderWidgetStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Content(context)
        }
    }

    @Composable
    private fun Content(context: Context) {
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

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
                .background(
                    ColorProvider(
                        day = Color(WidgetColors.BackgroundLight),
                        night = Color(WidgetColors.BackgroundDark)
                    )
                )
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
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${currentIndex + 1}/$totalCount",
                style = TextStyle(
                    color = ColorProvider(
                        day = Color(WidgetColors.TextSecondary),
                        night = Color(WidgetColors.TextSecondary)
                    ),
                    fontSize = 12.sp
                )
            )
        }

        Spacer(GlanceModifier.height(4.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sol ok
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .cornerRadius(18.dp)
                    .background(
                        ColorProvider(
                            day = Color(if (hasPrevious) WidgetColors.Primary else WidgetColors.Disabled),
                            night = Color(if (hasPrevious) WidgetColors.Primary else WidgetColors.Disabled)
                        )
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
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(GlanceModifier.width(8.dp))

            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = reminder.icon,
                    style = TextStyle(fontSize = 28.sp)
                )

                Spacer(GlanceModifier.height(4.dp))

                Text(
                    text = reminder.medicineName,
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color(WidgetColors.TextPrimary),
                            night = Color(WidgetColors.TextPrimary)
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1
                )

                Spacer(GlanceModifier.height(2.dp))

                Text(
                    text = reminder.time,
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color(WidgetColors.Primary),
                            night = Color(WidgetColors.Primary)
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(GlanceModifier.height(2.dp))

                Text(
                    text = "${reminder.dosage} ${reminder.unit}",
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color(WidgetColors.TextSecondary),
                            night = Color(WidgetColors.TextSecondary)
                        ),
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(GlanceModifier.width(8.dp))

            // Sağ ok
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .cornerRadius(18.dp)
                    .background(
                        ColorProvider(
                            day = Color(if (hasNext) WidgetColors.Primary else WidgetColors.Disabled),
                            night = Color(if (hasNext) WidgetColors.Primary else WidgetColors.Disabled)
                        )
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
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(GlanceModifier.height(8.dp))

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
                    .background(ColorProvider(day = Color(WidgetColors.Success), night = Color(WidgetColors.Success)))
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
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(GlanceModifier.width(12.dp))

            // Atla butonu
            Box(
                modifier = GlanceModifier
                    .width(80.dp)
                    .height(32.dp)
                    .cornerRadius(16.dp)
                    .background(ColorProvider(day = Color(WidgetColors.Secondary), night = Color(WidgetColors.Secondary)))
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
                        color = ColorProvider(day = Color.White, night = Color.White),
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
        Text(text = "💊", style = TextStyle(fontSize = 32.sp))

        Spacer(GlanceModifier.height(8.dp))

        Text(
            text = "Bugün ilaç yok",
            style = TextStyle(
                color = ColorProvider(day = Color(WidgetColors.TextSecondary), night = Color(WidgetColors.TextSecondary)),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(GlanceModifier.height(4.dp))

        Text(
            text = "Dokunarak aç",
            style = TextStyle(
                color = ColorProvider(day = Color(WidgetColors.Primary), night = Color(WidgetColors.Primary)),
                fontSize = 12.sp
            )
        )
    }
}

object WidgetColors {
    val Primary = android.graphics.Color.parseColor("#A78BFA")
    val Secondary = android.graphics.Color.parseColor("#FDA4AF")
    val Success = android.graphics.Color.parseColor("#6EE7B7")
    val Disabled = android.graphics.Color.parseColor("#E0E0E0")
    val TextPrimary = android.graphics.Color.parseColor("#212121")
    val TextSecondary = android.graphics.Color.parseColor("#757575")
    val BackgroundLight = android.graphics.Color.parseColor("#F3E8FF")
    val BackgroundDark = android.graphics.Color.parseColor("#1E1E1E")
}

object ActionParamKeys {
    val MEDICINE_ID = ActionParameters.Key<String>("medicine_id")
    val MEDICINE_NAME = ActionParameters.Key<String>("medicine_name")
    val DOSAGE = ActionParameters.Key<String>("dosage")
    val TIME = ActionParameters.Key<String>("time")
}

class NoOpAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {}
}

class PreviousReminderAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, ReminderWidgetStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                val currentIndex = this[ReminderWidgetKeys.CURRENT_INDEX] ?: 0
                this[ReminderWidgetKeys.CURRENT_INDEX] = (currentIndex - 1).coerceAtLeast(0)
            }
        }
        ReminderWidget().update(context, glanceId)
    }
}

class NextReminderAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, ReminderWidgetStateDefinition, glanceId) { prefs ->
            val currentIndex = prefs[ReminderWidgetKeys.CURRENT_INDEX] ?: 0
            val totalCount = prefs[ReminderWidgetKeys.TOTAL_COUNT] ?: 0

            prefs.toMutablePreferences().apply {
                this[ReminderWidgetKeys.CURRENT_INDEX] =
                    (currentIndex + 1).coerceAtMost(totalCount - 1)
            }
        }
        ReminderWidget().update(context, glanceId)
    }
}

class TakeMedicineAction : ActionCallback {
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val medicineId = parameters[ActionParamKeys.MEDICINE_ID] ?: return
        val medicineName = parameters[ActionParamKeys.MEDICINE_NAME] ?: return
        val dosage = parameters[ActionParamKeys.DOSAGE] ?: return
        val time = parameters[ActionParamKeys.TIME] ?: return

        val scheduledTime = parseTimeToMillis(time)
        val repository = MedicationLogRepository(context)

        withContext(Dispatchers.IO) {
            repository.logMedicationTaken(
                medicineId = medicineId,
                medicineName = medicineName,
                dosage = dosage,
                scheduledTime = scheduledTime
            )
        }

        saveMedicineStatus(context, medicineId, time, "taken")
        ReminderWidgetUpdater.updateWidgets(context)
    }

    private fun parseTimeToMillis(time: String): Long {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun saveMedicineStatus(context: Context, medicineId: String, time: String, status: String) {
        val prefs = context.getSharedPreferences("medicine_status", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
        val key = "dose_${medicineId}_${today}_${time}"
        prefs.edit().putString(key, status).apply()
    }
}

class SkipMedicineAction : ActionCallback {
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val medicineId = parameters[ActionParamKeys.MEDICINE_ID] ?: return
        val medicineName = parameters[ActionParamKeys.MEDICINE_NAME] ?: return
        val dosage = parameters[ActionParamKeys.DOSAGE] ?: return
        val time = parameters[ActionParamKeys.TIME] ?: return

        val scheduledTime = parseTimeToMillis(time)
        val repository = MedicationLogRepository(context)

        withContext(Dispatchers.IO) {
            repository.logMedicationSkipped(
                medicineId = medicineId,
                medicineName = medicineName,
                dosage = dosage,
                scheduledTime = scheduledTime,
                reason = "Widget'tan atlandı"
            )
        }

        saveMedicineStatus(context, medicineId, time, "skipped")

        ReminderWidgetUpdater.updateWidgets(context)
    }

    private fun parseTimeToMillis(time: String): Long {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun saveMedicineStatus(context: Context, medicineId: String, time: String, status: String) {
        val prefs = context.getSharedPreferences("medicine_status", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
        val key = "dose_${medicineId}_${today}_${time}"
        prefs.edit().putString(key, status).apply()
    }
}

object ReminderWidgetUpdater {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateWidgets(context: Context) {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(ReminderWidget::class.java)

        val repository = MedicineRepository()
        val upcomingMedicines = withContext(Dispatchers.IO) {
            try {
                repository.getUpcomingMedicines(context)
            } catch (e: Exception) {
                android.util.Log.e("ReminderWidget", "Error fetching medicines", e)
                emptyList()
            }
        }

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

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, ReminderWidgetStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    val currentIndex = this[ReminderWidgetKeys.CURRENT_INDEX] ?: 0
                    this[ReminderWidgetKeys.TOTAL_COUNT] = reminders.size
                    this[ReminderWidgetKeys.REMINDERS_JSON] = remindersJson
                    if (currentIndex >= reminders.size) {
                        this[ReminderWidgetKeys.CURRENT_INDEX] = (reminders.size - 1).coerceAtLeast(0)
                    }
                }
            }
            ReminderWidget().update(context, glanceId)
        }
    }
}
