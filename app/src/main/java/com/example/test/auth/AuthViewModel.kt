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
