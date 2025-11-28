package com.example.test.doctor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.data.User
import com.example.test.data.UserRepository
import com.example.test.medicines.Medicine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class DoctorViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val userRepository = UserRepository()

    private val _patients = MutableStateFlow<List<User>>(emptyList())
    val patients: StateFlow<List<User>> = _patients

    private val _linkingResult = MutableStateFlow<Result<Unit>?>(null)
    val linkingResult: StateFlow<Result<Unit>?> = _linkingResult

    private val _patientMedicines = MutableStateFlow<List<Medicine>>(emptyList())
    val patientMedicines: StateFlow<List<Medicine>> = _patientMedicines

    init {
        fetchPatients()
    }

    fun fetchPatients() {
        viewModelScope.launch {
            val doctorId = auth.currentUser?.uid ?: return@launch
            try {
                val doctorDoc = firestore.collection("users").document(doctorId).get().await()
                val doctor = doctorDoc.toObject<User>()
                
                if (doctor?.patientIds?.isNotEmpty() == true) {
                    val patientsQuery = firestore.collection("users")
                        .whereIn("uid", doctor.patientIds)
                        .get()
                        .await()
                    _patients.value = patientsQuery.toObjects(User::class.java)
                } else {
                    _patients.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("DoctorViewModel", "Error fetching patients", e)
            }
        }
    }
    
    fun linkPatient(patientEmail: String) {
        viewModelScope.launch {
            val result = userRepository.linkPatientToDoctor(patientEmail)
            _linkingResult.value = result
            if (result.isSuccess) {
                fetchPatients() // Refresh the patient list on success
            }
        }
    }

    fun getPatientMedicines(patientId: String) {
        viewModelScope.launch {
            firestore.collection("users").document(patientId).collection("medicines")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("DoctorViewModel", "Error fetching patient medicines", e)
                        return@addSnapshotListener
                    }
                    _patientMedicines.value = snapshot?.toObjects(Medicine::class.java) ?: emptyList()
                }
        }
    }

    fun prescribeMedicine(patientId: String, medicine: Medicine, frequency: Int?) {
        viewModelScope.launch {
            try {
                val collection = firestore.collection("users").document(patientId).collection("medicines")
                
                if (frequency == null) {
                    collection.document(medicine.id).set(medicine).await()
                    return@launch
                }
                
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, medicine.time.substringBefore(":").toInt())
                    set(Calendar.MINUTE, medicine.time.substringAfter(":").toInt())
                }

                for (i in 0 until frequency) {
                    val doseTime = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
                    val id = collection.document().id
                    val newDose = medicine.copy(id = id, time = doseTime, isTaken = false)
                    collection.document(id).set(newDose).await()
                    
                    calendar.add(Calendar.HOUR_OF_DAY, 6)
                }
            } catch (e: Exception) {
                Log.e("DoctorViewModel", "Error prescribing medicine", e)
            }
        }
    }

    fun deleteMedicine(patientId: String, medicineId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(patientId).collection("medicines")
                    .document(medicineId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e("DoctorViewModel", "Error deleting medicine", e)
            }
        }
    }
}