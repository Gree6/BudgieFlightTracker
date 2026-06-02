package com.example.budgieflighttracker.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

data class TrackedFlight(
    val icao24: String,
    val startTime: Int,
    val endTime: Int,
    val callsign: String?,
    val path: List<Waypoint>
)

data class Waypoint(
    val time: Int,
    val latitude: Float,
    val longitude: Float,
    val baro_altitude: Float,
    val true_track: Float,
    val on_ground: Boolean
)

/**
 * Custom deserializer for TrackedFlight because the OpenSky API returns
 * the path as an array of arrays instead of an array of objects
 */
class TrackedFlightDeserializer : JsonDeserializer<TrackedFlight> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): TrackedFlight {
        val jsonObject = json.asJsonObject

        val icao24 = jsonObject.get("icao24").asString
        val startTime = jsonObject.get("startTime").asInt
        val endTime = jsonObject.get("endTime").asInt
        val callsign = if (jsonObject.has("callsign")) jsonObject.get("callsign").asString else null

        val pathArray = jsonObject.getAsJsonArray("path")
        val waypoints = mutableListOf<Waypoint>()

        for (waypointElement in pathArray) {
            val waypointArray = waypointElement.asJsonArray
            if (waypointArray.size() >= 6) {
                val waypoint = Waypoint(
                    time = waypointArray[0].asInt,
                    latitude = waypointArray[1].asFloat,
                    longitude = waypointArray[2].asFloat,
                    baro_altitude = waypointArray[3].asFloat,
                    true_track = waypointArray[4].asFloat,
                    on_ground = waypointArray[5].asBoolean
                )
                waypoints.add(waypoint)
            }
        }

        return TrackedFlight(icao24, startTime, endTime, callsign, waypoints)
    }
}
