package com.apka.terminarzkliniki.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.apka.terminarzkliniki.data.DbProvider
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatDateTime(millis: Long): String {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return df.format(Date(millis))
}


// ===== Powiadomienia + WorkManager =====

private const val NOTIF_CHANNEL_ID = "visit_reminders"
private const val KEY_VISIT_ID = "visit_id"

// Tworzy kanał powiadomień.
fun ensureNotificationChannel(context: Context) {
    val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (mgr.getNotificationChannel(NOTIF_CHANNEL_ID) == null) {
        val channel = NotificationChannel(
            NOTIF_CHANNEL_ID,
            "Przypomnienia o wizytach",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        mgr.createNotificationChannel(channel)
    }
}

// Android 13+ wymaga zgody POST_NOTIFICATIONS.
fun hasPostNotificationsPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    } else true
}

@SuppressLint("MissingPermission")
private fun safeNotify(context: Context, id: Int, notification: android.app.Notification) {
    try {
        if (!hasPostNotificationsPermission(context)) return
        NotificationManagerCompat.from(context).notify(id, notification)
    } catch (_: SecurityException) {
        // brak zgody – nie wysyłamy
    }
}

// Worker uruchamia się o czasie przypomnienia i pokazuje notyfikację.
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val visitId = inputData.getLong(KEY_VISIT_ID, -1L)
        if (visitId <= 0L) return Result.success()

        val dao = DbProvider.get(applicationContext).visitDao()
        val visit = dao.observeById(visitId).first() ?: return Result.success()

        // Nie powiadamiamy dla archiwum albo gdy przypomnienie wyłączone.
        if (visit.isArchived || !visit.remindEnabled) return Result.success()

        ensureNotificationChannel(applicationContext)

        val timeText = formatDateTime(visit.dateTimeMillis)
        val text = "Za ${visit.remindMinutes} min: ${visit.petName} (${visit.species}) • $timeText"

        val big = buildString {
            appendLine("Data: $timeText")
            appendLine("Za: ${visit.remindMinutes} min")
            appendLine("Zwierzak: ${visit.petName} (${visit.species})")
            appendLine("Właściciel: ${visit.ownerName}")
            if (visit.vetName.isNotBlank()) appendLine("Lekarz: ${visit.vetName}")
            if (visit.notes.isNotBlank()) appendLine("Opis: ${visit.notes}")
        }

        val notification = NotificationCompat.Builder(applicationContext, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Przypomnienie o wizycie")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setAutoCancel(true)
            .build()

        val notifId = (visitId % Int.MAX_VALUE).toInt()
        safeNotify(applicationContext, notifId, notification)

        return Result.success()
    }
}

private fun workNameForVisit(visitId: Long) = "visit_reminder_$visitId"

fun cancelReminder(context: Context, visitId: Long) {
    WorkManager.getInstance(context).cancelUniqueWork(workNameForVisit(visitId))
}

// Ustawiamy przypomnienie: wizyta - X minut.
fun scheduleReminder(context: Context, visitId: Long, visitTimeMillis: Long, remindMinutes: Int) {
    val triggerAt = visitTimeMillis - TimeUnit.MINUTES.toMillis(remindMinutes.toLong())
    val delay = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L)

    val request = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setInputData(workDataOf(KEY_VISIT_ID to visitId))
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        workNameForVisit(visitId),
        ExistingWorkPolicy.REPLACE,
        request
    )
}