// ...new file...
package com.example.budgieflighttracker.datasource

import android.content.Context
import com.example.budgieflighttracker.models.Flight
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.Exception
import java.util.concurrent.Executors

/**
 * Simple file-based local cache for flight_groups.
 * This avoids adding Room to the project while providing offline access.
 *
 * Behavior
 * - Stores each dateHour as a JSON file named "flight_group_<dateHour>.json" in app files dir.
 * - Provides the same high-level async callbacks as the remote data source.
 * - Operations run on a background thread to avoid main-thread IO.
 */
class FlightLocalDataSource(private val context: Context) {
    private val gson = Gson()
    private val executor = Executors.newSingleThreadExecutor()

    private fun fileForDateHour(dateHour: String): File {
        val safeName = dateHour.replace(Regex("[^A-Za-z0-9_\\-]"), "_")
        return File(context.filesDir, "flight_group_$safeName.json")
    }

    // Cache a flight group for a specific hour (overwrites existing file)
    fun cacheFlightGroup(
        dateHour: String,
        flights: List<Flight>,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        executor.execute {
            try {
                val file = fileForDateHour(dateHour)
                val json = gson.toJson(flights)
                file.writeText(json)
                onSuccess?.invoke()
            } catch (e: Exception) {
                onFailure?.invoke(e)
            }
        }
    }

    // Merge new flights into cached group for the hour (deduplicates by icao24 + lastContact)
    fun mergeFlightsIntoGroup(
        dateHour: String,
        newFlights: List<Flight>,
        onSuccess: ((Int) -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        executor.execute {
            try {
                val existing = readFlightsSync(dateHour).toMutableList()
                // Combine and dedupe by key
                val combined = (existing + newFlights)
                    .groupBy { "${it.icao24}_${it.lastContact}" }
                    .map { it.value.first() }

                // Write back
                val file = fileForDateHour(dateHour)
                file.writeText(gson.toJson(combined))
                onSuccess?.invoke(newFlights.size)
            } catch (e: Exception) {
                onFailure?.invoke(e)
            }
        }
    }

    // Get flights for a specific hour
    fun getFlightsForHour(
        dateHour: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        executor.execute {
            try {
                val flights = readFlightsSync(dateHour)
                onSuccess(flights)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    // Synchronous helper used internally
    private fun readFlightsSync(dateHour: String): List<Flight> {
        val file = fileForDateHour(dateHour)
        if (!file.exists()) return emptyList()
        val json = file.readText()
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<Flight>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // Get flights for a range of dateHours (inclusive) - aggregates cached hours
    fun getFlightsForDateRange(
        startDateHour: String,
        endDateHour: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        executor.execute {
            try {
                val hours = getAvailableHoursSync()
                val selected = hours.filter { it >= startDateHour && it <= endDateHour }
                val allFlights = mutableListOf<Flight>()
                selected.forEach { hour ->
                    allFlights.addAll(readFlightsSync(hour))
                }
                onSuccess(allFlights.sortedBy { it.lastContact })
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    // List cached dateHour files
    fun getAvailableHours(
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        executor.execute {
            try {
                val hours = getAvailableHoursSync()
                onSuccess(hours)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    private fun getAvailableHoursSync(): List<String> {
        val prefix = "flight_group_"
        return context.filesDir.listFiles()
            ?.mapNotNull { file ->
                val name = file.name
                if (name.startsWith(prefix) && name.endsWith(".json")) {
                    val core = name.removePrefix(prefix).removeSuffix(".json")
                    core.replace(Regex("_[^A-Za-z0-9\\-]"), "_") // keep sanitized
                } else null
            }
            ?.sortedDescending()
            ?: emptyList()
    }

    // Get available days aggregated from cached hours
    fun getAvailableDays(
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        executor.execute {
            try {
                val days = getAvailableHoursSync()
                    .map { it.substringBeforeLast("-") }
                    .distinct()
                    .sorted()
                onSuccess(days)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }
}

