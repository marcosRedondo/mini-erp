package com.mrm.minierp.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mrm.minierp.database.SettingsManager
import com.mrm.minierp.database.CompanyRepository
import com.mrm.minierp.models.Company
import com.mrm.minierp.pickDirectory
import com.mrm.minierp.pickImageAsBase64
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class, ExperimentalResourceApi::class)
@Composable
fun SettingsScreen(
    companyRepository: CompanyRepository?,
    onStoragePathChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    var storagePath by remember { mutableStateOf(SettingsManager.storagePath ?: "No seleccionada") }
    
    // Estados del formulario de empresa
    var companyName by remember { mutableStateOf("") }
    var companyNif by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") }
    var companyLogoBase64 by remember { mutableStateOf<String?>(null) }
    
    var isInitialSetup by remember { mutableStateOf(SettingsManager.storagePath == null) }
    var hasExistingData by remember { mutableStateOf(false) }

    // Cargar datos si hay repositorio
    LaunchedEffect(companyRepository, storagePath) {
        if (companyRepository != null && storagePath != "No seleccionada") {
            val company = companyRepository.getCompany()
            companyName = company.name
            companyNif = company.nif
            companyPhone = company.phone
            companyAddress = company.address
            companyLogoBase64 = company.logoBase64
            
            if (companyName.isNotBlank()) {
                hasExistingData = true
                if (isInitialSetup) {
                    onBack() // Si ya hay datos y es el setup inicial, volvemos automáticamente
                }
            }
        }
    }

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
            
            if (storagePath != "No seleccionada") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Datos de la Empresa",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Nombre de la Empresa") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = companyNif,
                                onValueChange = { companyNif = it },
                                label = { Text("NIF / CIF") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = companyPhone,
                                onValueChange = { companyPhone = it },
                                label = { Text("Teléfono") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        
                        OutlinedTextField(
                            value = companyAddress,
                            onValueChange = { companyAddress = it },
                            label = { Text("Dirección") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        // Logo Section
                        Text("Logo de la Empresa", style = MaterialTheme.typography.labelLarge)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                if (companyLogoBase64 != null) {
                                    val imageBitmap = remember(companyLogoBase64) {
                                        try {
                                            val bytes = Base64.decode(companyLogoBase64!!)
                                            bytes.decodeToImageBitmap()
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (imageBitmap != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = imageBitmap,
                                            contentDescription = "Logo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                    }
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp).graphicsLayer(alpha = 0.3f),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            Button(onClick = {
                                pickImageAsBase64 { base64 ->
                                    if (base64 != null) {
                                        companyLogoBase64 = base64
                                    }
                                }
                            }) {
                                Text("Subir Logo")
                            }
                        }
                        
                        Button(
                            onClick = {
                                if (companyRepository != null) {
                                    companyRepository.updateCompany(
                                        Company(
                                            name = companyName,
                                            nif = companyNif,
                                            phone = companyPhone,
                                            address = companyAddress,
                                            logoBase64 = companyLogoBase64
                                        )
                                    )
                                    if (isInitialSetup) {
                                        onBack()
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            enabled = companyName.isNotBlank()
                        ) {
                            Text(if (isInitialSetup) "Finalizar Configuración" else "Guardar Datos")
                        }
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
