package com.example.test.patient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.test.chat.ChatUtils
import com.example.test.data.User
import com.example.test.medicines.AlarmScheduler
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Fetch Data on Load
    LaunchedEffect(user.uid) {
        medicineViewModel.fetchMedicinesForPatient(user.uid)
        patientViewModel.fetchLinkedDoctors()
    }

    // 2. Schedule Alarms whenever the medicine list changes
    // This ensures that if a new medicine is added remotely, the alarm is set instantly.
    LaunchedEffect(medicines) {
        medicines.forEach { medicine ->
            if (!medicine.isTaken) {
                AlarmScheduler.scheduleAlarmsForMedicine(context, medicine)
            } else {
                // Optional: Cancel alarm if taken? Usually we keep it for the next schedule.
                // AlarmScheduler.cancelAlarms(context, medicine)
            }
        }
    }

    // 3. Permission Logic
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Great! Notifications will work.
            }
        }
    )

    // Check permissions whenever the app comes to the foreground (onResume)
    // This handles the case where the user goes to Settings -> Allows Permission -> Returns to App
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Check Notification Permission (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Check Exact Alarm Permission (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
                    if (alarmManager?.canScheduleExactAlarms() == false) {
                        // Ideally, show a Dialog explaining WHY before redirecting
                        // For MVP, we just redirect if missing.
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        context.startActivity(intent)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(medicines) { medicine ->
                MedicineCard(medicine, user, medicineViewModel)
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
                // Display Doctor Name if available
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