package com.example.budgieflighttracker.datasource

import com.example.budgieflighttracker.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class UserRemoteDataSource {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    fun registerWithEmail(
        email: String,
        password: String,
        fullName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: ""

                val user = User(
                    userId = userId,
                    email = email,
                    fullName = fullName,
                    authProvider = "email"
                )

                usersCollection.document(userId)
                    .set(user)
                    .addOnSuccessListener {
                        println("User registered and saved to Firestore with ID: $userId")
                        onSuccess(userId)
                    }
                    .addOnFailureListener { e ->
                        println("Error saving user to Firestore: $e")
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                println("Error creating user with email: $e")
                onFailure(e)
            }
    }

    fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: ""

                getUserById(userId, { user ->
                    if (user != null) {
                        onSuccess(user)
                    } else {
                        onFailure(Exception("User data not found"))
                    }
                }, onFailure)
            }
            .addOnFailureListener { e ->
                println("Error signing in with email: $e")
                onFailure(e)
            }
    }

    fun signInWithGoogle(
        idToken: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                val userId = firebaseUser?.uid ?: ""

                usersCollection.document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val user = document.toObject(User::class.java)
                            if (user != null) {
                                onSuccess(user)
                            } else {
                                onFailure(Exception("Failed to parse user data"))
                            }
                        } else {
                            val user = User(
                                userId = userId,
                                email = firebaseUser?.email ?: "",
                                fullName = firebaseUser?.displayName ?: "",
                                profileImageUrl = firebaseUser?.photoUrl?.toString() ?: "",
                                authProvider = "google"
                            )

                            usersCollection.document(userId).set(user)
                                .addOnSuccessListener { onSuccess(user) }
                                .addOnFailureListener { onFailure(it) }
                        }
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { e ->
                println("Error signing in with Google: $e")
                onFailure(e)
            }
    }

    fun getCurrentUser(onSuccess: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            getUserById(currentUser.uid, onSuccess, onFailure)
        } else {
            onSuccess(null)
        }
    }

    fun getUserById(userId: String, onSuccess: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    onSuccess(user)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e ->
                println("Error getting user by ID: $e")
                onFailure(e)
            }
    }

    fun updateUser(userId: String, user: User, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId)
            .set(user)
            .addOnSuccessListener {
                println("User updated successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error updating user: $e")
                onFailure(e)
            }
    }

    fun signOut() {
        auth.signOut()
    }

    fun deleteAccount(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid

            usersCollection.document(userId)
                .delete()
                .addOnSuccessListener {
                    user.delete()
                        .addOnSuccessListener {
                            println("User account deleted successfully")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            println("Error deleting user from Auth: $e")
                            onFailure(e)
                        }
                }
                .addOnFailureListener { e ->
                    println("Error deleting user from Firestore: $e")
                    onFailure(e)
                }
        } else {
            onFailure(Exception("No user is currently signed in"))
        }
    }
}