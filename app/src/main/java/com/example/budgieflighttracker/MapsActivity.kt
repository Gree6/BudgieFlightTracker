package com.example.budgieflighttracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.CursorAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import com.example.budgieflighttracker.databinding.ActivityMapsBinding
import com.example.budgieflighttracker.datasource.FlightAPI
import com.example.budgieflighttracker.fragments.FiltersFragment
import com.example.budgieflighttracker.fragments.InspectAirportFragment
import com.example.budgieflighttracker.fragments.InspectFragment
import com.example.budgieflighttracker.fragments.PlaybackFragment
import com.example.budgieflighttracker.models.Flight
import com.example.budgieflighttracker.models.TrackedFlight
import com.example.budgieflighttracker.pages.AlertPage
import com.example.budgieflighttracker.pages.SettingPage
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.AdvancedMarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.budgieflighttracker.notifications.NotificationPreferences
import com.example.budgieflighttracker.notifications.NotificationWorker
import java.util.concurrent.TimeUnit

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnFiltersSet, FragmentListener {

    private var inspectFragmentOpen = false
    private var playbackFragmentOpen = false
    private var playbackMode = false // Track if we're in playback mode to disable API calls

    private var flights: List<Flight>? = null
    private var filteredFlights: List<Flight>? = null

    private lateinit var searchSuggestionAdapter: SimpleCursorAdapter
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val notificationsGranted =
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true

        if (locationGranted) {
            Log.d("Permissions", "Location permission granted")
            getLocation()
        } else {
            Log.d("Permissions", "Location permission denied")
        }

        if (notificationsGranted) {
            Log.d("Permissions", "Notification permission granted")
            // FCM SDK (or your app) can post notifications
        } else {
            Log.d("Permissions", "Notification permission denied")
        }
    }
    // Define search cursor columns
    companion object {
        const val COLUMN_ID = BaseColumns._ID
        const val COLUMN_FLIGHT_ID = "flight_id"
        const val COLUMN_ICAO24 = "icao24"
        const val COLUMN_CALLSIGN = "callsign"
        const val COLUMN_ORIGIN_COUNTRY = "origin_country"
        const val COLUMN_VELOCITY = "velocity"
        const val COLUMN_ALTITUDE = "altitude"
        const val COLUMN_ON_GROUND = "on_ground"
        const val COLUMN_SQUAWK = "squawk"

        // Performance optimization: Maximum markers to display at once
        const val MAX_MARKERS_ZOOMED_OUT = 700  // When zoom < 6
        const val MAX_MARKERS_MEDIUM_ZOOM = 500 // When zoom 6-9
        const val MAX_MARKERS_ZOOMED_IN = 200   // When zoom >= 10
    }

    private val cursorColumns = arrayOf(
        COLUMN_ID,
        COLUMN_FLIGHT_ID,
        COLUMN_ICAO24,
        COLUMN_CALLSIGN,
        COLUMN_ORIGIN_COUNTRY,
        COLUMN_VELOCITY,
        COLUMN_ALTITUDE,
        COLUMN_ON_GROUND,
        COLUMN_SQUAWK
    )

    private var selectedSearchItem: String? = null

    private var selectedArrivalIcao: String? = null
    private var selectedDepartureIcao: String? = null
    private var selectedAircraftType: String? = null
    private var selectedCountry: String? = null

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    var apiFilter = false

    private var lastApiCallTime: Long = 1000L
    private val apiCallDelay: Long = 6000L // 6 second
    private val handler = Handler(Looper.getMainLooper())
    private var pendingApiCall: Runnable? = null

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private var currentFlightPathPolyline: Polyline? = null // Keep track of the current flight path
    private var currentTrackedFlight: TrackedFlight? = null // Store the current tracked flight data

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val searchView = findViewById<SearchView>(R.id.searchEditText)
        val searchEditTextId = searchView.context.resources.getIdentifier("android:id/search_src_text", null, null)
        if (searchEditTextId != 0) {
            val searchEditText = searchView.findViewById<EditText>(searchEditTextId)
            if (searchEditText != null) {
                searchEditText.setTextColor(ContextCompat.getColor(this, R.color.black))
                searchEditText.setHintTextColor(ContextCompat.getColor(this, R.color.grey_text_dark_borders))
            }
        }

        // Set up UI click handlers
        setupClickHandlers()

        requestAllPermissions()
        startFlightChecker()

        // Network monitoring: show popup when offline
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                runOnUiThread {
                    showOfflineDialog()
                }
            }

            override fun onAvailable(network: Network) {
                runOnUiThread {
                    // If playback fragment is open, keep it until user closes
                }
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Listen for backstack changes to restore UI when playback fragment is closed
        supportFragmentManager.addOnBackStackChangedListener {
            val frag = supportFragmentManager.findFragmentByTag("playback")
            if (playbackFragmentOpen && frag == null) {
                playbackFragmentOpen = false
                playbackMode = false // Exit playback mode - re-enable API calls
                // Hide playback container
                val playbackContainer = findViewById<FrameLayout>(R.id.playbackFragmentContainerView)
                playbackContainer.visibility = INVISIBLE
                // Show top bar and bottom nav again
                val tb = findViewById<ConstraintLayout>(R.id.topBar)
                tb.visibility = VISIBLE
                val bn = findViewById<LinearLayout>(R.id.bottomNavigation)
                bn.visibility = VISIBLE
            }
        }
    }

    private fun showOfflineDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Offline")
        builder.setMessage("Internet connection lost. You can view previously cached flights in Playback mode.")
        builder.setPositiveButton("Open Playback") { _, _ ->
            openPlaybackFragment()
        }
        builder.setNegativeButton("Dismiss") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun openPlaybackFragment() {
        if (playbackFragmentOpen) return
        playbackFragmentOpen = true
        playbackMode = true // Enter playback mode - disable API calls

        // Make the playback fragment container visible
        val fragmentContainer = findViewById<FrameLayout>(R.id.playbackFragmentContainerView)
        fragmentContainer.visibility = VISIBLE

        val pf = PlaybackFragment()

        // Set callback to update map with playback flights
        pf.setOnFlightsUpdateCallback { flights ->
            runOnUiThread {
                // Display these cached flights on the map
                addPlaneMarkers(flights)
            }
        }

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .addToBackStack(null)
            .add(R.id.playbackFragmentContainerView, pf, "playback")
            .commit()
    }

    private fun setupClickHandlers() {
        // Filter button
        //==============================================================================

        findViewById<ImageView>(R.id.filterView).setOnClickListener {

            val ffv = findViewById<FrameLayout>(R.id.filterFragmentContainerView)

            ffv.visibility = VISIBLE


            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .addToBackStack(null)
                .add(R.id.filterFragmentContainerView, FiltersFragment())
                .commit()

        }
        //===============================================================================

        // Location FAB
        //===============================================================================
        findViewById<FloatingActionButton>(R.id.locationFab).setOnClickListener {

            //Checks user has approved location permission
            requestLocationPermission()
            //will then move to get latitude and longiture from getLocation() function
//            Toast.makeText(this, "Centering on your location", Toast.LENGTH_SHORT).show()
        }
        //===============================================================================

        // Bottom navigation buttons
        //===============================================================================
        findViewById<LinearLayout>(R.id.playbackButton).setOnClickListener {
            // Open PlaybackFragment
            openPlaybackFragment()
        }

        findViewById<LinearLayout>(R.id.alertsButton).setOnClickListener {
//            Toast.makeText(this, "Opening Alerts", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AlertPage::class.java)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.settingsButton).setOnClickListener {
//            Toast.makeText(this, "Opening Settings", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SettingPage::class.java)
            startActivity(intent)
        }
        //===============================================================================

        // Search functionality
        //===============================================================================
        val searchEditText = findViewById<SearchView>(R.id.searchEditText)
        setupSearchView(searchEditText)
        //===============================================================================
    }

    /**
     * Map Code
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val cape = LatLng(-33.92, 18.42)
        mMap.setMinZoomPreference(3f)
        mMap.addMarker(MarkerOptions().position(cape).title("Marker in Cape Town"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(cape))

        // Call API
        mMap.setOnCameraIdleListener {
            scheduleApiCall()
        }

        val sharedPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val isSatellite = sharedPrefs.getBoolean("satelliteView", false)

        mMap.mapType = if (isSatellite) {
            GoogleMap.MAP_TYPE_SATELLITE
        } else {
            GoogleMap.MAP_TYPE_NORMAL
        }

        // Marker OnClick
        mMap.setOnMarkerClickListener { marker ->
            selectedSearchItem = marker.title

            // Don't allow opening inspect fragment in playback mode
            if (playbackMode) {
                Toast.makeText(this, "Flight inspection not available in playback mode", Toast.LENGTH_SHORT).show()
                return@setOnMarkerClickListener true
            }

            if (!flights.isNullOrEmpty()) {
                val f = flights?.filter { it.callsign == selectedSearchItem }?.get(0)

                if (f != null) {
                    openInspectFragment(f)
                }
            }
            true
        }
    }

    private fun addPlaneMarkers(flights: List<Flight>?) {
        // Clear markers but preserve the flight path polyline
        mMap.clear()

        val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val selectedColorName = prefs.getString("plane_color", "Blue")

        // Determine max markers based on current zoom level
        val currentZoom = mMap.cameraPosition.zoom
        val maxMarkers = when {
            currentZoom < 6f -> MAX_MARKERS_ZOOMED_OUT
            currentZoom < 10f -> MAX_MARKERS_MEDIUM_ZOOM
            else -> MAX_MARKERS_ZOOMED_IN
        }

        val planeIconRes = when (selectedColorName) {
            "Red" -> R.drawable.plane_icon_red
            "Yellow" -> R.drawable.plane_icon_yellow
            "Purple" -> R.drawable.plane_icon_purple
            "Blue" -> R.drawable.plane_icon_blue
            else -> R.drawable.plane_icon
        }
        val planeIconDescriptor = BitmapDescriptorFactory.fromResource(planeIconRes)

        if (flights != null) {
            // Filter out flights without coordinates and sort by altitude (descending)
            // Higher altitude flights are typically more interesting
            val validFlights = flights
                .filter { it.latitude != null && it.longitude != null }
                .sortedByDescending { it.geoAltitude ?: it.baroAltitude ?: 0.0 }
                .take(maxMarkers) // Limit to max markers for performance

            var markerCount = 0
            for (flight in validFlights) {
                val lat = flight.latitude!!
                val lon = flight.longitude!!
                val position = LatLng(lat, lon)

                // Create a marker title with useful information
                val markerTitle =
                    flight.callsign?.takeIf { it.isNotBlank() } ?: "Unknown Flight"
                val markerSnippet = """
                    ICAO24: ${flight.icao24}
                    Origin: ${flight.originCountry}
                    Altitude: ${flight.geoAltitude ?: "N/A"} m
                    Velocity: ${flight.velocity ?: "N/A"} m/s
                    On Ground: ${if (flight.onGround) "Yes" else "No"}
                """.trimIndent()

                val rot: Float = (flight.heading ?: 0f).toFloat()

                // Add the marker to the map
                val markerOptions = AdvancedMarkerOptions()
                    .icon(planeIconDescriptor)
                    .position(position)
                    .title(markerTitle)
                    .snippet(markerSnippet)
                    .rotation(rot + 0.25f) // Rotate marker to match heading
                    .flat(true) // Makes the marker rotate smoothly with heading

                mMap.addMarker(markerOptions)
                markerCount++
            }

            // Log performance info
            Log.d("MapsActivity", "Displayed $markerCount out of ${flights.size} flights (zoom: $currentZoom, limit: $maxMarkers)")
        }

        // Redraw the flight path if we have one stored
        currentTrackedFlight?.let {
            Log.d("MapsActivity", "Redrawing flight path after map clear")
            redrawFlightPath(it)
        }
    }

    /**
     * Redraws the flight path without camera adjustment (used after map clear)
     */
    private fun redrawFlightPath(trackedFlight: TrackedFlight) {
        currentFlightPathPolyline?.remove()

        val pathPoints = trackedFlight.path.map { waypoint ->
            LatLng(waypoint.latitude.toDouble(), waypoint.longitude.toDouble())
        }

        if (pathPoints.isNotEmpty()) {
            val polylineOptions = PolylineOptions()
                .addAll(pathPoints)
                .color(0xFF2196F3.toInt()) // Blue color
                .width(8f)
                .geodesic(true)

            currentFlightPathPolyline = mMap.addPolyline(polylineOptions)
            Log.d("MapsActivity", "Flight path redrawn with ${pathPoints.size} waypoints")
        }
    }

    /**
     * API Code
     */
    private fun apiCallDefault() {
        // Get the current visible map bounds
        val bounds = mMap.projection.visibleRegion.latLngBounds

        // Extract the bounding box coordinates
        val lomin = bounds.southwest.longitude.toFloat() // Minimum longitude
        val lamin = bounds.southwest.latitude.toFloat()  // Minimum latitude
        val lomax = bounds.northeast.longitude.toFloat() // Maximum longitude
        val lamax = bounds.northeast.latitude.toFloat()  // Maximum latitude

        println("Map Bounds: lamin=$lamin, lomin=$lomin, lamax=$lamax, lomax=$lomax")

        // Make the API call using the bounding box
        val api = FlightAPI()
        val flights = api.returnFlights(lomin, lamin, lomax, lamax)
        this.flights = flights
        mMap.clear()
        runOnUiThread {
            addPlaneMarkers(flights)
        }

    }

    private fun apiFilterCall(
        selectedCountry: String?,
        selectedAircraftType: String?,
        selectedDepartureIcao: String?,
        selectedArrivalIcao: String?
    ) {

        if (selectedCountry.isNullOrEmpty() && selectedAircraftType.isNullOrEmpty() && selectedDepartureIcao.isNullOrEmpty() && selectedArrivalIcao.isNullOrEmpty()) {
//            Toast.makeText(this, "Filter Empty", Toast.LENGTH_SHORT).show()
            apiCallDefault()
            return
        }

        val api = FlightAPI()

        // Get the current visible map bounds
        val bounds = mMap.projection.visibleRegion.latLngBounds

        // Extract the bounding box coordinates
        val lomin = bounds.southwest.longitude.toFloat() // Minimum longitude
        val lamin = bounds.southwest.latitude.toFloat()  // Minimum latitude
        val lomax = bounds.northeast.longitude.toFloat() // Maximum longitude
        val lamax = bounds.northeast.latitude.toFloat()  // Maximum latitude


        if (!selectedCountry.isNullOrEmpty()) {
            val flights = api.returnFlightsByCountry(lomin, lamin, lomax, lamax, selectedCountry)
            this.flights = flights
            runOnUiThread {
                addPlaneMarkers(flights)
            }
            return
        }

        if (!selectedAircraftType.isNullOrEmpty()) {
            val flights = api.returnFlightsByAircraft(
                lomin,
                lamin,
                lomax,
                lamax,
                aircraftType = selectedAircraftType
            )
            this.flights = flights
            runOnUiThread {
                addPlaneMarkers(flights)
            }
            return
        }

        if (!selectedDepartureIcao.isNullOrEmpty()) {
            val info = api.returnFlightsByDepature(departureIcao = selectedDepartureIcao)
            runOnUiThread {
                // Display airport ICAO and flight count
                showAirportFlightInfo(info)
            }
            return
        }

        if (!selectedArrivalIcao.isNullOrEmpty()) {
            val info = api.returnFlightsByArrival(arrivalIcao = selectedArrivalIcao)
            runOnUiThread {
                // Display airport ICAO and flight count
                showAirportFlightInfo(info)
            }
            return
        }
    }

    // Preserve API Limits and system resources
    private fun scheduleApiCall() {
        // Don't make API calls when in playback mode or when inspect fragment is open
        if (playbackMode) {
            println("API CALL SKIPPED - In playback mode")
            return
        }

        if (inspectFragmentOpen) {
            println("API CALL SKIPPED - Inspect fragment is open")
            return
        }

        println("API CALL")

        // If a previous scheduled call exists
        pendingApiCall?.let { handler.removeCallbacks(it) }

        // Define the task
        pendingApiCall = Runnable {
            if (System.currentTimeMillis() - lastApiCallTime >= apiCallDelay) {
                apiFilterCall(
                    selectedCountry,
                    selectedAircraftType,
                    selectedDepartureIcao,
                    selectedArrivalIcao
                )
                lastApiCallTime = System.currentTimeMillis()
            }
        }

        // Delay execution slightly to allow for small camera movements to finish
        handler.postDelayed(pendingApiCall!!, 300) // 300ms debounce
    }

    /**
     * Search Code
     */

    private fun populateSuggestions(query: String) {
        val matrixCursor = MatrixCursor(cursorColumns)

        flights?.filter { flight ->
            flight.callsign?.contains(query, ignoreCase = true) == true
        }?.forEachIndexed { index, flight ->
            matrixCursor.addRow(
                arrayOf(
                    index,
                    flight.callsign,
                    flight.icao24,
                    flight.callsign,
                    flight.originCountry,
                    flight.velocity,
                    flight.geoAltitude,
                    flight.onGround,
                    flight.squawk
                )
            )
        }

        searchSuggestionAdapter.changeCursor(matrixCursor)
    }

    private fun setupSearchView(searchView: SearchView) {
        val from = arrayOf(COLUMN_CALLSIGN)

        val to = intArrayOf(android.R.id.text1)

        searchSuggestionAdapter = SimpleCursorAdapter(
            this,
            R.layout.search_dropdown_item,
            null,
            from,
            to,
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )

        searchView.suggestionsAdapter = searchSuggestionAdapter

        // Listen for text changes
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("SearchView", "Submitted query: $query")
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    populateSuggestions(newText)
                }
                return true
            }
        })

        // Handle suggestion clicks
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return true
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = searchSuggestionAdapter.getItem(position) as android.database.Cursor
                val callsign = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CALLSIGN))
                searchView.setQuery(callsign, true)

                val selectedFlight = flights?.find { it.callsign == callsign }
                if (selectedFlight?.latitude != null && selectedFlight.longitude != null) {
                    val flightPosition = LatLng(selectedFlight.latitude, selectedFlight.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(flightPosition, 10f))
                }

                searchView.clearFocus()
                return true
            }
        })
    }


    private fun updateSearchSuggestions(query: String) {
        filteredFlights = if (query.isEmpty()) {
            flights
        } else {
            flights?.filter { flight ->
                // Search in multiple fields
                val searchQuery = query.lowercase().trim()

                (flight.callsign?.lowercase()?.contains(searchQuery) == true) ||
                        (flight.icao24.lowercase().contains(searchQuery)) ||
                        (flight.originCountry.lowercase().contains(searchQuery)) ||
                        (flight.squawk?.lowercase()?.contains(searchQuery) == true) ||
                        (flight.id.lowercase().contains(searchQuery))
            }
        }

        createCursorFromFlights(filteredFlights)
    }

    private fun createCursorFromFlights(flights: List<Flight>?) {
        val cursor = MatrixCursor(cursorColumns)

        if (flights != null) {
            flights.forEachIndexed { index, flight ->
                val row = arrayOf(
                    index.toLong(), // _ID for cursor
                    flight.id,
                    flight.icao24,
                    flight.callsign ?: "",
                    flight.originCountry,
                    flight.velocity?.toString() ?: "0",
                    flight.baroAltitude?.toString() ?: "0",
                    if (flight.onGround) "1" else "0",
                    flight.squawk ?: ""
                )
                cursor.addRow(row)
            }
        }

        searchSuggestionAdapter.changeCursor(cursor)
    }

    /**
     * Location Code
     */

    private fun requestAllPermissions() {
        val permissionsList = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Only add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionsLauncher.launch(permissionsList.toTypedArray())
    }


    private fun requestLocationPermission() {
        val coarseLocationCheck =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val fineLocationCheck =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (coarseLocationCheck != PackageManager.PERMISSION_GRANTED &&
            fineLocationCheck != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 1
            )
        } else {
            getLocation()
            Log.d("location", "permission granted")
        }
    }

    @Suppress("MissingPermission")
    private fun getLocation() {
        if (::mMap.isInitialized) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        Log.d("location", "Lat: $latitude, Lng: $longitude")

                        // Save user location for notifications
                        val notificationPrefs = NotificationPreferences(this)
                        notificationPrefs.setUserLocation(latitude, longitude)

                        val currentLocation = LatLng(latitude, longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 10f))
                    } else {
                        Log.d("location", "Last location is null, requesting update...")
                        requestNewLocationData(fusedLocationClient)
                    }
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permissions granted → get location
            getLocation()
        } else {
            Log.d("location", "Permission denied")
        }
    }

    @Suppress("MissingPermission")
    private fun requestNewLocationData(fusedLocationClient: FusedLocationProviderClient) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        )
            .setWaitForAccurateLocation(true)
            .setMaxUpdates(1)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null) {
                        Log.d("location", "Updated Lat: ${location.latitude}, Lng: ${location.longitude}")

                        // Save user location for notifications
                        val notificationPrefs = NotificationPreferences(this@MapsActivity)
                        notificationPrefs.setUserLocation(location.latitude, location.longitude)
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                }
            },
            mainLooper
        )
    }

    /**
     * InspectFragment
     */
    private fun openInspectFragment(f: Flight) {
        println("Selected Flight: $f")

        if (inspectFragmentOpen){
            return
        }
        inspectFragmentOpen = true

        // Zoom in on Flight
        val lat = f.latitude
        val lon = f.longitude
        if (lat != null && lon != null) {
            val position = LatLng(lat, lon)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 10f))
        }

        // Pass along data
        val inspectObject = InspectFragment()
        val bundle = Bundle()
        bundle.putSerializable("flight", f)
        inspectObject.arguments = bundle

        // Hide other elements
        val tb = findViewById<ConstraintLayout>(R.id.topBar)
        tb.visibility = INVISIBLE
        val bn = findViewById<LinearLayout>(R.id.bottomNavigation)
        bn.visibility = INVISIBLE

        // Show Inspect Fragment
        val ifv = findViewById<FrameLayout>(R.id.inspectFragmentContainerView)
        ifv.visibility = VISIBLE

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.inspectFragmentContainerView, inspectObject)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Call Backs
     */
    override fun onFiltersSet(
        selectedCountry: String?,
        selectedAircraftType: String?,
        selectedDepartureIcao: String?,
        selectedArrivalIcao: String?
    ) {
        apiFilter = true

        this.selectedCountry = selectedCountry
        this.selectedAircraftType = selectedAircraftType
        this.selectedDepartureIcao = selectedDepartureIcao
        this.selectedArrivalIcao = selectedArrivalIcao
        scheduleApiCall()
    }

    override fun OnInspectFragmentClose() {
        inspectFragmentOpen = false

        currentFlightPathPolyline?.remove()
        currentFlightPathPolyline = null
        currentTrackedFlight = null

        // Zoom out
        mMap.animateCamera(CameraUpdateFactory.zoomOut())
        // Show other elements
        val tb = findViewById<ConstraintLayout>(R.id.topBar)
        tb.visibility = VISIBLE
        val bn = findViewById<LinearLayout>(R.id.bottomNavigation)
        bn.visibility = VISIBLE
    }

    // Look up airport coordinates by ICAO code
    private fun getAirportCoordinates(icao: String): LatLng? {
        try {
            val inputStream = resources.openRawResource(R.raw.airports)
            val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
            reader.useLines { lines ->
                lines.drop(1).forEach { line -> // Skip header
                    val tokens = line.split(",")
                    if (tokens.size >= 7) {
                        val csvIcao = tokens[3].trim().replace("\"", "")
                        if (csvIcao.equals(icao, ignoreCase = true)) {
                            val lat = tokens[5].trim().replace("\"", "").toDoubleOrNull()
                            val lon = tokens[6].trim().replace("\"", "").toDoubleOrNull()
                            if (lat != null && lon != null) {
                                return LatLng(lat, lon)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Add this function to display airport ICAO and flight count, and zoom to airport
    private fun showAirportFlightInfo(info: FlightAPI.AirportFlightInfo) {
        // Zoom to airport location
        val airportLatLng = getAirportCoordinates(info.airportIcao)
        if (airportLatLng != null && ::mMap.isInitialized) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(airportLatLng, 12f))
        }

        // Set flag to indicate fragment is open
        inspectFragmentOpen = true

        // Hide other UI elements
        val tb = findViewById<ConstraintLayout>(R.id.topBar)
        tb.visibility = INVISIBLE
        val bn = findViewById<LinearLayout>(R.id.bottomNavigation)
        bn.visibility = INVISIBLE

        // Show Airport Inspect Fragment
        val ifv = findViewById<FrameLayout>(R.id.inspectFragmentContainerView)
        ifv.visibility = VISIBLE

        val inspectAirportFragment = InspectAirportFragment.newInstance(info)

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.inspectFragmentContainerView, inspectAirportFragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Draws the flight path on the map using a polyline
     */
    override fun drawFlightPath(trackedFlight: TrackedFlight) {
        Log.d("MapsActivity", "drawFlightPath called with ${trackedFlight.path.size} waypoints")

        // Store the tracked flight so we can redraw it after map clears
        currentTrackedFlight = trackedFlight

        // Remove existing polyline if any
        currentFlightPathPolyline?.remove()

        // Create a list of LatLng points from the waypoints
        val pathPoints = trackedFlight.path.map { waypoint ->
            LatLng(waypoint.latitude.toDouble(), waypoint.longitude.toDouble())
        }

        Log.d("MapsActivity", "Converted to ${pathPoints.size} LatLng points")

        if (pathPoints.isNotEmpty()) {
            // Create polyline options with styling
            val polylineOptions = PolylineOptions()
                .addAll(pathPoints)
                .color(ContextCompat.getColor(this, R.color.primary))
                .width(8f)
                .geodesic(true) // Make the line follow the Earth's curvature

            // Draw the polyline on the map
            currentFlightPathPolyline = mMap.addPolyline(polylineOptions)

            Log.d("MapsActivity", "Flight path drawn successfully with ${pathPoints.size} waypoints")

            // Optionally zoom to show the entire path
            if (pathPoints.size > 1) {
                try {
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                    pathPoints.forEach { bounds.include(it) }
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
                    Log.d("MapsActivity", "Camera adjusted to show full flight path")
                } catch (e: Exception) {
                    Log.e("MapsActivity", "Error adjusting camera: ${e.message}")
                }
            }
        } else {
            Log.w("MapsActivity", "No path points to draw")
        }
    }

    private fun startFlightChecker() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "flight_checker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}

interface OnFiltersSet {
    fun onFiltersSet(
        selectedCountry: String?,
        selectedAircraftType: String?,
        selectedDepartureIcao: String?,
        selectedArrivalIcao: String?
    )
}

interface FragmentListener {
    fun OnInspectFragmentClose()
    fun drawFlightPath(flight: TrackedFlight)
}