package com.example.budgieflighttracker.services

import com.example.budgieflighttracker.datasource.FlightRemoteDataSource
import com.example.budgieflighttracker.models.Flight
import java.text.SimpleDateFormat
import java.util.*

class FlightPlaybackService {

    private val firestore = FlightRemoteDataSource()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateHourFormatter = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault())

    /**
     * Get all available hours that have flight data for playback selection
     */
    fun getAvailablePlaybackHours(
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.getAvailableHours(onSuccess, onFailure)
    }

    /**
     * Get all available days that have flight data for playback selection
     */
    fun getAvailablePlaybackDays(
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.getAvailableDays(onSuccess, onFailure)
    }

    /**
     * Get flights for a specific hour for playback
     */
    fun getFlightsForPlayback(
        dateHour: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.getFlightsForHour(dateHour, onSuccess, onFailure)
    }

    /**
     * Get flights for a specific day for playback (all hours)
     */
    fun getFlightsForDayPlayback(
        date: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.getFlightsForDay(date, onSuccess, onFailure)
    }

    /**
     * Get flights for an hour range for extended playback
     */
    fun getFlightsForHourRange(
        startHour: String,
        endHour: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.getFlightsForDateRange(startHour, endHour, onSuccess, onFailure)
    }

    /**
     * Get flights for a date range for extended playback
     */
    fun getFlightsForDateRange(
        startDate: Date,
        endDate: Date,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val startDateHour = dateHourFormatter.format(startDate)
        val endDateHour = dateHourFormatter.format(endDate)
        firestore.getFlightsForDateRange(startDateHour, endDateHour, onSuccess, onFailure)
    }

    /**
     * Get flights for playback by number of hours back from current time
     */
    fun getFlightsForLastNHours(
        hoursBack: Int,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        calendar.add(Calendar.HOUR_OF_DAY, -hoursBack)
        val startDate = calendar.time

        getFlightsForDateRange(startDate, endDate, onSuccess, onFailure)
    }

    /**
     * Get flights for playback by number of days back from today
     */
    fun getFlightsForLastNDays(
        daysBack: Int,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        calendar.add(Calendar.DAY_OF_YEAR, -daysBack)
        val startDate = calendar.time

        getFlightsForDateRange(startDate, endDate, onSuccess, onFailure)
    }

    /**
     * Convert hour string to user-friendly format for UI
     */
    fun formatHourForDisplay(hourString: String): String {
        return try {
            val date = dateHourFormatter.parse(hourString)
            val displayFormatter = SimpleDateFormat("MMMM dd, yyyy 'at' HH:00", Locale.getDefault())
            displayFormatter.format(date!!)
        } catch (e: Exception) {
            hourString
        }
    }

    /**
     * Convert date string to user-friendly format for UI
     */
    fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = dateFormatter.parse(dateString)
            val displayFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            displayFormatter.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Get flights sorted by time for chronological playback within an hour
     */
    fun getFlightsForPlaybackSorted(
        dateHour: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.getFlightsForHour(
            dateHour,
            onSuccess = { flights ->
                val sortedFlights = flights.sortedBy { it.lastContact }
                onSuccess(sortedFlights)
            },
            onFailure = onFailure
        )
    }

    /**
     * Get hourly breakdown for a specific day
     */
    fun getHourlyBreakdownForDay(
        date: String,
        onSuccess: (Map<String, Int>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.getAvailableHours(
            onSuccess = { hours ->
                val dayHours = hours.filter { it.startsWith(date) }
                val hourCounts = mutableMapOf<String, Int>()
                var completedHours = 0

                if (dayHours.isEmpty()) {
                    onSuccess(emptyMap())
                    return@getAvailableHours
                }

                dayHours.forEach { hour ->
                    firestore.getFlightsForHour(
                        hour,
                        onSuccess = { flights ->
                            hourCounts[hour] = flights.size
                            completedHours++

                            if (completedHours == dayHours.size) {
                                onSuccess(hourCounts.toSortedMap())
                            }
                        },
                        onFailure = { _ ->
                            hourCounts[hour] = 0
                            completedHours++

                            if (completedHours == dayHours.size) {
                                onSuccess(hourCounts.toSortedMap())
                            }
                        }
                    )
                }
            },
            onFailure = onFailure
        )
    }
}
