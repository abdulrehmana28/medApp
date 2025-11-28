package com.example.test.doctor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test.medicines.MedicineViewModel

@Composable
fun PatientDetailsScreen(patientId: String, medicineViewModel: MedicineViewModel = viewModel()) {
    val medicines by medicineViewModel.medicines.collectAsState()
    var showAddMedicineDialog by remember { mutableStateOf(false) }

    LaunchedEffect(patientId) {
        medicineViewModel.fetchMedicinesForPatient(patientId)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddMedicineDialog = true }) {
                Text("+")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp)
        ) {
            if (showAddMedicineDialog) {
                AddMedicineDialog(
                    onDismiss = { showAddMedicineDialog = false },
                    onAddMedicine = { name, dosage ->
                        medicineViewModel.addMedicineForPatient(patientId, name, dosage)
                        showAddMedicineDialog = false
                    }
                )
            }

            LazyColumn {
                items(medicines) { medicine ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = medicine.name, style = MaterialTheme.typography.headlineSmall)
                                Text(text = medicine.dosage, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { medicineViewModel.deleteMedicine(medicine.id, patientId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Medicine")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMedicineDialog(
    onDismiss: () -> Unit,
    onAddMedicine: (name: String, dosage: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Medicine") },
        text = {
            Column {
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
            }
        },
        confirmButton = {
            Button(onClick = { onAddMedicine(name, dosage) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
