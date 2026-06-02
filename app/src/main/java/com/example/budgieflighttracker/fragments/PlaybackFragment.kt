package com.example.budgieflighttracker.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.budgieflighttracker.R
import com.example.budgieflighttracker.datasource.FlightLocalDataSource
import com.example.budgieflighttracker.datasource.FlightRemoteDataSource
import com.example.budgieflighttracker.models.Flight
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple overlay fragment that shows a timeline/seekbar at the bottom of the map
 * to scrub through cached flight data over time.
 * Now loads data from Firestore flight_groups and caches it locally.
 */
class PlaybackFragment : Fragment() {

    private lateinit var local: FlightLocalDataSource
    private lateinit var remote: FlightRemoteDataSource
    private lateinit var timeSeekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var playPauseBtn: Button
    private lateinit var closeBtn: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private var playing = false
    private var cachedFlights: List<Pair<Long, List<Flight>>> = emptyList() // timestamp to flights
    private var onFlightsUpdateCallback: ((List<Flight>) -> Unit)? = null

    private val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_playback, container, false)

        timeSeekBar = view.findViewById(R.id.timeSeekBar)
        currentTimeText = view.findViewById(R.id.currentTimeText)
        playPauseBtn = view.findViewById(R.id.playPauseBtn)
        closeBtn = view.findViewById(R.id.closePlaybackBtn)

        local = FlightLocalDataSource(requireContext().applicationContext)
        remote = FlightRemoteDataSource()

        loadCachedFlightData()
        setupListeners()

        return view
    }

    fun setOnFlightsUpdateCallback(callback: (List<Flight>) -> Unit) {
        this.onFlightsUpdateCallback = callback
    }

    private fun loadCachedFlightData() {
        currentTimeText.text = getString(R.string.loading_flights_from_database)

        // First, try to load from Firestore flight_groups
        remote.getAvailableHours(
            onSuccess = { hours ->
                handler.post {
                    if (hours.isEmpty()) {
                        // If no Firestore data, fall back to local cache
                        loadFromLocalCache()
                        return@post
                    }

                    currentTimeText.text =
                        getString(R.string.loading_hours_from_database, hours.size)

                    // Load all available hours from Firestore
                    val flightsByTime = mutableListOf<Pair<Long, List<Flight>>>()
                    var loadedCount = 0

                    hours.forEach { hour ->
                        remote.getFlightsForHour(hour,
                            onSuccess = { flights ->
                                handler.post {
                                    if (flights.isNotEmpty()) {
                                        // Cache flights locally for offline use
                                        local.cacheFlightGroup(hour, flights)

                                        // Group flights by the hour
                                        val grouped = flights.groupBy { (it.lastContact / 3600) * 3600 }
                                        grouped.forEach { (timestamp, flightList) ->
                                            flightsByTime.add(Pair(timestamp, flightList))
                                        }
                                    }
                                    loadedCount++

                                    if (loadedCount == hours.size) {
                                        // All hours loaded, sort by timestamp
                                        cachedFlights = flightsByTime.sortedBy { it.first }

                                        if (cachedFlights.isNotEmpty()) {
                                            timeSeekBar.max = cachedFlights.size - 1
                                            timeSeekBar.progress = 0
                                            updateFlightsDisplay(0)
                                        } else {
                                            currentTimeText.text = getString(R.string.no_cached_flights)
                                            timeSeekBar.isEnabled = false
                                        }
                                    } else {
                                        currentTimeText.text = getString(
                                            R.string.loaded_hours,
                                            loadedCount,
                                            hours.size
                                        )
                                    }
                                }
                            },
                            onFailure = { _ ->
                                handler.post {
                                    loadedCount++
                                    if (loadedCount == hours.size) {
                                        // If all Firestore loads failed, fall back to local cache
                                        if (cachedFlights.isEmpty()) {
                                            loadFromLocalCache()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            },
            onFailure = { _ ->
                handler.post {
                    // If Firestore fails, fall back to local cache
                    currentTimeText.text =
                        getString(R.string.firestore_unavailable_loading_local_cache)
                    loadFromLocalCache()
                }
            }
        )
    }

    private fun loadFromLocalCache() {
        local.getAvailableHours(onSuccess = { hours ->
            handler.post {
                if (hours.isEmpty()) {
                    currentTimeText.text = getString(R.string.no_cached_flights)
                    timeSeekBar.isEnabled = false
                    return@post
                }

                currentTimeText.text =
                    getString(R.string.loading_hours_from_local_cache, hours.size)

                // Load all cached flight data grouped by timestamp
                val flightsByTime = mutableListOf<Pair<Long, List<Flight>>>()
                var loadedCount = 0

                hours.forEach { hour ->
                    local.getFlightsForHour(hour, onSuccess = { flights ->
                        handler.post {
                            if (flights.isNotEmpty()) {
                                // Group flights by their timestamp
                                val grouped = flights.groupBy { (it.lastContact / 3600) * 3600 }
                                grouped.forEach { (timestamp, flightList) ->
                                    flightsByTime.add(Pair(timestamp, flightList))
                                }
                            }
                            loadedCount++

                            if (loadedCount == hours.size) {
                                // All hours loaded, sort by timestamp
                                cachedFlights = flightsByTime.sortedBy { it.first }

                                if (cachedFlights.isNotEmpty()) {
                                    timeSeekBar.max = cachedFlights.size - 1
                                    timeSeekBar.progress = 0
                                    updateFlightsDisplay(0)
                                } else {
                                    currentTimeText.text = getString(R.string.no_cached_flights)
                                    timeSeekBar.isEnabled = false
                                }
                            } else {
                                currentTimeText.text = getString(
                                    R.string.loaded_hours_from_cache,
                                    loadedCount,
                                    hours.size
                                )
                            }
                        }
                    }, onFailure = { _ ->
                        handler.post {
                            loadedCount++
                            if (loadedCount == hours.size && cachedFlights.isEmpty()) {
                                currentTimeText.text = getString(R.string.no_cached_flights)
                                timeSeekBar.isEnabled = false
                            }
                        }
                    })
                }
            }
        }, onFailure = { _ ->
            handler.post {
                currentTimeText.text = getString(R.string.no_cached_flights)
                timeSeekBar.isEnabled = false
            }
        })
    }

    private fun setupListeners() {
        timeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateFlightsDisplay(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                playing = false
                playPauseBtn.text = getString(R.string.play)
                stopPlaybackLoop()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        playPauseBtn.setOnClickListener {
            playing = !playing
            playPauseBtn.text = if (playing) getString(R.string.pause) else getString(R.string.play)
            if (playing) startPlaybackLoop() else stopPlaybackLoop()
        }

        closeBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun updateFlightsDisplay(index: Int) {
        if (cachedFlights.isEmpty() || index < 0 || index >= cachedFlights.size) return

        val (timestamp, flights) = cachedFlights[index]
        val dateTime = Date(timestamp * 1000)
        currentTimeText.text = dateTimeFormatter.format(dateTime)
        // Notify the map to update with these flights
        onFlightsUpdateCallback?.invoke(flights)
    }

    private fun startPlaybackLoop() {
        handler.post(object : Runnable {
            override fun run() {
                if (!playing || cachedFlights.isEmpty()) return

                val currentProgress = timeSeekBar.progress
                if (currentProgress < timeSeekBar.max) {
                    timeSeekBar.progress = currentProgress + 1
                    updateFlightsDisplay(currentProgress + 1)
                    handler.postDelayed(this, 1000) // Update every second
                } else {
                    // Reached the end, stop playback
                    playing = false
                    playPauseBtn.text = getString(R.string.play)
                }
            }
        })
    }

    private fun stopPlaybackLoop() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPlaybackLoop()
        onFlightsUpdateCallback?.invoke(emptyList()) // Clear flights from map
    }
}
