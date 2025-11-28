package com.example.test.doctor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            FloatingActionButton(
                onClick = {
                    selectedMedicine = null // Clear selection for new prescription
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

    val initialHour = medicine?.time?.substringBefore(":")?.toIntOrNull() ?: 8
    val initialMinute = medicine?.time?.substringAfter(":")?.toIntOrNull() ?: 0
    val timeState = rememberTimePickerState(initialHour, initialMinute, false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        // This outer column allows the button to be pushed up by the keyboard
        Column(Modifier.navigationBarsPadding().imePadding()) {
            // This inner column contains the scrollable content
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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
            }
            
            // Save Button is outside the scrollable column but inside the IME padded column
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    val formattedTime = String.format("%02d:%02d", timeState.hour, timeState.minute)
                    val updatedMedicine = medicine?.copy(name = name, dosage = dosage, time = formattedTime)
                        ?: Medicine(name = name, dosage = dosage, time = formattedTime)

                    val freqInt = if (isEditing) null else frequency.toIntOrNull() ?: 1
                    onSave(updatedMedicine, freqInt)
                }
            ) {
                Text(if (isEditing) "Save Changes" else "Save Prescription")
            }
            Spacer(modifier = Modifier.height(16.dp)) // Spacer at the very bottom
        }
    }
}
