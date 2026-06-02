package com.example.budgieflighttracker.models

data class Aircraft(
    val icao24: String,
    val callSign: String?,
    val originCountry: String,
    val category: Int
)
