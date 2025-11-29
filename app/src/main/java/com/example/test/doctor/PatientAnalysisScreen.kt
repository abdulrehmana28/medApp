package com.example.test.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.test.medicines.Medicine
import com.example.test.medicines.MedicineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientAnalysisScreen(
    navController: NavController,
    patientId: String,
    patientName: String = "Patient", // Optional: pass name if available for title
    medicineViewModel: MedicineViewModel = viewModel()
) {
    // Fetch this specific patient's medicines when screen loads
    LaunchedEffect(patientId) {
        medicineViewModel.fetchMedicinesForPatient(patientId)
    }

    val patientMedicines by medicineViewModel.medicines.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$patientName's Adherence") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Medicine Intake Status today",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // The Table Structure
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    // Table Header
                    TableHeaderRow()
                    Divider()
                    // Table Content
                    LazyColumn {
                        items(patientMedicines) { medicine ->
                            TableRowItem(medicine)
                            Divider(color = Color.LightGray, thickness = 0.5.dp)
                        }
                    }
                    if (patientMedicines.isEmpty()) {
                        Text(
                            text = "No medicines prescribed today.",
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// Define weights for columns so headers align with content rows
private val columnWeights = listOf(2f, 1f, 1f, 1f)

@Composable
fun TableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(text = "Medicine", weight = columnWeights[0], header = true)
        TableCell(text = "Dosage", weight = columnWeights[1], header = true)
        TableCell(text = "Time", weight = columnWeights[2], header = true)
        TableCell(text = "Status", weight = columnWeights[3], header = true, align = TextAlign.Center)
    }
}

@Composable
fun TableRowItem(medicine: Medicine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(text = medicine.name, weight = columnWeights[0])
        TableCell(text = medicine.dosage, weight = columnWeights[1])
        TableCell(text = medicine.time, weight = columnWeights[2])

        // Status Icon Cell
        Box(
            modifier = Modifier.weight(columnWeights[3]),
            contentAlignment = Alignment.Center
        ) {
            if (medicine.isTaken) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Taken",
                        tint = Color(0xFF4CAF50) // Green
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Taken", fontSize = 12.sp, color = Color(0xFF4CAF50))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Pending",
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pending", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    header: Boolean = false,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        textAlign = align,
        fontSize = if (header) 16.sp else 14.sp,
        color = if (header) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified
    )
}