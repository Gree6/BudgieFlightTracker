package com.example.budgieflighttracker.models

data class User(
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val authProvider: String = "email",
    val preferences: Map<String, Boolean> = mapOf(
        "show_airports" to true,
        "show_helicopters" to true,
        "show_gliders" to true,
        "push_notifications" to true,
        "enable_afrikaans" to false
    )
) {
    // No-argument constructor required for Firestore
    constructor() : this("", "", "", "", 0L, "email", mapOf())
}