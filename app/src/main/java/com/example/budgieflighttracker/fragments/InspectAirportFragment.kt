package com.example.budgieflighttracker.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.budgieflighttracker.FragmentListener
import com.example.budgieflighttracker.R
import com.example.budgieflighttracker.datasource.FlightAPI
import java.text.SimpleDateFormat
import java.util.*

class InspectAirportFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_inspect_airport, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleOnClickListeners()

        val bundle = arguments
        val airportInfo = bundle?.getSerializable("airportInfo") as? FlightAPI.AirportFlightInfo
        airportInfo?.let { setAirportData(it) }
    }

    private fun handleOnClickListeners() {
        val closeButton = requireView().findViewById<ImageView>(R.id.closeButton)

        closeButton.setOnClickListener {
            listener?.OnInspectFragmentClose()
            parentFragmentManager.popBackStack()
        }
    }

    private fun setAirportData(airportInfo: FlightAPI.AirportFlightInfo) {
        // Set airport name using proper lookup from airports file
        val airportNameTextView = requireView().findViewById<TextView>(R.id.airportNameTextView)
        airportNameTextView.text = getAirportName(airportInfo.airportIcao)

        // Set current time and date
        val airportTimeDateTextView = requireView().findViewById<TextView>(R.id.airportTimeDateTextView)
        val currentTime = SimpleDateFormat("HH:mm z EEE, dd MMM", Locale.getDefault()).format(Date())
        airportTimeDateTextView.text = currentTime

        // Set flight counts (using total flight count for both departures and arrivals for now)
        val departuresValueTextView = requireView().findViewById<TextView>(R.id.departuresValueTextView)
        val arrivalsValueTextView = requireView().findViewById<TextView>(R.id.arrivalsValueTextView)

        // Split the flight count roughly between departures and arrivals
        val totalFlights = airportInfo.flightCount
        val departures = (totalFlights * 0.5).toInt()
        val arrivals = totalFlights - departures

        departuresValueTextView.text = departures.toString()
        arrivalsValueTextView.text = arrivals.toString()

        // Set airport flag (placeholder for now)
        val airportFlagImageView = requireView().findViewById<ImageView>(R.id.airportFlagImageView)
        // TODO: Implement country flag lookup based on airport ICAO code
        airportFlagImageView.setImageResource(R.drawable.ic_google) // Placeholder
    }

    private fun getAirportName(icao: String): String {
        try {
            val inputStream = resources.openRawResource(R.raw.airports)
            val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
            reader.useLines { lines ->
                lines.drop(1).forEach { line -> // Skip header
                    val tokens = line.split(",")
                    if (tokens.size >= 5) {
                        val csvIcao = tokens[3].trim().replace("\"", "")
                        if (csvIcao.equals(icao, ignoreCase = true)) {
                            val airportName = tokens[4].trim().replace("\"", "")
                            return airportName
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Fallback if airport not found in file
        return "$icao Airport"
    }

    companion object {
        fun newInstance(airportInfo: FlightAPI.AirportFlightInfo): InspectAirportFragment {
            val fragment = InspectAirportFragment()
            val bundle = Bundle()
            bundle.putSerializable("airportInfo", airportInfo)
            fragment.arguments = bundle
            return fragment
        }
    }
}
