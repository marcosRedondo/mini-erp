package com.mrm.minierp.features.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mrm.minierp.UpdateManager

@Composable
fun DashboardScreen(
    onNavigateToClients: () -> Unit,
    onNavigateToQuotes: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("MiniERP - Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Bienvenido de nuevo",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.widthIn(max = 800.dp).weight(1f)
                ) {
                    item { DashboardButton("Clientes", Icons.Default.Person, true, onNavigateToClients) }
                    item { DashboardButton("Presupuestos", Icons.Default.Description, true, onNavigateToQuotes) }
                    item { DashboardButton("Facturas", Icons.Default.Receipt, true, onNavigateToInvoices) }
                    item { DashboardButton("Configuración", Icons.Default.Settings, true, onNavigateToSettings) }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Área para futuras estadísticas

            }

            // Versión en la esquina inferior
            Text(
                text = "v${UpdateManager.APP_VERSION}",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun DashboardButton(text: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(120.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(text, fontWeight = FontWeight.Medium)
        }
    }
}
