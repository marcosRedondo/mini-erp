package com.mrm.minierp.features.clients

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mrm.minierp.database.ClientRepository
import com.mrm.minierp.models.Client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    repository: ClientRepository,
    forceRefresh: Int = 0,
    onBack: () -> Unit,
    onAddClient: () -> Unit,
    onClientClick: (Int) -> Unit
) {
    val clients = remember(forceRefresh, repository) { repository.getAllClients() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clientes", fontWeight = FontWeight.Bold) },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClient,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Cliente")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (clients.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No hay clientes",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Pulsa el botón '+' para añadir uno.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clients) { client ->
                        Card(
                            onClick = { onClientClick(client.id) },
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(client.name, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(client.taxId.ifBlank { "Sin CIF/NIF" }) },
                                leadingContent = {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (client.isVip) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                            Icon(
                                                if (client.isVip) Icons.Default.Star else Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (client.isVip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                trailingContent = {
                                    IconButton(onClick = { onClientClick(client.id) }) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Ver detalles")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
