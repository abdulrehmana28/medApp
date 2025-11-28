package com.example.test.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.test.patient.PatientDashboard
import com.example.test.doctor.DoctorDashboard

@Composable
fun HomeScreen(navController: NavController, homeViewModel: HomeViewModel = viewModel()) {
    val user by homeViewModel.user.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (user == null) {
            CircularProgressIndicator()
        } else {
            when (user?.role) {
                "patient" -> PatientDashboard(navController, user!!)
                "doctor" -> DoctorDashboard(navController)
                else -> Text("Unknown user role")
            }
        }
    }
}
