package com.example.budgieflighttracker.models

import java.io.Serializable

data class Flight(
    val id: String = "",
    val icao24: String = "",
    val longitude: Double? = null,
    val latitude: Double? = null,
    val baroAltitude: Double? = null,
    val geoAltitude: Double? = null,
    val onGround: Boolean = false,
    val velocity: Double? = null,
    val verticalRate: Double? = null,
    val spi: Boolean = false,
    val category: Int = 0,
    val callsign: String? = null,
    val originCountry: String = "",
    val timePosition: Long? = null,
    val lastContact: Long = 0,
    val heading: Double? = null,
    val sensors: List<Int>? = null,
    val squawk: String? = null
) : Serializable {
    // No-argument constructor required by Firestore
    constructor() : this(
        id = "",
        icao24 = "",
        longitude = null,
        latitude = null,
        baroAltitude = null,
        geoAltitude = null,
        onGround = false,
        velocity = null,
        verticalRate = null,
        spi = false,
        category = 0,
        callsign = null,
        originCountry = "",
        timePosition = null,
        lastContact = 0,
        heading = null,
        sensors = null,
        squawk = null
    )
}
