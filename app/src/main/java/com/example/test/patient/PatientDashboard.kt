package com.example.test.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.test.chat.ChatUtils
import com.example.test.data.User
import com.example.test.medicines.MedicineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboard(
    navController: NavController,
    user: User,
    medicineViewModel: MedicineViewModel = viewModel(),
    patientViewModel: PatientViewModel = viewModel()
) {
    val medicines by medicineViewModel.medicines.collectAsState()
    val doctors by patientViewModel.linkedDoctors.collectAsState()

    LaunchedEffect(user.uid) {
        medicineViewModel.fetchMedicinesForPatient(user.uid)
        patientViewModel.fetchLinkedDoctors()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Prescriptions") },
                actions = {
                    if (doctors.isNotEmpty()) {
                        val primaryDoctor = doctors.first()
                        Button(onClick = { 
                            val chatId = ChatUtils.getChatId(user.uid, primaryDoctor.uid)
                            navController.navigate("chat/$chatId/${primaryDoctor.name}")
                        }) {
                            Text("Chat with ${primaryDoctor.name}")
                        }
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(medicines) { medicine ->
                val cardColors = if (medicine.isTaken) {
                    CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.5f))
                } else {
                    CardDefaults.cardColors()
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = cardColors
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = medicine.isTaken,
                            onCheckedChange = { isChecked ->
                                medicineViewModel.updateMedicineTakenStatus(user.uid, medicine.id, isChecked)
                            }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val textDecoration = if (medicine.isTaken) TextDecoration.LineThrough else TextDecoration.None
                            Text(text = medicine.name, style = MaterialTheme.typography.headlineSmall, textDecoration = textDecoration)
                            Text(text = "Dosage: ${medicine.dosage}", style = MaterialTheme.typography.bodyMedium, textDecoration = textDecoration)
                            Text(text = "Time: ${medicine.time}", style = MaterialTheme.typography.bodyMedium, textDecoration = textDecoration)
                        }
                    }
                }
            }
        }
    }
}
