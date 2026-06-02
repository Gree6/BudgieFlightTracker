package com.example.budgieflighttracker.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.budgieflighttracker.FragmentListener
import com.example.budgieflighttracker.R
import com.example.budgieflighttracker.datasource.FlightAPI
import com.example.budgieflighttracker.models.Flight


class InspectFragment : Fragment() {

    private var listener: FragmentListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement FragmentListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_inspect, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleOnClickListeners()

        val b = arguments
        val flight = b?.getSerializable("flight") as Flight
        Log.i("InspectFragment", "Received flight data for ${flight.callsign}")
        setFlightData(flight)

    }

    private fun handleOnClickListeners() {
        val cancelButton = requireView().findViewById<ImageView>(R.id.closeButton)

        cancelButton.setOnClickListener {
            listener?.OnInspectFragmentClose()
            parentFragmentManager.popBackStack()
        }
    }

    private fun setFlightData(flight: Flight){
        Log.i("InspectFragment", "Setting flight data for ${flight.callsign}, ICAO24: ${flight.icao24}")
        val callsign = requireView().findViewById<TextView>(R.id.flightCallsignTextView)
        callsign.text = flight.callsign

        val category = requireView().findViewById<TextView>(R.id.airlineAircraftTextView)
        val aircraftTypes = resources.getStringArray(R.array.aircraft_list)
        val aircraftType = aircraftTypes[flight.category]
        category.text = aircraftType

        val speed = requireView().findViewById<TextView>(R.id.speedValueTextView)
        speed.text = flight.velocity?.toInt().toString() + " kts"

        val altitude = requireView().findViewById<TextView>(R.id.altitudeValueTextView)
        altitude.text = flight.baroAltitude?.toInt().toString() + " ft"

        val course = requireView().findViewById<TextView>(R.id.courseValueTextView)
        course.text = flight.heading?.toInt().toString() + "°"

        // Track Flight and draw path on map
        Log.i("InspectFragment", "Starting to fetch flight path for ICAO24: ${flight.icao24}")
        Thread {
            try {
                val api = FlightAPI()
                Log.i("InspectFragment", "Calling returnTrackedFlight API...")
                val trackedFlight = api.returnTrackedFlight(flight.icao24)
                
                Log.i("InspectFragment", "API response received: $trackedFlight")
                
                if (trackedFlight != null) {
                    Log.i("InspectFragment", "TrackedFlight has ${trackedFlight.path.size} waypoints")
                    if (trackedFlight.path.isNotEmpty()) {
                        // Call the listener on the main thread to draw the flight path
                        requireActivity().runOnUiThread {
                            Log.i("InspectFragment", "Drawing flight path on map...")
                            listener?.drawFlightPath(trackedFlight)
                        }
                    } else {
                        Log.w("InspectFragment", "Flight path is empty")
                    }
                } else {
                    Log.w("InspectFragment", "TrackedFlight is null - API may have returned no data")
                }
            } catch (e: Exception) {
                Log.e("InspectFragment", "Error fetching flight path: ${e.message}", e)
                e.printStackTrace()
            }
        }.start()
    }

}