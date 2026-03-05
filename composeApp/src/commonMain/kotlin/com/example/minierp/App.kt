package com.example.minierp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch

import minierp.composeapp.generated.resources.Res
import minierp.composeapp.generated.resources.compose_multiplatform
import java.awt.Desktop
import java.net.URI

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        var showUpdateDialog by remember { mutableStateOf(false) }
        var latestVersion by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        // Comprobar actualización al iniciar
        LaunchedEffect(Unit) {
            latestVersion = UpdateManager.getLatestVersion()
            if (latestVersion != null && UpdateManager.isNewerVersion(UpdateManager.APP_VERSION, latestVersion!!)) {
                showUpdateDialog = true
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = { showContent = !showContent }) {
                    Text("Click me!")
                }
                AnimatedVisibility(showContent) {
                    val greeting = remember { Greeting().greet() }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(painterResource(Res.drawable.compose_multiplatform), null)
                        Text("Compose: $greeting")
                    }
                }
            }

            // Mostrar versión en la esquina inferior derecha
            Text(
                text = "v${UpdateManager.APP_VERSION}",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (showUpdateDialog) {
                AlertDialog(
                    onDismissRequest = { showUpdateDialog = false },
                    title = { Text("Actualización disponible") },
                    text = { Text("Hay una nueva versión disponible (v$latestVersion). ¿Quieres descargarla?") },
                    confirmButton = {
                        Button(onClick = {
                            try {
                                Desktop.getDesktop().browse(URI(UpdateManager.DOWNLOAD_URL))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
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
}
