package com.divyansh.nc.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.divyansh.nc.data.NCDatabase
import com.divyansh.nc.data.NCSettings
import com.divyansh.nc.report.NCReports
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

private const val CHANNEL = "nc_reminders"

private fun post(context: Context, id: Int, title: String, body: String) {
    if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(NotificationChannel(CHANNEL, "NC fitness reminders", NotificationManager.IMPORTANCE_DEFAULT))
    val notification = NotificationCompat.Builder(context, CHANNEL)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .build()
    manager.notify(id, notification)
}

class ReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val dao = NCDatabase.get(applicationContext).dao()
        val settings = dao.getSettings() ?: NCSettings()
        val activity = dao.getActivity(LocalDate.now().toString())
        val missing = mutableListOf<String>()
        if ((activity?.steps ?: 0) < settings.stepGoal) missing.add("steps")
        if ((activity?.pushups ?: 0) < settings.pushupGoal) missing.add("push-ups")
        val details = if (missing.isEmpty()) "Review today's nutrition and workout entries." else
            "Check your ${missing.joinToString(" and ")} goal; review today's log."
        post(applicationContext, 110, "NC — Evening check-in", details)
        val today = LocalDate.now()
        if (today.dayOfMonth <= 7 && today.monthValue in listOf(1, 4, 7, 10)) {
            val prefs = applicationContext.getSharedPreferences("nc_tasks", Context.MODE_PRIVATE)
            val key = "${today.year}-Q${(today.monthValue - 1) / 3 + 1}"
            if (prefs.getString("last_quarter_notice", "") != key) {
                post(applicationContext, 111, "NC — Quarterly export", "New reporting quarter started. Export and archive your previous 3 months.")
                prefs.edit().putString("last_quarter_notice", key).apply()
            }
        }
        Result.success()
    } catch (_: Exception) { Result.retry() }
}

class DailyCsvWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val dao = NCDatabase.get(applicationContext).dao()
        val reporter = NCReports(applicationContext, dao)
        // Late-running jobs can cross midnight. Re-export yesterday as well as today.
        reporter.exportDay(LocalDate.now().minusDays(1))
        reporter.exportDay(LocalDate.now())
        reporter.exportReferences()
        Result.success()
    } catch (_: Exception) { Result.retry() }
}

object NCWorkScheduler {
    private fun millisUntil(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(0)
    }

    fun schedule(context: Context, settings: NCSettings) {
        val manager = WorkManager.getInstance(context.applicationContext)
        val reminder = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntil(settings.reminderHour, settings.reminderMinute), TimeUnit.MILLISECONDS)
            .build()
        val export = PeriodicWorkRequestBuilder<DailyCsvWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntil(23, 58), TimeUnit.MILLISECONDS)
            .build()
        manager.enqueueUniquePeriodicWork("nc_evening_reminder", ExistingPeriodicWorkPolicy.UPDATE, reminder)
        manager.enqueueUniquePeriodicWork("nc_daily_csv", ExistingPeriodicWorkPolicy.UPDATE, export)
    }
}
