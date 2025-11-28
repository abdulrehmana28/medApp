package com.example.test.medicines

/**
 * Represents a prescribed medicine, including the time it should be taken.
 * This will be stored in a user's sub-collection.
 */
data class Medicine(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val time: String = "", // Time stored as a string, e.g., "09:00 AM"
    @get:JvmName("getIsTaken") // Solves Firestore mapping issue for "is" prefixed fields
    val isTaken: Boolean = false,
    val doctorName: String = "" // Name of the prescribing doctor
)