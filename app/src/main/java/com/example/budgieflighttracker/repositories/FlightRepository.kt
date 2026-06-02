package com.example.budgieflighttracker.repositories

import com.example.budgieflighttracker.datasource.FlightRemoteDataSource
import com.example.budgieflighttracker.datasource.FlightLocalDataSource
import com.example.budgieflighttracker.models.Flight
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class FlightRepository(
    private val flightRemoteDataSource: FlightRemoteDataSource,
    private val flightLocalDataSource: FlightLocalDataSource? = null
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateHourFormatter = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault())

    // CRUD

    // Read all available flight hours
    fun getAvailableFlightDates(
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Prefer local cache when available
        flightLocalDataSource?.getAvailableHours(
            onSuccess = { hours -> onSuccess(hours) },
            onFailure = { _ ->
                // Fallback to remote
                flightRemoteDataSource.getAvailableHours(onSuccess, onFailure)
            }
        ) ?: flightRemoteDataSource.getAvailableHours(onSuccess, onFailure)
    }

    // Read all available flight days
    fun getAvailableFlightDays(
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        flightLocalDataSource?.getAvailableDays(
            onSuccess = { days -> onSuccess(days) },
            onFailure = { _ ->
                flightRemoteDataSource.getAvailableDays(onSuccess, onFailure)
            }
        ) ?: flightRemoteDataSource.getAvailableDays(onSuccess, onFailure)
    }

    // Get flights for a specific hour
    fun getFlightsForDate(
        dateHour: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getFlightsForHour(dateHour, onSuccess, onFailure)
    }

    // Get flights for a specific hour (clearer method name)
    fun getFlightsForHour(
        dateHour: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Try local first
        flightLocalDataSource?.getFlightsForHour(
            dateHour,
            onSuccess = { localFlights ->
                if (localFlights.isNotEmpty()) {
                    onSuccess(localFlights)
                } else {
                    // Fallback to remote and cache
                    flightRemoteDataSource.getFlightsForHour(
                        dateHour,
                        onSuccess = { remoteFlights ->
                            // Cache remotely fetched group locally
                            flightLocalDataSource?.cacheFlightGroup(dateHour, remoteFlights)
                            onSuccess(remoteFlights)
                        },
                        onFailure = onFailure
                    )
                }
            },
            onFailure = { _ ->
                // If local read fails, fallback to remote
                flightRemoteDataSource.getFlightsForHour(dateHour, onSuccess, onFailure)
            }
        ) ?: run {
            // No local datasource configured, use remote
            flightRemoteDataSource.getFlightsForHour(dateHour, onSuccess, onFailure)
        }
    }

    // Get flights for a specific day (all hours)
    fun getFlightsForDay(
        date: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val startHour = "$date-00"
        val endHour = "$date-23"

        flightLocalDataSource?.getFlightsForDateRange(
            startHour,
            endHour,
            onSuccess = { localFlights ->
                if (localFlights.isNotEmpty()) {
                    onSuccess(localFlights)
                } else {
                    // Fallback to remote and cache groups
                    flightRemoteDataSource.getFlightsForDay(
                        date,
                        onSuccess = { remoteFlights ->
                            // Group remote results by hour and cache each group
                            val grouped = remoteFlights.groupBy { flight ->
                                val dateHour = dateHourFormatter.format(Date(flight.lastContact * 1000))
                                dateHour
                            }
                            grouped.forEach { (hour, flights) ->
                                flightLocalDataSource?.cacheFlightGroup(hour, flights)
                            }
                            onSuccess(remoteFlights)
                        },
                        onFailure = onFailure
                    )
                }
            },
            onFailure = { _ ->
                flightRemoteDataSource.getFlightsForDay(date, onSuccess, onFailure)
            }
        ) ?: flightRemoteDataSource.getFlightsForDay(date, onSuccess, onFailure)
    }

    // Return Flights that match a specific plane ID across all hours
    fun returnFlight(
        icao24: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // If local available, search local cache first
        flightLocalDataSource?.getAvailableHours(
            onSuccess = { hours ->
                if (hours.isEmpty()) {
                    // Fallback to remote search if no local hours
                    performRemoteReturnFlightSearch(icao24, onSuccess, onFailure)
                    return@getAvailableHours
                }

                val allMatchingFlights = mutableListOf<Flight>()
                var completedSearches = 0
                val totalSearches = hours.size

                hours.forEach { hour ->
                    flightLocalDataSource.getFlightsForHour(
                        hour,
                        onSuccess = { flights ->
                            val matching = flights.filter { it.icao24 == icao24 }
                            allMatchingFlights.addAll(matching)
                            completedSearches++
                            if (completedSearches == totalSearches) {
                                onSuccess(allMatchingFlights.sortedBy { it.lastContact })
                            }
                        },
                        onFailure = { _ ->
                            completedSearches++
                            if (completedSearches == totalSearches) {
                                onSuccess(allMatchingFlights.sortedBy { it.lastContact })
                            }
                        }
                    )
                }
            },
            onFailure = { _ ->
                performRemoteReturnFlightSearch(icao24, onSuccess, onFailure)
            }
        ) ?: performRemoteReturnFlightSearch(icao24, onSuccess, onFailure)
    }

    private fun performRemoteReturnFlightSearch(
        icao24: String,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Reuse existing remote-based approach
        flightRemoteDataSource.getAvailableHours(
            onSuccess = { hours ->
                val allMatchingFlights = mutableListOf<Flight>()
                var completedSearches = 0
                val totalSearches = hours.size

                if (hours.isEmpty()) {
                    onSuccess(emptyList())
                    return@getAvailableHours
                }

                hours.forEach { hour ->
                    flightRemoteDataSource.getFlightsForHour(
                        hour,
                        onSuccess = { flights ->
                            val matchingFlights = flights.filter { it.icao24 == icao24 }
                            allMatchingFlights.addAll(matchingFlights)
                            completedSearches++

                            if (completedSearches == totalSearches) {
                                onSuccess(allMatchingFlights.sortedBy { it.lastContact })
                            }
                        },
                        onFailure = { exception ->
                            completedSearches++
                            if (completedSearches == totalSearches) {
                                onSuccess(allMatchingFlights.sortedBy { it.lastContact })
                            }
                        }
                    )
                }
            },
            onFailure = onFailure
        )
    }

    // Return Aircraft in the air around a specific timestamp
    fun returnFlights(
        time: Timestamp,
        timeRangeMinutes: Long = 30,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val targetDate = Date(time.time)
        val targetDateHour = dateHourFormatter.format(targetDate)

        // Calculate hour range to search (previous and next hours to cover range)
        val calendar = Calendar.getInstance()
        calendar.time = targetDate
        calendar.add(Calendar.HOUR_OF_DAY, -1)
        val startHour = dateHourFormatter.format(calendar.time)

        calendar.time = targetDate
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        val endHour = dateHourFormatter.format(calendar.time)

        // Try local first
        flightLocalDataSource?.getFlightsForDateRange(
            startHour,
            endHour,
            onSuccess = { allFlights ->
                if (allFlights.isNotEmpty()) {
                    val targetTimeSeconds = time.time / 1000
                    val rangeStart = targetTimeSeconds - (timeRangeMinutes * 60)
                    val rangeEnd = targetTimeSeconds + (timeRangeMinutes * 60)

                    val flightsInRange = allFlights.filter { flight ->
                        flight.lastContact in rangeStart..rangeEnd
                    }.sortedBy { it.lastContact }

                    onSuccess(flightsInRange)
                } else {
                    // Fallback to remote
                    flightRemoteDataSource.getFlightsForDateRange(
                        startHour,
                        endHour,
                        onSuccess = { remoteFlights ->
                            val targetTimeSeconds = time.time / 1000
                            val rangeStart = targetTimeSeconds - (timeRangeMinutes * 60)
                            val rangeEnd = targetTimeSeconds + (timeRangeMinutes * 60)

                            val flightsInRange = remoteFlights.filter { flight ->
                                flight.lastContact in rangeStart..rangeEnd
                            }.sortedBy { it.lastContact }

                            onSuccess(flightsInRange)
                        },
                        onFailure = onFailure
                    )
                }
            },
            onFailure = { _ ->
                flightRemoteDataSource.getFlightsForDateRange(startHour, endHour, onSuccess, onFailure)
            }
        ) ?: flightRemoteDataSource.getFlightsForDateRange(startHour, endHour, onSuccess, onFailure)
    }

    // Get flights for current hour
    fun getCurrentHourFlights(
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentHour = dateHourFormatter.format(Date())
        getFlightsForHour(currentHour, onSuccess, onFailure)
    }

    // Get flights for today
    fun getTodayFlights(
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val today = dateFormatter.format(Date())
        getFlightsForDay(today, onSuccess, onFailure)
    }

    // Get flights for hour range
    fun getFlightsForDateRange(
        startDate: Date,
        endDate: Date,
        onSuccess: (List<Flight>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val startDateHour = dateHourFormatter.format(startDate)
        val endDateHour = dateHourFormatter.format(endDate)
        // Prefer local
        flightLocalDataSource?.getFlightsForDateRange(
            startDateHour,
            endDateHour,
            onSuccess = { localFlights ->
                if (localFlights.isNotEmpty()) onSuccess(localFlights)
                else flightRemoteDataSource.getFlightsForDateRange(startDateHour, endDateHour, onSuccess, onFailure)
            },
            onFailure = { _ ->
                flightRemoteDataSource.getFlightsForDateRange(startDateHour, endDateHour, onSuccess, onFailure)
            }
        ) ?: flightRemoteDataSource.getFlightsForDateRange(startDateHour, endDateHour, onSuccess, onFailure)
    }

    // Won't be used currently
    fun updateFlight(icao24: String) {
        // Not implemented - would need to update flights within their hour groups
    }
}