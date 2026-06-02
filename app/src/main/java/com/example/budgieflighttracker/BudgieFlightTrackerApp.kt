
package com.example.budgieflighttracker

import android.app.Application
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.budgieflighttracker.notifications.NotificationHelper
import com.example.budgieflighttracker.notifications.NotificationWorker
import java.util.concurrent.TimeUnit

class BudgieFlightTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()


        //apply language changes when app starts
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val languageCode = sharedPref.getString("app_language", "en") ?: "en"

        LocaleHelper.setLocale(this, languageCode)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule periodic flight checks
        scheduleFlightChecks()
    }

    private fun scheduleFlightChecks() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FlightCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
            )
        }
}