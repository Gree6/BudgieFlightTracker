package com.example.budgieflighttracker.datasource

import com.example.budgieflighttracker.jsontokt.ApiResponse
import com.example.budgieflighttracker.jsontokt.ArrivalFlight
import com.example.budgieflighttracker.jsontokt.DepartureFlight
import com.example.budgieflighttracker.models.Flight
import com.example.budgieflighttracker.models.TrackedFlight
import com.example.budgieflighttracker.models.TrackedFlightDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Serializable
import android.util.Log

class FlightAPI {

    // Tempory Usage of Client and Secret for testing only
    private val clientId = "jwehart.7@gmail.com-api-client"
    private val clientSecret = "FiPI6Z8MA7qCn57ZZkCOBwHiDTUmtKt9"

    private val client = OkHttpClient()
    private val firestore = FlightRemoteDataSource()

    // Data class for airport flight info
    data class AirportFlightInfo(val airportIcao: String, val flightCount: Int): Serializable

    /**
     * Gets arrivals for an airport using OAuth2 authentication.
     */
    fun returnArrivals(airport: String, begin: Int, end: Int): List<ArrivalFlight>? {
        var flights: List<ArrivalFlight>? = null
        val thread = Thread {
            try {
                val token = getAccessToken(clientId, clientSecret)
                if (token == null) return@Thread
                val url = HttpUrl.Builder()
                    .scheme("https")
                    .host("opensky-network.org")
                    .addPathSegment("api")
                    .addPathSegment("flights")
                    .addPathSegment("arrival")
                    .addQueryParameter("airport", airport)
                    .addQueryParameter("begin", begin.toString())
                    .addQueryParameter("end", end.toString())
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val gson = Gson()
                    flights = gson.fromJson(body, Array<ArrivalFlight>::class.java).toList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
        thread.join()
        return flights
    }

    /**
     * Gets departures for an airport using OAuth2 authentication.
     */
    fun returnDepartures(airport: String, begin: Int, end: Int): List<DepartureFlight>? {
        var flights: List<DepartureFlight>? = null
        val thread = Thread {
            try {
                val token = getAccessToken(clientId, clientSecret)
                if (token == null) return@Thread
                val url = HttpUrl.Builder()
                    .scheme("https")
                    .host("opensky-network.org")
                    .addPathSegment("api")
                    .addPathSegment("flights")
                    .addPathSegment("departure")
                    .addQueryParameter("airport", airport)
                    .addQueryParameter("begin", begin.toString())
                    .addQueryParameter("end", end.toString())
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val gson = Gson()
                    flights = gson.fromJson(body, Array<DepartureFlight>::class.java).toList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
        thread.join()
        return flights
    }

    fun returnFlights(lomin: Float, lamin: Float, lomax: Float, lamax: Float) : List<Flight>? {
        var flights: List<Flight>? = null
        val thread = Thread {
            try {
                val url = HttpUrl.Builder()
                    .scheme("https")
                    .host("opensky-network.org")
                    .addPathSegment("api")
                    .addPathSegment("states")
                    .addPathSegment("all")
                    .addQueryParameter("lamin","$lamin")
                    .addQueryParameter("lomin","$lomin")
                    .addQueryParameter("lamax","$lamax")
                    .addQueryParameter("lomax","$lomax")
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .build()
                val response = client.newCall(request).execute()
                println(response.body?.string())

                val gson = Gson()
                val responseBody = client.newCall(request).execute().body
                val entity = gson.fromJson(responseBody!!.string(),ApiResponse::class.java)

                flights = entity.states.map { it.toFlight() }

                // Upload flights to Firestore organized by date groups for optimal performance
                flights?.let { flightList ->
                    firestore.mergeFlightsIntoDateGroups(
                        flightList,
                        onSuccess = { insertedCount ->
                            println("Successfully processed $insertedCount flights into date-grouped structure")
                        },
                        onFailure = { exception ->
                            println("Failed to upload flights to date groups: ${exception.message}")
                        }
                    )
                }

                print(flights)
                return@Thread
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
        thread.join()
        return flights
    }

    fun returnFlightsByCountry(lomin: Float, lamin: Float, lomax: Float, lamax: Float, country: String) : List<Flight>? {
        val flights = returnFlights(lomin,lamin,lomax,lamax)
        val filteredFlights = flights?.filter { it.originCountry == country }
        return filteredFlights
    }

    fun returnFlightsByAircraft(lomin: Float, lamin: Float, lomax: Float, lamax: Float, aircraftType: String) : List<Flight>? {
        val flights = returnFlights(lomin,lamin,lomax,lamax)
        val aircraftStrings = arrayOf(
            "No information at all",
            "No ADS-B Emitter Category Information",
            "Light (< 15,500 lbs)",
            "Small (15,500 to 75,000 lbs)",
            "Large (75,000 to 300,000 lbs)",
            "High Vortex Large (e.g., B-757)",
            "Heavy (> 300,000 lbs)",
            "High Performance (> 5g acceleration and 400 kts)",
            "Rotorcraft",
            "Glider / Sailplane",
            "Lighter-than-air",
            "Parachutist / Skydiver",
            "Ultralight / Hang-glider / Paraglider",
            "Reserved",
            "Unmanned Aerial Vehicle",
            "Space / Trans-atmospheric vehicle",
            "Surface Vehicle – Emergency Vehicle",
            "Surface Vehicle – Service Vehicle",
            "Point Obstacle (includes tethered balloons)",
            "Cluster Obstacle",
            "Line Obstacle"
        )

        val aircraftNumber = aircraftStrings.indexOf(aircraftType)
        val filteredFlights = flights?.filter { it.category == aircraftNumber }
        return filteredFlights
    }

    /**
     * Returns flights by arrival ICAO, returns airport ICAO and flight count.
     */
    fun returnFlightsByArrival(arrivalIcao: String): AirportFlightInfo {
        val currentUnixTime = (System.currentTimeMillis() / 1000L ) - 36000L
        val olderUnixTime = (System.currentTimeMillis() / 1000L)
        val arrivals = returnArrivals(arrivalIcao, currentUnixTime.toInt(), olderUnixTime.toInt())
        val count = arrivals?.size ?: 0
        return AirportFlightInfo(arrivalIcao, count)
    }

    /**
     * Returns flights by departure ICAO, returns airport ICAO and flight count.
     */
    fun returnFlightsByDepature(departureIcao: String): AirportFlightInfo {
        val currentUnixTime = System.currentTimeMillis() / 1000L - 36000f
        val olderUnixTime = (System.currentTimeMillis() / 1000L)
        val departures = returnDepartures(departureIcao, currentUnixTime.toInt(), olderUnixTime.toInt())
        val count = departures?.size ?: 0
        return AirportFlightInfo(departureIcao, count)
    }

    fun returnTrackedFlight(icao24: String): TrackedFlight? {
        var flight : TrackedFlight? = null
        val thread = Thread {
            try {
                Log.d("FlightAPI", "Fetching tracked flight for ICAO24: $icao24")
                val url = HttpUrl.Builder()
                    .scheme("https")
                    .host("opensky-network.org")
                    .addPathSegment("api")
                    .addPathSegment("tracks")
                    .addPathSegment("all")
                    .addQueryParameter("icao24", icao24)
                    .addQueryParameter("time", "0")
                    .build()

                Log.d("FlightAPI", "Request URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("FlightAPI", "Response code: ${response.code}")
                Log.d("FlightAPI", "Response body: ${responseBody?.take(500)}") // Log first 500 chars

                if (response.isSuccessful && responseBody != null) {
                    // Use custom deserializer to handle array-of-arrays format
                    val gson = GsonBuilder()
                        .registerTypeAdapter(TrackedFlight::class.java, TrackedFlightDeserializer())
                        .create()
                    flight = gson.fromJson(responseBody, TrackedFlight::class.java)
                    Log.d("FlightAPI", "Parsed TrackedFlight: icao24=${flight?.icao24}, waypoints=${flight?.path?.size}")
                } else {
                    Log.w("FlightAPI", "API call failed or returned empty body")
                }
            } catch (e: java.lang.Exception) {
                Log.e("FlightAPI", "Exception fetching tracked flight: ${e.message}", e)
                e.printStackTrace()
            }
        }
        thread.start()
        thread.join()
        return flight
    }

    /**
     * Requests an OAuth2 access token from OpenSky using client credentials.
     * @param clientId Your OpenSky API client_id
     * @param clientSecret Your OpenSky API client_secret
     * @return The access token as a String, or null if the request fails
     */
    fun getAccessToken(clientId: String, clientSecret: String): String? {
        val url = "https://auth.opensky-network.org/auth/realms/opensky-network/protocol/openid-connect/token"
        val requestBody = okhttp3.FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val json = Gson().fromJson(body, Map::class.java)
                json["access_token"] as? String
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a list of Any? objects to a Flight object.
     * The list is expected to have a specific structure, where each element
     * corresponds to a property of the Flight object.
     *
     * @return A Flight object initialized with the values from the list.
     * @throws ClassCastException if an element in the list cannot be cast to its expected type.
     * @throws IndexOutOfBoundsException if the list does not have enough elements.
     */
    fun List<Any?>.toFlight(): Flight {
        return Flight(
            id = this[0] as String,
            icao24 = this[0] as String,
            callsign = this[1] as? String,
            originCountry = this[2] as String,
            timePosition = (this[3] as? Number)?.toLong(),
            lastContact = (this[4] as Number).toLong(),
            longitude = (this[5] as? Number)?.toDouble(),
            latitude = (this[6] as? Number)?.toDouble(),
            baroAltitude = (this[7] as? Number)?.toDouble(),
            onGround = this[8] as Boolean,
            velocity = (this[9] as? Number)?.toDouble(),
            heading = (this[10] as? Number)?.toDouble(),
            verticalRate = (this[11] as? Number)?.toDouble(),
            sensors = (this[12] as? List<Int>),
            geoAltitude = (this[13] as? Number)?.toDouble(),
            squawk = this[14] as? String,
            spi = this[15] as Boolean,
            category = (this[16] as Number).toInt()
        )
    }
}
