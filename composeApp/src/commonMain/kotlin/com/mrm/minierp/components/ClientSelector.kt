package com.mrm.minierp.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mrm.minierp.models.Client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientSelector(
    clients: List<Client>,
    selectedClient: Client?,
    onClientSelected: (Client) -> Unit,
    onCreateClient: () -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Cliente",
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = if (enabled) expanded else false,
                onExpandedChange = { if (enabled) expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedClient?.name ?: "Seleccionar cliente...",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    trailingIcon = { if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    enabled = enabled
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (clients.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No hay clientes") },
                            onClick = { expanded = false },
                            enabled = false
                        )
                    } else {
                        clients.forEach { client ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(client.name, fontWeight = FontWeight.Bold)
                                        if (client.taxId.isNotBlank()) {
                                            Text(client.taxId, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                },
                                onClick = {
                                    onClientSelected(client)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onCreateClient,
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear nuevo cliente")
            }
        }
    }
}
