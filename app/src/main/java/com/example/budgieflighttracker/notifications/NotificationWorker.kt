package com.example.budgieflighttracker.notifications

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.budgieflighttracker.datasource.FlightAPI
import kotlin.compareTo
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.text.toDouble
import kotlin.text.toFloat

data class BoundingBox(
    val latMin: Double,
    val latMax: Double,
    val lonMin: Double,
    val lonMax: Double
)

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        private const val TAG = "FlightCheckWorker"
        private const val DEFAULT_RADIUS_KM = 50.0
    }

    override fun doWork(): Result {
        return try {
            Log.d(TAG, "start checking flights")

            val notificationPrefs = NotificationPreferences(applicationContext)

            // Check if alerts are enabled
            if (!notificationPrefs.areNotificationsEnabled()) {
                Log.d(TAG, "Alerts disabled")
                return Result.success()
            }

            // Get user location and radius
            val userLocation = notificationPrefs.getUserLocation()
            if (userLocation == null) {
                Log.d(TAG, "User location not set")
                return Result.success()
            }

            val (userLat, userLon) = userLocation
            val radiusKm = notificationPrefs.getAlertRadius().toDouble()

            val boundingBox = calculateBoundingBox(userLat, userLon, radiusKm)
            val flightAPI = FlightAPI()
            val flights = flightAPI.returnFlights(
                boundingBox.lonMin.toFloat(),
                boundingBox.latMin.toFloat(),
                boundingBox.lonMax.toFloat(),
                boundingBox.latMax.toFloat()
            )

            if (flights.isNullOrEmpty()) {
                Log.d(TAG, "No flights found in area")
                return Result.success()
            }

            val nearbyFlights = flights.filter { flight ->
                flight.latitude != null && flight.longitude != null &&
                        calculateDistance(
                            userLat, userLon,
                            flight.latitude, flight.longitude
                        ) <= radiusKm
            }

            Log.d(TAG, "Found ${nearbyFlights.size} flights within ${radiusKm}km")

            if (nearbyFlights.isNotEmpty()) {
                val callsigns = nearbyFlights.mapNotNull { it.callsign?.trim() }.filter { it.isNotEmpty() }
                NotificationHelper.showFlightNotification(
                    applicationContext,
                    nearbyFlights.size,
                    callsigns
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking flights: ${e.message}", e)
            Result.retry()
        }
    }


    //calculate distance between two lat/lon points using Haversine formula
    //https://www.movable-type.co.uk/scripts/latlong.html
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun calculateBoundingBox(lat: Double, lon: Double, radiusKm: Double): BoundingBox {
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(lat)))
        return BoundingBox(
            latMin = lat - latDelta,
            latMax = lat + latDelta,
            lonMin = lon - lonDelta,
            lonMax = lon + lonDelta
        )
    }
}
