package com.example.test.doctor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.test.chat.ChatUtils
import com.example.test.home.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboard(
    navController: NavController, 
    doctorViewModel: DoctorViewModel = viewModel(), 
    homeViewModel: HomeViewModel = viewModel()
) {
    val patients by doctorViewModel.patients.collectAsState()
    val user by homeViewModel.user.collectAsState()
    val linkingResult by doctorViewModel.linkingResult.collectAsState()
    
    var showLinkPatientDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(linkingResult) {
        linkingResult?.let {
            if (it.isSuccess) {
                scope.launch { snackbarHostState.showSnackbar("Patient linked successfully!") }
            } else {
                val errorMessage = it.exceptionOrNull()?.message ?: "Failed to link patient."
                scope.launch { snackbarHostState.showSnackbar(errorMessage) }
            }
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("My Patients") },
                actions = {
                    Button(onClick = { showLinkPatientDialog = true }) {
                        Text("Link New Patient")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp)
        ) {
            if (showLinkPatientDialog) {
                LinkPatientDialog(
                    onDismiss = { showLinkPatientDialog = false },
                    onLinkPatient = { email ->
                        doctorViewModel.linkPatient(email)
                        showLinkPatientDialog = false
                    }
                )
            }

            LazyColumn {
                items(patients) { patient ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { navController.navigate("patient_details/${patient.uid}") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Display patient's name instead of email
                            Text(text = patient.name, style = MaterialTheme.typography.bodyLarge)
                            Button(onClick = { 
                                val chatId = ChatUtils.getChatId(user!!.uid, patient.uid)
                                // Pass patient's name to the chat screen
                                navController.navigate("chat/$chatId/${patient.name}")
                            }) {
                                Text("Chat")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkPatientDialog(
    onDismiss: () -> Unit,
    onLinkPatient: (email: String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link New Patient") },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Patient Email") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onLinkPatient(email) }) {
                Text("Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
