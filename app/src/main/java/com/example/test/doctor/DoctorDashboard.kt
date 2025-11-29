package com.example.test.doctor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.test.chat.ChatUtils
import com.example.test.home.HomeViewModel
import kotlinx.coroutines.launch
import com.example.test.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboard(
    navController: NavController,
    doctorViewModel: DoctorViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
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

                    // Logout Button
                    TextButton(onClick = {
                        authViewModel.signOut()
                        // Navigate back to login and clear backstack
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            // Clicking the whole card goes to details (edit medicines)
                            .clickable { navController.navigate("patient_details/${patient.uid}") }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Patient Name
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = patient.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Tap to edit prescriptions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            // 2. Action Buttons (Chat & Analysis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Chat Button (Icon only to save space)
                                IconButton(onClick = {
                                    val chatId = ChatUtils.getChatId(user!!.uid, patient.uid)
                                    navController.navigate("chat/$chatId/${patient.name}")
                                }) {
                                    Icon(Icons.Default.Email, contentDescription = "Chat")
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Analysis Button
                                Button(
                                    onClick = {
                                        // Navigate to the Analysis Screen
                                        navController.navigate("patient_analysis/${patient.uid}/${patient.name}")
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DateRange, // Using DateRange icon as it looks like a table/schedule
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analysis")
                                }
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