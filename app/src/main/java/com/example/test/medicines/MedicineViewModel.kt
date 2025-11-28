package com.example.test.medicines

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MedicineViewModel : ViewModel() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var medicinesListener: ListenerRegistration? = null

    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    val medicines: StateFlow<List<Medicine>> = _medicines

    private fun getMedicinesCollection(patientId: String) = 
        firestore.collection("users").document(patientId).collection("medicines")

    fun fetchMedicinesForPatient(patientId: String) {
        // Remove any existing listener to avoid multiple listeners
        medicinesListener?.remove()
        
        medicinesListener = getMedicinesCollection(patientId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("MedicineViewModel", "Error fetching medicines", e)
                _medicines.value = emptyList()
                return@addSnapshotListener
            }
            _medicines.value = snapshot?.toObjects(Medicine::class.java) ?: emptyList()
        }
    }

    // This function is now mostly obsolete and should be removed in a future refactor.
    fun addMedicineForPatient(patientId: String, name: String, dosage: String) {
        viewModelScope.launch {
            try {
                val medicinesCollection = getMedicinesCollection(patientId)
                val id = medicinesCollection.document().id
                val medicine = Medicine(id, name, dosage, "", false) // Corrected constructor
                medicinesCollection.document(id).set(medicine).await()
            } catch (e: Exception) {
                Log.e("MedicineViewModel", "Error adding medicine", e)
            }
        }
    }

    fun deleteMedicine(medicineId: String, patientId: String) {
        viewModelScope.launch {
            try {
                getMedicinesCollection(patientId).document(medicineId).delete().await()
            } catch (e: Exception) {
                Log.e("MedicineViewModel", "Error deleting medicine", e)
            }
        }
    }
    
    fun updateMedicineTakenStatus(patientId: String, medicineId: String, isTaken: Boolean) {
        viewModelScope.launch {
            try {
                getMedicinesCollection(patientId).document(medicineId).update("isTaken", isTaken).await()
            } catch (e: Exception) {
                Log.e("MedicineViewModel", "Error updating medicine status", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up the listener when the ViewModel is destroyed
        medicinesListener?.remove()
    }
}