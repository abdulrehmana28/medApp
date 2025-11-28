package com.example.test.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PatientViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _linkedDoctors = MutableStateFlow<List<User>>(emptyList())
    val linkedDoctors: StateFlow<List<User>> = _linkedDoctors

    fun fetchLinkedDoctors() {
        viewModelScope.launch {
            val patientId = auth.currentUser?.uid ?: return@launch
            try {
                val patientDoc = firestore.collection("users").document(patientId).get().await()
                val patient = patientDoc.toObject<User>()

                if (patient?.linkedDoctorIds?.isNotEmpty() == true) {
                    val doctorsQuery = firestore.collection("users")
                        .whereIn("uid", patient.linkedDoctorIds)
                        .get()
                        .await()
                    _linkedDoctors.value = doctorsQuery.toObjects(User::class.java)
                } else {
                    _linkedDoctors.value = emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}