package com.mrm.minierp.features.quotes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mrm.minierp.components.ClientSelector
import com.mrm.minierp.database.ClientRepository
import com.mrm.minierp.database.QuoteRepository
import com.mrm.minierp.models.Client
import com.mrm.minierp.models.Quote
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
    clientRepository: ClientRepository,
    quoteRepository: QuoteRepository,
    quote: Quote? = null,
    onSave: (Quote) -> Unit,
    onCancel: () -> Unit,
    onDelete: (Quote) -> Unit = {},
    onCreateClient: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var clients by remember { mutableStateOf(clientRepository.getAllClients()) }
    var selectedClient by remember { 
        mutableStateOf(quote?.let { q -> clients.find { it.id == q.clientId } }) 
    }
    
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var quoteDate by remember { mutableStateOf(quote?.date?.toString() ?: today.toString()) }
    var quoteNumber by remember { mutableStateOf(quote?.number ?: "") }
    
    // Observar si se crea un cliente nuevo reactivamente desde el repositorio
    LaunchedEffect(selectedClient, quoteDate) {
        val client = selectedClient
        if (client != null) {
            try {
                val year = quoteDate.toLocalDate().year
                quoteNumber = quoteRepository.getNextQuoteNumber(client.id, year)
            } catch (e: Exception) {
                // Si la fecha no es válida todavía, no actualizamos el número
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (quote == null) "Nuevo Presupuesto" else "Editar Presupuesto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    val client = selectedClient
                    
                    // Botón Imprimir (deshabilitado por ahora)
                    IconButton(onClick = { /* TODO: Imprimir */ }, enabled = false) {
                        Icon(
                            Icons.Default.Print, 
                            contentDescription = "Imprimir presupuesto",
                            tint = if (false) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
                    }

                    if (quote != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = "Eliminar presupuesto",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            if (client != null && quoteNumber.isNotBlank()) {
                                try {
                                    onSave(Quote(
                                        id = quote?.id ?: 0,
                                        clientId = client.id,
                                        number = quoteNumber,
                                        date = quoteDate.toLocalDate()
                                    ))
                                } catch (e: Exception) {
                                    // Manejar error de fecha
                                }
                            }
                        },
                        enabled = client != null && quoteNumber.isNotBlank()
                    ) {
                        Text(
                            "GUARDAR", 
                            color = if (client != null && quoteNumber.isNotBlank()) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Seccion 1: Cliente
            ClientSelector(
                clients = clients,
                selectedClient = selectedClient,
                onClientSelected = { selectedClient = it },
                onCreateClient = onCreateClient,
                enabled = quote == null // Bloqueado si estamos editando
            )

            // Seccion 2: Numero y Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = quoteNumber,
                    onValueChange = { quoteNumber = it },
                    readOnly = false, // Ahora es editable por si el usuario necesita cambiarlo
                    label = { Text("Número (YYYY-NNNN)") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                OutlinedTextField(
                    value = quoteDate,
                    onValueChange = { quoteDate = it },
                    label = { Text("Fecha (ISO)") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    placeholder = { Text("YYYY-MM-DD") }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Aquí irá la tabla de datos en el futuro
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Próximamente: Tabla de artículos y servicios",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (showDeleteDialog && quote != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar Presupuesto") },
                text = { Text("¿Estás seguro de que quieres eliminar el presupuesto ${quote.number}? Esta acción no se puede deshacer.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDelete(quote)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
