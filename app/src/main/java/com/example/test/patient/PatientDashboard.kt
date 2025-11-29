package com.example.test.patient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.test.auth.AuthViewModel
import com.example.test.chat.ChatUtils
import com.example.test.data.User
import com.example.test.medicines.AlarmScheduler
import com.example.test.medicines.MedicineViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboard(
    navController: NavController,
    user: User,
    medicineViewModel: MedicineViewModel = viewModel(),
    patientViewModel: PatientViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val medicines by medicineViewModel.medicines.collectAsState()
    val doctors by patientViewModel.linkedDoctors.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- PERMISSION LOGIC ---

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission granted. We can optionally show a success message.
                scope.launch {
                    snackbarHostState.showSnackbar("Notifications enabled! Alarms will work.")
                }
            } else {
                // Permission Denied: Show Snackbar with "Settings" action
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Notifications are needed for medicine reminders.",
                        actionLabel = "Settings",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Open App Settings if user clicks "Settings"
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    )

    // Check permissions ONCE when screen loads
    LaunchedEffect(Unit) {
        // 1. Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. Exact Alarm Permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                val result = snackbarHostState.showSnackbar(
                    message = "Exact alarms are required for timely reminders.",
                    actionLabel = "Allow",
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    context.startActivity(intent)
                }
            }
        }
    }

    // --- DATA LOADING & ALARM SCHEDULING ---

    LaunchedEffect(user.uid) {
        medicineViewModel.fetchMedicinesForPatient(user.uid)
        patientViewModel.fetchLinkedDoctors()
    }

    LaunchedEffect(medicines) {
        medicines.forEach { medicine ->
            if (!medicine.isTaken) {
                AlarmScheduler.scheduleAlarmsForMedicine(context, medicine)
            }
        }
    }

    // --- UI STRUCTURE ---

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // REQUIRED for Snackbar to show
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
                            Text("Chat with Dr ${primaryDoctor.name}")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    //  Logout Button
                    TextButton(onClick = {
                        authViewModel.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Optional: Persistent Warning Banner if permission is missing (Double check)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                ) {
                    Text(
                        "⚠️ Notifications are disabled. Tap here to enable them.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(medicines) { medicine ->
                    MedicineCard(medicine, user, medicineViewModel)
                }
            }
        }
    }
}

@Composable
fun MedicineCard(medicine: com.example.test.medicines.Medicine, user: User, viewModel: MedicineViewModel) {
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
                    viewModel.updateMedicineTakenStatus(user.uid, medicine.id, isChecked)
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                val textDecoration = if (medicine.isTaken) TextDecoration.LineThrough else TextDecoration.None
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.headlineSmall,
                    textDecoration = textDecoration
                )
                Text(
                    text = "Dosage: ${medicine.dosage}",
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = textDecoration
                )
                Text(
                    text = "Time: ${medicine.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = textDecoration
                )
                if (medicine.doctorName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Prescribed by Dr. ${medicine.doctorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}