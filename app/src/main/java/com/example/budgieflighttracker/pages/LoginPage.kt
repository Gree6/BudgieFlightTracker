package com.example.budgieflighttracker.pages

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.budgieflighttracker.MapsActivity
import com.example.budgieflighttracker.R
import com.example.budgieflighttracker.datasource.UserRemoteDataSource
import com.example.budgieflighttracker.repositories.UserRepository
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor


class LoginPage : AppCompatActivity() {

    private lateinit var userRepository: UserRepository
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    // UI Components
    private lateinit var signUpLink: TextView
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var rememberMeCheckbox: CheckBox
    private lateinit var forgotPasswordLink: TextView
    private lateinit var loginButton: Button
    private lateinit var googleSignInButton: Button
    private lateinit var fingerPrintSignInButton: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        // Initialize repository
        val userRemoteDataSource = UserRemoteDataSource()
        userRepository = UserRepository(userRemoteDataSource)

        // Initialize Google Sign In
        initializeGoogleSignIn()

        // Initialize UI components
        initializeViews()

        // Set up click listeners
        setupClickListeners()

        // Check if user is already logged in
        checkCurrentUser()
    }

    private fun initializeGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initializeViews() {
        signUpLink = findViewById(R.id.signUpLink)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox)
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink)
        loginButton = findViewById(R.id.loginButton)
        googleSignInButton = findViewById(R.id.googleSignInButton)
    }

    private fun setupClickListeners() {
        // Sign up link
        signUpLink.setOnClickListener {
            val intent = Intent(this, RegistrationPage::class.java)
            startActivity(intent)
        }

        // Login button
        loginButton.setOnClickListener {
            performEmailLogin()
        }

        // Forgot password link
        forgotPasswordLink.setOnClickListener {
            // TODO: Implement forgot password functionality
            Toast.makeText(this, "Forgot password feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Google sign in button
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun checkCurrentUser() {
        userRepository.getCurrentUser(
            onSuccess = { user ->
                if (user != null) {
                    // User is already logged in, navigate to main app
                    navigateToMainApp()
                }
            },
            onFailure = { /* User not logged in */ }
        )
    }

    private fun performEmailLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        // Validate inputs
        if (email.isEmpty()) {
            emailEditText.error = getString(R.string.email_is_required)
            emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = getString(R.string.password_is_required)
            passwordEditText.requestFocus()
            return
        }

        // Disable button to prevent multiple submissions
        loginButton.isEnabled = false
        loginButton.text = getString(R.string.logging_in)

        // Login with email and password
        userRepository.loginWithEmail(
            email = email,
            password = password,
            onSuccess = { user ->
                runOnUiThread {
                    Toast.makeText(this, "Welcome back, ${user.fullName}!", Toast.LENGTH_SHORT).show()

                    // Save remember me preference if needed
                    if (rememberMeCheckbox.isChecked) {
                        saveRememberMePreference(email)
                    }

                    navigateToMainApp()
                }
            },
            onFailure = { exception ->
                runOnUiThread {
                    loginButton.isEnabled = true
                    loginButton.text = getString(R.string.log_in)

                    val errorMessage = when {
                        exception.message?.contains("no user record") == true ->
                            getString(R.string.no_account_found)
                        exception.message?.contains("password is invalid") == true ->
                            getString(R.string.incorrect_password)
                        exception.message?.contains("network error") == true ->
                            getString(R.string.network_error)
                        else -> getString(R.string.login_failed) + exception.message
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun signInWithGoogle() {
        // Sign out first to force account picker to show
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                userRepository.signInWithGoogle(
                    idToken = idToken,
                    onSuccess = { user ->
                        runOnUiThread {
                            Toast.makeText(this, "Welcome, ${user.fullName}!", Toast.LENGTH_SHORT).show()
                            navigateToMainApp()
                        }
                    },
                    onFailure = { exception ->
                        runOnUiThread {
                            Toast.makeText(this, "Google sign-in failed: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } else {
                Toast.makeText(this, "Google sign-in failed: No ID token", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMainApp() {
        val intent = Intent(this, MapsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun saveRememberMePreference(email: String) {
        val sharedPref = getSharedPreferences("login_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("saved_email", email)
            putBoolean("remember_me", true)
            apply()
        }
    }




    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
     */
}