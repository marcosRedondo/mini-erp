package com.mrm.minierp

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mrm.minierp.features.dashboard.DashboardScreen
import com.mrm.minierp.features.clients.ClientsScreen
import com.mrm.minierp.features.clients.ClientDetailScreen
import com.mrm.minierp.features.settings.SettingsScreen
import com.mrm.minierp.database.MiniErpDatabase
import com.mrm.minierp.database.DatabaseDriverFactory
import com.mrm.minierp.database.ClientRepository
import com.mrm.minierp.database.SettingsManager

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        var showUpdateDialog by remember { mutableStateOf(false) }
        var latestVersion by remember { mutableStateOf<String?>(null) }
        
        // Estado para la ruta de almacenamiento (para reactividad)
        var currentStoragePath by remember { mutableStateOf(SettingsManager.storagePath) }
        
        // Inicializar persistencia
        val database by remember(currentStoragePath) {
            derivedStateOf {
                val driver = DatabaseDriverFactory().createDriver(currentStoragePath)
                MiniErpDatabase(driver)
            }
        }
        val repository = remember(database) { ClientRepository(database) }

        // Comprobar actualización al iniciar
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
                DashboardScreen(
                    onNavigateToClients = { navController.navigate("clients") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onStoragePathChanged = { newPath ->
                        SettingsManager.storagePath = newPath
                        currentStoragePath = newPath
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("clients") {
                ClientsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onAddClient = { navController.navigate("clients/new") }
                )
            }
            composable("clients/new") {
                ClientDetailScreen(
                    onSave = { client -> 
                        repository.saveClient(client)
                        navController.popBackStack() 
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }

        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("Actualización disponible") },
                text = { Text("Hay una nueva versión disponible (v$latestVersion). ¿Quieres descargarla?") },
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
                        Text("Más tarde")
                    }
                }
            )
        }
    }
}
