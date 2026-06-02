package com.example.budgieflighttracker.datasource

import com.example.budgieflighttracker.models.Flight
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch

// Data class for grouped flights by hour
data class FlightGroup(
    val dateHour: String = "", // Format: "2024-11-14-14" (year-month-day-hour)
    val flights: List<Flight> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    // No-argument constructor required by Firestore
    constructor() : this("", emptyList(), System.currentTimeMillis())
}

class FlightRemoteDataSource {

    private val firestore = FirebaseFirestore.getInstance()
    private val dateHourFormatter = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault())

    // Insert flights grouped by hour - much more efficient structure
    fun insertFlightsGroupedByDate(
        flights: List<Flight>,
        onSuccess: (Int) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (flights.isEmpty()) {
            onSuccess(0)
            return
        }

        // Group flights by hour
        val flightsByHour = flights.groupBy { flight ->
            val date = Date(flight.lastContact * 1000) // Convert from seconds to milliseconds
            dateHourFormatter.format(date)
        }

        var completedGroups = 0
        var totalInserted = 0
        val totalGroups = flightsByHour.size

        flightsByHour.forEach { (dateHour, flightsForHour) ->
            // Remove duplicates within the same hour group
            val uniqueFlights = flightsForHour.groupBy { "${it.icao24}_${it.lastContact}" }
                .map { it.value.first() }

            val flightGroup = FlightGroup(
                dateHour = dateHour,
                flights = uniqueFlights
            )

            // Use dateHour as document ID to enable easy querying and prevent duplicate hour groups
            val docRef = firestore.collection("flight_groups").document(dateHour)

            docRef.set(flightGroup)
                .addOnSuccessListener {
                    completedGroups++
                    totalInserted += uniqueFlights.size
                    println("Successfully uploaded flight group for $dateHour with ${uniqueFlights.size} flights")

                    if (completedGroups == totalGroups) {
                        onSuccess(totalInserted)
                    }
                }
                .addOnFailureListener { exception ->
                    println("Error uploading flight group for $dateHour: ${exception.message}")
                    onFailure(exception)
                }
        }
    }

    // Merge new flights into existing hour groups
    fun mergeFlightsIntoDateGroups(
        flights: List<Flight>,
        onSuccess: (Int) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (flights.isEmpty()) {
            onSuccess(0)
            return
        }

        // Group flights by hour
        val flightsByHour = flights.groupBy { flight ->
            val date = Date(flight.lastContact * 1000)
            dateHourFormatter.format(date)
        }

        var completedGroups = 0
        var totalProcessed = 0
        val totalGroups = flightsByHour.size

        flightsByHour.forEach { (dateHour, newFlights) ->
            val docRef = firestore.collection("flight_groups").document(dateHour)

            // First, try to get existing group
            docRef.get()
                .addOnSuccessListener { document ->
                    val existingFlights = if (document.exists()) {
                        val existingGroup = document.toObject(FlightGroup::class.java)
                        existingGroup?.flights ?: emptyList()
                    } else {
                        emptyList()
                    }

                    // Combine existing and new flights, removing duplicates
                    val allFlights = (existingFlights + newFlights)
                        .groupBy { "${it.icao24}_${it.lastContact}" }
                        .map { it.value.first() }

                    val updatedGroup = FlightGroup(
                        dateHour = dateHour,
                        flights = allFlights
                    )

                    // Update the document
                    docRef.set(updatedGroup)
                        .addOnSuccessListener {
                            completedGroups++
                            totalProcessed += newFlights.size
                            println("Successfully merged ${newFlights.size} flights into group for $dateHour (total: ${allFlights.size})")

                            if (completedGroups == totalGroups) {
                                onSuccess(totalProcessed)
                            }
                        }
                        .addOnFailureListener { exception ->
                            println("Error merging flights for $dateHour: ${exception.message}")
                            onFailure(exception)
                        }
                }
                .addOnFailureListener { exception ->
                    println("Error getting existing group for $dateHour: ${exception.message}")
                    onFailure(exception)
                }
        }
    }

    // Get flights for a specific hour
    fun getFlightsForDate(
        dateHour: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("flight_groups").document(dateHour)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val flightGroup = document.toObject(FlightGroup::class.java)
                    onSuccess(flightGroup?.flights ?: emptyList())
                } else {
                    onSuccess(emptyList())
                }
            }
            .addOnFailureListener { exception ->
                println("Error getting flights for hour $dateHour: ${exception.message}")
                onFailure(exception)
            }
    }

    // Get flights for a specific hour (new method with clearer naming)
    fun getFlightsForHour(
        dateHour: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getFlightsForDate(dateHour, onSuccess, onFailure)
    }

    // Get flights for a specific day (all hours within that day)
    fun getFlightsForDay(
        date: String, // Format: "2024-11-14"
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val startHour = "$date-00"
        val endHour = "$date-23"
        getFlightsForDateRange(startHour, endHour, onSuccess, onFailure)
    }

    // Get flights for an hour range
    fun getFlightsForDateRange(
        startDateHour: String,
        endDateHour: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("flight_groups")
            .whereGreaterThanOrEqualTo("dateHour", startDateHour)
            .whereLessThanOrEqualTo("dateHour", endDateHour)
            .get()
            .addOnSuccessListener { result ->
                val allFlights = mutableListOf<Flight>()
                for (document in result) {
                    try {
                        val flightGroup = document.toObject(FlightGroup::class.java)
                        allFlights.addAll(flightGroup.flights)
                    } catch (e: Exception) {
                        println("Error converting document to FlightGroup: ${e.message}")
                    }
                }
                onSuccess(allFlights.sortedBy { it.lastContact })
            }
            .addOnFailureListener { exception ->
                println("Error getting flights for hour range $startDateHour to $endDateHour: ${exception.message}")
                onFailure(exception)
            }
    }

    // Get available hours
    fun getAvailableDates(
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("flight_groups")
            .orderBy("dateHour", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val dateHours = result.documents.mapNotNull { it.id }
                onSuccess(dateHours)
            }
            .addOnFailureListener { exception ->
                println("Error getting available hours: ${exception.message}")
                onFailure(exception)
            }
    }

    // Get available hours (new method with clearer naming)
    fun getAvailableHours(
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getAvailableDates(onSuccess, onFailure)
    }

    // Get available days (grouped from hours)
    fun getAvailableDays(
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("flight_groups")
            .orderBy("dateHour", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val days = result.documents.mapNotNull { document ->
                    // Extract date from dateHour format "2024-11-14-14" -> "2024-11-14"
                    document.id?.substringBeforeLast("-")
                }.distinct().sorted()
                onSuccess(days)
            }
            .addOnFailureListener { exception ->
                println("Error getting available days: ${exception.message}")
                onFailure(exception)
            }
    }
}