package com.example.budgieflighttracker.pages

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import com.example.budgieflighttracker.R
import com.example.budgieflighttracker.datasource.UserRemoteDataSource
import com.example.budgieflighttracker.repositories.UserRepository
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.Executor
import java.util.regex.Pattern
import kotlin.or

class RegistrationPage : AppCompatActivity() {

    private lateinit var userRepository: UserRepository

    // UI Components
    private lateinit var backButton: ImageView
    private lateinit var loginLink: TextView
    private lateinit var fullNameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var signUpButton: Button


    //biometric components
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: PromptInfo




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_page)

        // Initialize repository with Firebase Auth
        val userRemoteDataSource = UserRemoteDataSource()
        userRepository = UserRepository(userRemoteDataSource)

        // Initialize UI components
        initializeViews()

        // Set up click listeners
        setupClickListeners()


        //initialize Biometrics
        initializeBiometrics()
    }

    private fun initializeBiometrics() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(applicationContext, "Biometric authentication successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@RegistrationPage, LoginPage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                promptInfo = PromptInfo.Builder()
                    .setTitle("Device Approval")
                    .setSubtitle("Please approve that this is your device using Biometric authentication")
                    .setNegativeButtonText("Cancel")
                    .build()
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(applicationContext, "No biometric features available on this device", Toast.LENGTH_LONG).show()
                navigateToLogin()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(applicationContext, "Biometric features are currently unavailable", Toast.LENGTH_LONG).show()
                navigateToLogin()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No biometrics enrolled. Please set up a fingerprint or face unlock in your device settings.", Toast.LENGTH_LONG).show()
                navigateToLogin()
            }
            else -> {
                Toast.makeText(this, "An unknown error occurred.", Toast.LENGTH_LONG).show()
                navigateToLogin()
            }
        }
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        loginLink = findViewById(R.id.loginLink)
        fullNameEditText = findViewById(R.id.fullNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        signUpButton = findViewById(R.id.signUpButton)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupClickListeners() {
        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Login link - navigate to login page
        loginLink.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
            finish()
        }

        // Sign up button
        signUpButton.setOnClickListener {
            performRegistration()
        }
    }

    private fun performRegistration() {
        // Get input values
        val fullName = fullNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        // Validate inputs
        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return
        }

        // Disable button to prevent multiple submissions
        signUpButton.isEnabled = false
        signUpButton.text = "Creating account..."

        // Register user with Firebase Auth
        userRepository.registerWithEmail(
            email = email,
            password = password,
            fullName = fullName,
            onSuccess = { userId ->
                // Registration successful
                runOnUiThread {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                    // Navigate to main app (MapsActivity or LoginPage) after biometrics
                   showBiometricPrompt()
                }
            },
            onFailure = { exception ->
                // Registration failed
                runOnUiThread {
                    signUpButton.isEnabled = true
                    signUpButton.text = "Sign up"

                    val errorMessage = when {
                        exception.message?.contains("email address is already in use") == true ->
                            "This email is already registered. Please login instead."
                        exception.message?.contains("network error") == true ->
                            "Network error. Please check your connection."
                        exception.message?.contains("password") == true ->
                            "Password must be at least 6 characters."
                        else -> "Registration failed: ${exception.message}"
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun validateInputs(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        // Validate full name
        if (fullName.isEmpty()) {
            fullNameEditText.error = "Full name is required"
            fullNameEditText.requestFocus()
            return false
        }

        if (fullName.length < 2) {
            fullNameEditText.error = "Full name must be at least 2 characters"
            fullNameEditText.requestFocus()
            return false
        }

        // Validate email
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return false
        }

        if (!isValidEmail(email)) {
            emailEditText.error = "Please enter a valid email address"
            emailEditText.requestFocus()
            return false
        }

        // Validate password
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return false
        }

        if (password.length < 6) {
            passwordEditText.error = "Password must be at least 6 characters"
            passwordEditText.requestFocus()
            return false
        }

        // Validate password confirmation
        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.error = "Please confirm your password"
            confirmPasswordEditText.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            confirmPasswordEditText.requestFocus()
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return Pattern.compile(emailPattern).matcher(email).matches()
    }
}