package com.example.budgieflighttracker.fragments

import android.app.SearchManager
import android.content.Context
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.budgieflighttracker.OnFiltersSet
import com.example.budgieflighttracker.R
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * A simple [Fragment] subclass.
 * Use the [FiltersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FiltersFragment : Fragment() {

    private val destinationList = mutableListOf<Map<String, String>>()
    private lateinit var departureSuggestionsAdapter: SimpleCursorAdapter
    private lateinit var arrivalSuggestionsAdapter: SimpleCursorAdapter

    // Store selected filter values
    private var selectedCountry: String? = null
    private var selectedAircraftType: String? = null
    private var selectedDepartureIcao: String? = null
    private var selectedArrivalIcao: String? = null

    private val SUGGESTION_COLUMNS = arrayOf(
        BaseColumns._ID,
        SearchManager.SUGGEST_COLUMN_TEXT_1, // Visible (Airport name)
        SearchManager.SUGGEST_COLUMN_INTENT_DATA // Hidden (ICAO code)
    )

    lateinit var dataPasser: OnFiltersSet

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        super.onAttach(context)
        dataPasser = context as OnFiltersSet
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_filters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCSVData()
        handleOnClickListeners()
    }

    private fun handleOnClickListeners() {
        // Country Filter
        //==============================================================================
        val countryFilter = requireView().findViewById<LinearLayout>(R.id.countryFilter)
        countryFilter?.setOnClickListener {
            if (isAdded) {
                val countrySpinner: Spinner = requireView().findViewById(R.id.countrySpinner)

                countrySpinner.visibility = View.VISIBLE

                // Load countries from arrays.xml
                val countries = resources.getStringArray(R.array.countries_list)

                // Create an adapter for the spinner
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    countries
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                countrySpinner.adapter = adapter

                // Handle selection events
                countrySpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            selectedCountry = countries[position]
                            Toast.makeText(
                                requireContext(),
                                "Selected: $selectedCountry",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            selectedCountry = null
                        }
                    }
            }
        }

        // Aircraft Type Filter
        //==============================================================================
        val aircraftTypeFilter =
            requireView().findViewById<LinearLayout>(R.id.aircraftTypeFilter)
        aircraftTypeFilter.setOnClickListener {
            if (isAdded) {
                val aircraftSpinner: Spinner = requireView().findViewById(R.id.aircraftSpinner)

                aircraftSpinner.visibility = View.VISIBLE

                // Load aircraft types from arrays.xml
                val aircraftTypes = resources.getStringArray(R.array.aircraft_list)

                // Create an adapter for the spinner
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    aircraftTypes
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                aircraftSpinner.adapter = adapter

                // Handle selection events
                aircraftSpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            selectedAircraftType = aircraftTypes[position]
                            Toast.makeText(
                                requireContext(),
                                "Selected: $selectedAircraftType",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            selectedAircraftType = null
                        }
                    }
            }
        }

        // Departure Destination Filter
        //==============================================================================
        val departureDestinationFilter =
            requireView().findViewById<LinearLayout>(R.id.departureDestinationFilter)
        departureDestinationFilter.setOnClickListener {
            if (isAdded) {
                val departureTextView = requireView().findViewById<TextView>(R.id.depatureTextView)
                departureTextView.visibility = GONE
                val departureSearchView =
                    requireView().findViewById<SearchView>(R.id.departureSearchView)
                departureSearchView.visibility = View.VISIBLE

                setupDepartureSearchView(departureSearchView)
            }
        }

        // Arrival Destination Filter
        //==============================================================================
        val arrivalDestinationFilter =
            requireView().findViewById<LinearLayout>(R.id.arrivalDestinationFilter)
        arrivalDestinationFilter.setOnClickListener {
            if (isAdded) {
                val arrivalTextView = requireView().findViewById<TextView>(R.id.arrivalTextView)
                arrivalTextView.visibility = GONE
                val arrivalSearchView =
                    requireView().findViewById<SearchView>(R.id.arrivalSearchView)
                arrivalSearchView.visibility = View.VISIBLE

                setupArrivalSearchView(arrivalSearchView)
            }
        }

        // Choice Buttons
        //==============================================================================
        val cancelButton = requireView().findViewById<Button>(R.id.cancelButton)
        val closeButton = requireView().findViewById<ImageView>(R.id.closeButton)
        val saveButton = requireView().findViewById<Button>(R.id.saveButton)

        // Cancel button - reset all filters and close fragment
        cancelButton.setOnClickListener {
            resetFilters()
            Toast.makeText(requireContext(), "Filters cancelled", Toast.LENGTH_SHORT).show()
            // Close the fragment - you may need to adjust this based on your navigation setup
            parentFragmentManager.popBackStack()
        }

        // Close button - same as cancel
        closeButton.setOnClickListener {
            resetFilters()
            parentFragmentManager.popBackStack()
        }

        // Save button - apply the selected filters
        saveButton.setOnClickListener {
            applyFilters()
        }
    }

    private fun setupDepartureSearchView(departureSearchView: SearchView) {
        // Configure SimpleCursorAdapter
        departureSuggestionsAdapter = SimpleCursorAdapter(
            requireContext(),
            R.layout.search_dropdown_item, // Layout for each suggestion
            null, // Cursor will be swapped dynamically
            arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1), // Columns to display
            intArrayOf(android.R.id.text1),
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )

        departureSearchView.suggestionsAdapter = departureSuggestionsAdapter

        // Listen for text changes
        departureSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                selectedDepartureIcao = query
                Toast.makeText(requireContext(), "Departure: $query", Toast.LENGTH_SHORT).show()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                updateDepartureSuggestions(newText ?: "")
                return true
            }
        })

        // Handle suggestion clicks
        departureSearchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean = false

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = departureSuggestionsAdapter.cursor
                if (cursor.moveToPosition(position)) {
                    // Get the hidden ICAO code
                    selectedDepartureIcao =
                        cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_INTENT_DATA))

                    // Put ICAO into the SearchView instead of airport name
                    departureSearchView.setQuery(selectedDepartureIcao, false)

                    Toast.makeText(
                        requireContext(),
                        "Selected Departure ICAO: $selectedDepartureIcao",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
        })
    }

    private fun setupArrivalSearchView(arrivalSearchView: SearchView) {
        // Configure SimpleCursorAdapter
        arrivalSuggestionsAdapter = SimpleCursorAdapter(
            requireContext(),
            R.layout.search_dropdown_item, // Layout for each suggestion
            null, // Cursor will be swapped dynamically
            arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1), // Columns to display
            intArrayOf(android.R.id.text1),
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )

        arrivalSearchView.suggestionsAdapter = arrivalSuggestionsAdapter

        // Listen for text changes
        arrivalSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                selectedArrivalIcao = query
                Toast.makeText(requireContext(), "Arrival: $query", Toast.LENGTH_SHORT).show()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                updateArrivalSuggestions(newText ?: "")
                return true
            }
        })

        // Handle suggestion clicks
        arrivalSearchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean = false

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = arrivalSuggestionsAdapter.cursor
                if (cursor.moveToPosition(position)) {
                    // Get the hidden ICAO code
                    selectedArrivalIcao =
                        cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_INTENT_DATA))

                    // Put ICAO into the SearchView instead of airport name
                    arrivalSearchView.setQuery(selectedArrivalIcao, false)

                    Toast.makeText(
                        requireContext(),
                        "Selected Arrival ICAO: $selectedArrivalIcao",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
        })
    }

    private fun loadCSVData() {
        val inputStream = resources.openRawResource(R.raw.airports)
        val reader = BufferedReader(InputStreamReader(inputStream))

        reader.useLines { lines ->
            lines.drop(1).forEach { line -> // Skip header
                val tokens = line.split(",")

                if (tokens.size >= 7) {
                    val airportName = tokens[4].trim().replace("\"", "")
                    val icao = tokens[3].trim().replace("\"", "")

                    if (airportName.isNotEmpty() && icao.isNotEmpty()) {
                        destinationList.add(
                            mapOf(
                                "airport" to airportName,
                                "icao" to icao
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Dynamically update departure suggestions by airport name
     */
    private fun updateDepartureSuggestions(query: String) {
        val matrixCursor = MatrixCursor(SUGGESTION_COLUMNS)

        if (query.isNotEmpty()) {
            val filtered = destinationList.filter {
                it["airport"]!!.contains(query, ignoreCase = true)
            }

            filtered.forEachIndexed { index, airport ->
                matrixCursor.addRow(
                    arrayOf(
                        index,                // _ID
                        airport["airport"],   // Visible name
                        airport["icao"]       // Hidden ICAO
                    )
                )
            }
        }

        departureSuggestionsAdapter.changeCursor(matrixCursor)
    }

    /**
     * Dynamically update arrival suggestions by airport name
     */
    private fun updateArrivalSuggestions(query: String) {
        val matrixCursor = MatrixCursor(SUGGESTION_COLUMNS)

        if (query.isNotEmpty()) {
            val filtered = destinationList.filter {
                it["airport"]!!.contains(query, ignoreCase = true)
            }

            filtered.forEachIndexed { index, airport ->
                matrixCursor.addRow(
                    arrayOf(
                        index,                // _ID
                        airport["airport"],   // Visible name
                        airport["icao"]       // Hidden ICAO
                    )
                )
            }
        }

        arrivalSuggestionsAdapter.changeCursor(matrixCursor)
    }

    /**
     * Reset all filter selections
     */
    private fun resetFilters() {
        selectedCountry = null
        selectedAircraftType = null
        selectedDepartureIcao = null
        selectedArrivalIcao = null

        // Hide and reset UI elements
        val countrySpinner = requireView().findViewById<Spinner>(R.id.countrySpinner)
        val aircraftSpinner = requireView().findViewById<Spinner>(R.id.aircraftSpinner)
        val departureSearchView = requireView().findViewById<SearchView>(R.id.departureSearchView)
        val arrivalSearchView = requireView().findViewById<SearchView>(R.id.arrivalSearchView)
        val departureTextView = requireView().findViewById<TextView>(R.id.depatureTextView)
        val arrivalTextView = requireView().findViewById<TextView>(R.id.arrivalTextView)

        countrySpinner.visibility = GONE
        aircraftSpinner.visibility = GONE
        departureSearchView.visibility = GONE
        arrivalSearchView.visibility = GONE
        departureTextView.visibility = View.VISIBLE
        arrivalTextView.visibility = View.VISIBLE

        // Clear search views
        departureSearchView.setQuery("", false)
        arrivalSearchView.setQuery("", false)
        applyFilters()
    }

    /**
     * Apply the selected filters
     * TODO: Implement the actual filtering logic based on your app's requirements
     */
    private fun applyFilters() {
        dataPasser.onFiltersSet(selectedCountry, selectedAircraftType, selectedDepartureIcao, selectedArrivalIcao)

        // Close the fragment after applying filters
        parentFragmentManager.popBackStack()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        @JvmStatic
        fun newInstance() = FiltersFragment()
    }
}