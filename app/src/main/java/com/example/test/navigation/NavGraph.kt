package com.example.test.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.test.auth.AuthState
import com.example.test.auth.AuthViewModel
import com.example.test.auth.LoginScreen
import com.example.test.auth.RegisterScreen
import com.example.test.chat.ChatScreen
import com.example.test.doctor.DoctorDashboard
import com.example.test.doctor.PatientDetailScreen
import com.example.test.home.HomeScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            LaunchedEffect(authState) {
                when (authState) {
                    AuthState.Authenticated -> navController.navigate("home") { popUpTo("splash") { inclusive = true } }
                    is AuthState.Error, AuthState.Idle -> navController.navigate("login") { popUpTo("splash") { inclusive = true } }
                    AuthState.Loading -> { /* Do nothing, just wait on the splash screen */ }
                }
            }
        }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("home") { HomeScreen(navController) }
        composable(
            "patient_details/{patientId}",
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) {
            PatientDetailScreen(it.arguments?.getString("patientId") ?: "")
        }
        composable(
            "chat/{chatId}/{chatPartnerName}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("chatPartnerName") { type = NavType.StringType }
            )
        ) {
            val chatPartnerName = it.arguments?.getString("chatPartnerName") ?: ""
            ChatScreen(
                chatId = it.arguments?.getString("chatId") ?: "", 
                chatPartnerName = chatPartnerName,
                navController = navController // Pass the NavController
            )
        }
    }
}
