package com.example.budgieflighttracker.repositories

import com.example.budgieflighttracker.datasource.UserRemoteDataSource
import com.example.budgieflighttracker.models.User

class UserRepository(
    private val userRemoteDataSource: UserRemoteDataSource
) {

    fun registerWithEmail(
        email: String,
        password: String,
        fullName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            onFailure(IllegalArgumentException("All fields are required"))
            return
        }

        userRemoteDataSource.registerWithEmail(email, password, fullName, onSuccess, onFailure)
    }

    fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (email.isEmpty() || password.isEmpty()) {
            onFailure(IllegalArgumentException("Email and password are required"))
            return
        }

        userRemoteDataSource.loginWithEmail(email, password, onSuccess, onFailure)
    }

    fun signInWithGoogle(
        idToken: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        userRemoteDataSource.signInWithGoogle(idToken, onSuccess, onFailure)
    }

    fun getCurrentUser(onSuccess: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        userRemoteDataSource.getCurrentUser(onSuccess, onFailure)
    }

    fun updateUser(userId: String, user: User, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        userRemoteDataSource.updateUser(userId, user, onSuccess, onFailure)
    }

    fun signOut() {
        userRemoteDataSource.signOut()
    }

    fun deleteAccount(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        userRemoteDataSource.deleteAccount(onSuccess, onFailure)
    }
}