package com.mrm.minierp.features.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mrm.minierp.models.Client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    client: Client? = null,
    onSave: (Client) -> Unit,
    onCancel: () -> Unit,
    onDelete: (Client) -> Unit = {}
) {
    var name by remember { mutableStateOf(client?.name ?: "") }
    var taxId by remember { mutableStateOf(client?.taxId ?: "") }
    var address by remember { mutableStateOf(client?.address ?: "") }
    var phone by remember { mutableStateOf(client?.phone ?: "") }
    var email by remember { mutableStateOf(client?.email ?: "") }
    var notes by remember { mutableStateOf(client?.notes ?: "") }
    var isVip by remember { mutableStateOf(client?.isVip ?: false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isEmailValid = email.isEmpty() || Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$").matches(email)
    val isFormValid = name.isNotBlank() && isEmailValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (client == null) "Nuevo Cliente" else "Editar Cliente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                },
                actions = {
                    if (client != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar Cliente", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    TextButton(
                        onClick = {
                            if (isFormValid) {
                                onSave(Client(client?.id ?: 0, name, taxId, address, phone, email, notes, isVip))
                            }
                        },
                        enabled = isFormValid
                    ) {
                        Text("GUARDAR", color = if (isFormValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ClientTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre *",
                icon = Icons.Default.Person,
                placeholder = "Nombre de la empresa o contacto"
            )

            ClientTextField(
                value = taxId,
                onValueChange = { taxId = it },
                label = "CIF/NIF",
                icon = Icons.Default.Badge,
                placeholder = "Ej: B12345678"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ClientTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Teléfono",
                    icon = Icons.Default.Phone,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Phone
                )
                ClientTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    icon = Icons.Default.Email,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Email,
                    isError = !isEmailValid,
                    errorMessage = if (!isEmailValid) "Email inválido" else null
                )
            }

            ClientTextField(
                value = address,
                onValueChange = { address = it },
                label = "Dirección",
                icon = Icons.Default.LocationOn,
                placeholder = "Calle, número, CP y ciudad"
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isVip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Cliente VIP", fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = isVip,
                    onCheckedChange = { isVip = it }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showDeleteDialog && client != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar Cliente") },
                text = { Text("¿Estás seguro de que quieres eliminar a ${client.name}? Esta acción no se puede deshacer.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDelete(client)
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

@Composable
fun ClientTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = { Icon(icon, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            isError = isError
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
