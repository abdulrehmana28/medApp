package com.example.test.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Allows a doctor to link a patient to their account by the patient's email.
     * This function is intended to be called by a logged-in doctor.
     */
    suspend fun linkPatientToDoctor(patientEmail: String): Result<Unit> {
        val doctorId = auth.currentUser?.uid
            ?: return Result.failure(Exception("Doctor is not logged in."))

        return try {
            // Find the patient by their email
            val patientQuery = firestore.collection("users")
                .whereEqualTo("email", patientEmail)
                .whereEqualTo("role", "patient")
                .get()
                .await()

            if (patientQuery.isEmpty) {
                return Result.failure(Exception("No patient found with that email."))
            }

            val patientDoc = patientQuery.documents.first()
            val patientId = patientDoc.id

            // Add the doctor's ID to the patient's linkedDoctorIds list
            firestore.collection("users").document(patientId)
                .update("linkedDoctorIds", FieldValue.arrayUnion(doctorId)).await()

            // Add the patient's ID to the doctor's patientIds list
            firestore.collection("users").document(doctorId)
                .update("patientIds", FieldValue.arrayUnion(patientId)).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}