package com.example.test.data

/**
 * Represents a user, who can be either a patient or a doctor.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    // For patients: A list of doctor UIDs they are linked to.
    val linkedDoctorIds: List<String> = emptyList(),
    // For doctors: A list of patient UIDs they are linked to.
    val patientIds: List<String> = emptyList()
)