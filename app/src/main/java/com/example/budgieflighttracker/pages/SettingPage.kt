package com.example.budgieflighttracker.pages

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.budgieflighttracker.LocaleHelper
import com.example.budgieflighttracker.R
import com.example.budgieflighttracker.datasource.UserRemoteDataSource
import com.example.budgieflighttracker.repositories.UserRepository
import com.example.budgieflighttracker.MapsActivity

class SettingPage : AppCompatActivity() {

    private lateinit var userRepository: UserRepository

    // UI Components
    private lateinit var closeButton: ImageView
    private lateinit var showSateliteView: Switch

    private lateinit var pushNotificationsSwitch: Switch
    private lateinit var enableAfrikaansSwitch: Switch
    private lateinit var colourSpinner: Spinner
    private lateinit var deleteAccountButton: Button
    private lateinit var logOutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_page)

        // Initialize repository
        val userRemoteDataSource = UserRemoteDataSource()
        userRepository = UserRepository(userRemoteDataSource)

        // Initialize UI components
        initializeViews()

        // Load saved preferences
        loadPreferences()

        // Set up click listeners
        setupClickListeners()

        // Satellite Code
        val satelliteView = findViewById<Switch>(R.id.SatelliteView)
        val sharedPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val isSatellite = sharedPrefs.getBoolean("satelliteView", false)
        satelliteView.isChecked = isSatellite
        //checks the switch for when the user clicks it


        satelliteView.setOnCheckedChangeListener { _, isChecked ->
//            Toast.makeText(this, "Satellite View: $isChecked", Toast.LENGTH_SHORT).show()
            sharedPrefs.edit {
                putBoolean("satelliteView", isChecked)
            }

        }

        //using spinner to change the colour of plane icons
        val colors = listOf(getString(R.string.Black), getString(R.string.Red), getString(R.string.Yellow), getString(R.string.Blue), getString(R.string.Purple))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colourSpinner.adapter = adapter
        val savedColor = sharedPrefs.getString("plane_color", "Black")
        colourSpinner.setSelection(colors.indexOf(savedColor))

        colourSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>,
                view: android.view.View,
                position: Int,
                id: Long
            ) {
                val selectedColor = colors[position]
                sharedPrefs.edit {
                    putString("plane_color", selectedColor)
                }
    //                Toast.makeText(this@SettingPage, "Plane color set to $selectedColor", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }


    }

    private fun initializeViews() {
        closeButton = findViewById(R.id.closeButton)
        showSateliteView = findViewById(R.id.SatelliteView)
        colourSpinner = findViewById<Spinner>(R.id.colSpinner)
        pushNotificationsSwitch = findViewById(R.id.pushNotificationsSwitch)
        enableAfrikaansSwitch = findViewById(R.id.enableAfrikaansSwitch)
        deleteAccountButton = findViewById(R.id.deleteAccountButton)
        logOutButton = findViewById(R.id.logOutButton)
    }

    private fun setupClickListeners() {
        // Close button
        closeButton.setOnClickListener {
            finish()
        }

        // Switch listeners - save preferences when changed
        showSateliteView.setOnCheckedChangeListener { _, isChecked ->
            savePreference("show_satellite", isChecked)
        }

        pushNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Android 13+ (API 33) requires POST_NOTIFICATIONS permission
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
                    } else {
                        savePreference("push_notifications", true)
                    }
                } else {
                    savePreference("push_notifications", true)
                }
            } else {
                savePreference("push_notifications", false)
            }
        }

        enableAfrikaansSwitch.setOnCheckedChangeListener { _, isChecked ->
            savePreference("enable_afrikaans", isChecked)
            if (isChecked) {
                LocaleHelper.setLocale(this, "af")
            } else {
                LocaleHelper.setLocale(this, "en")
            }
        }

        // Log out button
        logOutButton.setOnClickListener {
            performLogout()
        }

        // Delete account button
        deleteAccountButton.setOnClickListener {
            showDeleteAccountDialog()
        }
        onClickHandlers()
    }
    private fun onClickHandlers(){
        findViewById<ImageView>(R.id.closeButton).setOnClickListener {

            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
//            Toast.makeText(this, "Back to Home Page", Toast.LENGTH_SHORT).show()
        }
    }
    private fun loadPreferences() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)

        showSateliteView.isChecked = sharedPref.getBoolean("show_satellite", true)

        pushNotificationsSwitch.isChecked = sharedPref.getBoolean("push_notifications", true)
        enableAfrikaansSwitch.isChecked = sharedPref.getBoolean("enable_afrikaans", false)

        val isAfrikaansEnabled = sharedPref.getString("app_language", "en") == "af"
        enableAfrikaansSwitch.isChecked = isAfrikaansEnabled
    }

    private fun savePreference(key: String, value: Boolean) {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(key, value)
            apply()
        }
    }

    private fun performLogout() {
        // Show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                // Sign out from Firebase
                userRepository.signOut()

                // Clear saved preferences
                clearSavedPreferences()

                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

                // Navigate to login page
                val intent = Intent(this, LoginPage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone and all your data will be lost.")
            .setPositiveButton("Delete") { _, _ ->
                confirmDeleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteAccount() {
        // Show loading state
        deleteAccountButton.isEnabled = false
        deleteAccountButton.text = getString(R.string.deleting)

        // Delete account through repository
        userRepository.deleteAccount(
            onSuccess = {
                runOnUiThread {
                    // Clear all saved preferences
                    clearSavedPreferences()

                    Toast.makeText(
                        this,
                        "Account deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to login page
                    val intent = Intent(this, LoginPage::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            },
            onFailure = { exception ->
                runOnUiThread {
                    deleteAccountButton.isEnabled = true
                    deleteAccountButton.text = getString(R.string.delete_account)

                    val errorMessage = when {
                        exception.message?.contains("recent login") == true ->
                            "Please log in again before deleting your account for security reasons."
                        else -> "Failed to delete account: ${exception.message}"
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun clearSavedPreferences() {
        // Clear app settings
        val appSettings = getSharedPreferences("app_settings", MODE_PRIVATE)
        appSettings.edit().clear().apply()

        // Clear login preferences
        val loginPrefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
        loginPrefs.edit().clear().apply()
    }

    // Handle notification permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                savePreference("push_notifications", true)
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                pushNotificationsSwitch.isChecked = false
                savePreference("push_notifications", false)
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}