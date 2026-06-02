package com.example.budgieflighttracker.notifications

import android.content.Context
import android.content.SharedPreferences

class NotificationPreferences(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "notification_preferences"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_ALERT_RADIUS = "alert_radius"
        private const val KEY_USER_LOCATION_LAT = "user_location_lat"
        private const val KEY_USER_LOCATION_LON = "user_location_lon"
    }

    // Save notification enabled state
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }

    // Get notification enabled state
    fun areNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    // Save alert radius (in kilometers)
    fun setAlertRadius(radius: Int) {
        sharedPreferences.edit()
            .putInt(KEY_ALERT_RADIUS, radius)
            .apply()
    }

    // Get alert radius
    fun getAlertRadius(): Int {
        return sharedPreferences.getInt(KEY_ALERT_RADIUS, 10) // Default 10km
    }

    // Save user location for proximity alerts
    fun setUserLocation(latitude: Double, longitude: Double) {
        sharedPreferences.edit()
            .putFloat(KEY_USER_LOCATION_LAT, latitude.toFloat())
            .putFloat(KEY_USER_LOCATION_LON, longitude.toFloat())
            .apply()
    }

    // Get user location
    fun getUserLocation(): Pair<Double, Double>? {
        val lat = sharedPreferences.getFloat(KEY_USER_LOCATION_LAT, Float.NaN)
        val lon = sharedPreferences.getFloat(KEY_USER_LOCATION_LON, Float.NaN)

        return if (lat.isNaN() || lon.isNaN()) {
            null
        } else {
            Pair(lat.toDouble(), lon.toDouble())
        }
    }
}
