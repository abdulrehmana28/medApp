package com.example.test.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Check for existing user on startup
    init {
        if (auth.currentUser != null) {
            _authState.value = AuthState.Authenticated
        }
    }

    // signOut function
    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account found with this email."
                    is FirebaseAuthInvalidCredentialsException -> "Incorrect password. Please try again."
                    else -> e.message ?: "Login failed. Please try again."
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    fun register(name: String, email: String, password: String, isDoctor: Boolean) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = User(
                    uid = authResult.user!!.uid,
                    name = name,
                    email = email,
                    role = if (isDoctor) "doctor" else "patient"
                )
                firestore.collection("users").document(user.uid).set(user).await()
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthWeakPasswordException -> "Your password is too weak. Please use at least 6 characters."
                    is FirebaseAuthUserCollisionException -> "An account with this email already exists."
                    else -> e.message ?: "Registration failed. Please try again."
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    fun signInWithGoogleCredential(credential: AuthCredential) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.signInWithCredential(credential).await()
                // Check if user exists in Firestore, if not, create them
                checkAndCreateUserInFirestore(authResult.user!!)
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
            }
        }
    }

    private suspend fun checkAndCreateUserInFirestore(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        val docRef = firestore.collection("users").document(firebaseUser.uid)
        val snapshot = docRef.get().await()

        if (!snapshot.exists()) {
            // Logic: New Google users are "patients" by default
            val newUser = User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Google User",
                email = firebaseUser.email ?: "",
                role = "patient"
            )
            docRef.set(newUser).await()
        }
    }
    
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
