package com.example.test.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState by authViewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                navController.navigate("home") { popUpTo("login") { inclusive = true } }
                authViewModel.resetAuthState()
            }
            is AuthState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
                // NOTE: We no longer reset the state immediately.
                // This keeps the error message visible until the user takes another action.
            }
            else -> {}
        }
    }

    val context = LocalContext.current
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("676511602560-loou4cov7m2gok8kilv2550nk9sq6rev.apps.googleusercontent.com") // <--- Web client ID
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            authViewModel.signInWithGoogleCredential(credential)
        } catch (e: ApiException) {
            // Optional: Handle error (e.g., user cancelled)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { authViewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = authState !is AuthState.Loading // Corrected state check
            ) {
                if (authState is AuthState.Loading) { // Corrected state check
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            //  SignIn GOOGLE BUTTON
            OutlinedButton(
                onClick = {
                    googleLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier.fillMaxWidth(),
                // Optional: Add a Google icon here if you like
            ) {
                Text("Continue in with Google")
            }

            TextButton(
                onClick = { navController.navigate("register") },
                enabled = authState !is AuthState.Loading // Corrected state check
            ) {
                Text("Don\'t have an account? Register")
            }
        }
    }
}
