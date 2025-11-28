package com.example.test.doctor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test.medicines.Medicine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: String,
    doctorViewModel: DoctorViewModel = viewModel()
) {
    val medicines by doctorViewModel.patientMedicines.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var selectedMedicine by remember { mutableStateOf<Medicine?>(null) }

    LaunchedEffect(patientId) {
        doctorViewModel.getPatientMedicines(patientId)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedMedicine = null // Clear selection for new prescription
                showSheet = true
            }) {
                Text("Prescribe Medicine")
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
                        IconButton(onClick = {
                            selectedMedicine = medicine
                            showSheet = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Medicine")
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
            onSave = { updatedMedicine, frequency ->
                doctorViewModel.prescribeMedicine(patientId, updatedMedicine, frequency)
                showSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrescribeMedicineSheet(
    medicine: Medicine?,
    onDismiss: () -> Unit,
    onSave: (Medicine, Int?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf(medicine?.name ?: "") }
    var dosage by remember { mutableStateOf(medicine?.dosage ?: "") }
    var frequency by remember { mutableStateOf("1") }
    val isEditing = medicine != null

    // Default time is 8 AM for new, or existing time for edits
    val initialHour = medicine?.time?.substringBefore(":")?.toIntOrNull() ?: 8
    val initialMinute = medicine?.time?.substringAfter(":")?.toIntOrNull() ?: 0
    val timeState = rememberTimePickerState(initialHour, initialMinute, false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEditing) "Edit Prescription" else "New Prescription",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Medicine Name") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Dosage") }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Show frequency only for new prescriptions
            if (!isEditing) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Frequency (times per day)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(if (isEditing) "Edit Time" else "Start Time")
            TimePicker(state = timeState)

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                val formattedTime = String.format("%02d:%02d", timeState.hour, timeState.minute)
                val updatedMedicine = medicine?.copy(name = name, dosage = dosage, time = formattedTime)
                    ?: Medicine(name = name, dosage = dosage, time = formattedTime)

                val freqInt = if (isEditing) null else frequency.toIntOrNull() ?: 1
                onSave(updatedMedicine, freqInt)
            }) {
                Text(if (isEditing) "Save Changes" else "Save Prescription")
            }
        }
    }
}