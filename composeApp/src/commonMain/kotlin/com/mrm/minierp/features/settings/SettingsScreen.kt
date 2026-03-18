package com.mrm.minierp.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mrm.minierp.database.SettingsManager
import com.mrm.minierp.pickDirectory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onStoragePathChanged: (String) -> Unit, onBack: () -> Unit) {
    var storagePath by remember { mutableStateOf(SettingsManager.storagePath ?: "No seleccionada") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Almacenamiento de Datos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Selecciona la carpeta donde se guardará la base de datos (puedes elegir una carpeta sincronizada con Google Drive).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = storagePath,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Button(
                        onClick = {
                            pickDirectory { path ->
                                onStoragePathChanged(path)
                                storagePath = path
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cambiar Carpeta")
                    }
                }
            }
            
            Text(
                "Nota: Si cambias la carpeta, la aplicación usará la base de datos que se encuentre en la nueva ubicación o creará una nueva si no existe.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
