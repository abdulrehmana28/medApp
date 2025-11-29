package com.example.test.doctor

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test.medicines.Medicine
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: String,
    doctorViewModel: DoctorViewModel = viewModel()
) {
    val medicines by doctorViewModel.patientMedicines.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var selectedMedicine by remember { mutableStateOf<Medicine?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }

    LaunchedEffect(patientId) {
        doctorViewModel.getPatientMedicines(patientId)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedMedicine = null
                    showSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Prescribe Medicine"
                )
            }
        }
    ) {
        LazyColumn(contentPadding = it) {
            items(medicines) { medicine ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = medicine.name, style = MaterialTheme.typography.titleLarge)
                            Text(text = "Dosage: ${medicine.dosage}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Time: ${medicine.time}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row {
                            IconButton(onClick = {
                                selectedMedicine = medicine
                                showSheet = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Medicine")
                            }
                            IconButton(onClick = { 
                                medicineToDelete = medicine
                                showDeleteDialog = true
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Medicine", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        PrescribeMedicineSheet(
            medicine = selectedMedicine,
            onDismiss = { showSheet = false },
            onSave = { updatedMedicine ->
                doctorViewModel.prescribeMedicine(patientId, updatedMedicine)
            }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                medicineToDelete?.let { doctorViewModel.deleteMedicine(patientId, it.id) }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Medicine?") },
        text = { Text("Are you sure you want to remove this prescription?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrescribeMedicineSheet(
    medicine: Medicine?,
    onDismiss: () -> Unit,
    onSave: (Medicine) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf(medicine?.name ?: "") }
    var dosage by remember { mutableStateOf(medicine?.dosage ?: "") }

    // Day Selection State
    // Default to empty (which logic handles as daily) or pre-select existing days
    var selectedDays by remember { mutableStateOf(medicine?.selectedDays ?: emptyList()) }

    var isNameError by remember { mutableStateOf(false) }
    var isDosageError by remember { mutableStateOf(false) }

    val initialHour = medicine?.time?.substringBefore(":")?.toIntOrNull() ?: 8
    val initialMinute = medicine?.time?.substringAfter(":")?.toIntOrNull() ?: 0
    val timeState = rememberTimePickerState(initialHour, initialMinute, false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.navigationBarsPadding().imePadding()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (medicine != null) "Edit Prescription" else "New Prescription",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isNameError = false
                    },
                    label = { Text("Medicine Name") },
                    isError = isNameError,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dosage,
                    onValueChange = {
                        dosage = it
                        isDosageError = false
                    },
                    label = { Text("Dosage") },
                    isError = isDosageError,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // NEW: Day Selector UI
                Text("Frequency", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                DaySelector(selectedDays = selectedDays) { newDays ->
                    selectedDays = newDays
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Time", style = MaterialTheme.typography.titleMedium)
                TimePicker(state = timeState)
            }

            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    isNameError = name.isBlank()
                    isDosageError = dosage.isBlank()

                    if (isNameError || isDosageError) {
                        Toast.makeText(context, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val formattedTime = String.format("%02d:%02d", timeState.hour, timeState.minute)

                    // Create the updated medicine object with selected days
                    val updatedMedicine = medicine?.copy(
                        name = name,
                        dosage = dosage,
                        time = formattedTime,
                        selectedDays = selectedDays
                    ) ?: Medicine(
                        name = name,
                        dosage = dosage,
                        time = formattedTime,
                        selectedDays = selectedDays
                    )

                    onSave(updatedMedicine)
                }
            ) {
                Text("Save Prescription")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// The Bubble Selector
@Composable
fun DaySelector(selectedDays: List<Int>, onSelectionChanged: (List<Int>) -> Unit) {
    val days = listOf(
        "S" to Calendar.SUNDAY,
        "M" to Calendar.MONDAY,
        "T" to Calendar.TUESDAY,
        "W" to Calendar.WEDNESDAY,
        "T" to Calendar.THURSDAY,
        "F" to Calendar.FRIDAY,
        "S" to Calendar.SATURDAY
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { (label, dayConst) ->
            val isSelected = selectedDays.contains(dayConst)
            val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val contentColor = if (isSelected) Color.White else Color.Gray
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, borderColor, CircleShape)
                    .background(backgroundColor)
                    .clickable {
                        val newSelection = if (isSelected) {
                            selectedDays - dayConst
                        } else {
                            selectedDays + dayConst
                        }
                        onSelectionChanged(newSelection)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = label, color = contentColor, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}