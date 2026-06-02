package com.example.budgieflighttracker.pages

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.budgieflighttracker.MapsActivity
import com.example.budgieflighttracker.R
import com.example.budgieflighttracker.notifications.NotificationPreferences
import com.example.budgieflighttracker.notifications.NotificationWorker

class AlertPage : AppCompatActivity() {

    private lateinit var notificationPreferences: NotificationPreferences
    private lateinit var notificationsSwitch: Switch
    private lateinit var radiusSpinner: Spinner
    private lateinit var closeButton: ImageView

    private lateinit var testAlertButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_page)

        // Initialize preferences
        notificationPreferences = NotificationPreferences(this)

        initializeViews()
        setupRadiusSpinner()
        loadSavedPreferences()
        setupListeners()

    }

    private fun initializeViews() {
        notificationsSwitch = findViewById(R.id.NotificationsSwitch)
        radiusSpinner = findViewById(R.id.RadiusSpinner)
        closeButton = findViewById(R.id.closeButton)
        testAlertButton = findViewById(R.id.testAlertButton)

    }

    private fun setupRadiusSpinner() {
        val radiusOptions = arrayOf("10 km", "50 km", "100 km")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, radiusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        radiusSpinner.adapter = adapter
    }

    private fun loadSavedPreferences() {
        // Load notification switch state
        notificationsSwitch.isChecked = notificationPreferences.areNotificationsEnabled()

        // Load radius selection
        val savedRadius = notificationPreferences.getAlertRadius()
        val radiusIndex = when (savedRadius) {
            10 -> 0
            50 -> 1
            100 -> 2
            else -> 0
        }
        radiusSpinner.setSelection(radiusIndex)
    }

    private fun setupListeners() {
        // Notifications switch listener
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            notificationPreferences.setNotificationsEnabled(isChecked)
        }

        // Radius spinner listener
        radiusSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val radius = when (position) {
                    0 -> 10
                    1 -> 50
                    2 -> 100
                    else -> 10
                }
                notificationPreferences.setAlertRadius(radius)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        // Close button listener
        closeButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }
        testAlertButton.setOnClickListener {
            val request = OneTimeWorkRequest.Builder(NotificationWorker::class.java).build()
            WorkManager.getInstance(this).enqueue(request)

            Toast.makeText(this, "Running flight check…", Toast.LENGTH_SHORT).show()
        }
    }
}
