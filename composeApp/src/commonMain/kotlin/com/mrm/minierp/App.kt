package com.mrm.minierp

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mrm.minierp.features.dashboard.DashboardScreen

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        var showUpdateDialog by remember { mutableStateOf(false) }
        var latestVersion by remember { mutableStateOf<String?>(null) }

        // Comprobar actualizaciÃ³n al iniciar
        LaunchedEffect(Unit) {
            latestVersion = UpdateManager.getLatestVersion()
            if (latestVersion != null && UpdateManager.isNewerVersion(UpdateManager.APP_VERSION, latestVersion!!)) {
                showUpdateDialog = true
            }
        }

        NavHost(
            navController = navController,
            startDestination = "dashboard"
        ) {
            composable("dashboard") {
                DashboardScreen()
            }
            // AquÃ­ aÃ±adiremos mÃ¡s rutas en el futuro
        }

        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("ActualizaciÃ³n disponible") },
                text = { Text("Hay una nueva versiÃ³n disponible (v$latestVersion). Â¿Quieres descargarla?") },
                confirmButton = {
                    Button(onClick = {
                        openUrl(UpdateManager.DOWNLOAD_URL)
                        showUpdateDialog = false
                    }) {
                        Text("Descargar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("MÃ¡s tarde")
                    }
                }
            )
        }
    }
}
