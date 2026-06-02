package com.example.budgieflighttracker.jsontokt

/**
 * Data class representing a single arrival flight from the API response.
 */
data class ArrivalFlight(
    val icao24: String,
    val firstSeen: Long,
    val estDepartureAirport: String?,
    val lastSeen: Long,
    val estArrivalAirport: String?,
    val callsign: String?,
    val estDepartureAirportHorizDistance: Int?,
    val estDepartureAirportVertDistance: Int?,
    val estArrivalAirportHorizDistance: Int?,
    val estArrivalAirportVertDistance: Int?,
    val departureAirportCandidatesCount: Int,
    val arrivalAirportCandidatesCount: Int
)

/**
 * Wrapper for the arrivals API response (list of ArrivalFlight).
 */
data class ApiResponseArrivals(
    val arrivals: List<ArrivalFlight>
)

/**
 * Data class representing a single departure flight from the API response.
 */
data class DepartureFlight(
    val icao24: String,
    val firstSeen: Long,
    val estDepartureAirport: String?,
    val lastSeen: Long,
    val estArrivalAirport: String?,
    val callsign: String?,
    val estDepartureAirportHorizDistance: Int?,
    val estDepartureAirportVertDistance: Int?,
    val estArrivalAirportHorizDistance: Int?,
    val estArrivalAirportVertDistance: Int?,
    val departureAirportCandidatesCount: Int,
    val arrivalAirportCandidatesCount: Int
)

/**
 * Wrapper for the departures API response (list of DepartureFlight).
 */
data class ApiResponseDepartures(
    val departures: List<DepartureFlight>
)
