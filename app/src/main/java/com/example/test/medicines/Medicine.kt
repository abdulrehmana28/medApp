package com.example.test.medicines

/**
 * Represents a prescribed medicine.
 * @param selectedDays List of integers representing days of the week (java.util.Calendar constants: 1=Sunday, 2=Monday, etc.)
 */
data class Medicine(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val time: String = "", // e.g., "09:00"
    @get:JvmName("getIsTaken")
    val isTaken: Boolean = false,
    val doctorName: String = "",
    val selectedDays: List<Int> = emptyList() // NEW FIELD: Stores days like [2, 4, 6] for Mon/Wed/Fri
)