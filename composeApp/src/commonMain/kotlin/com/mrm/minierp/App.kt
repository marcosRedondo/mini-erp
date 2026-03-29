package com.mrm.minierp

import androidx.compose.material3.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mrm.minierp.features.dashboard.DashboardScreen
import com.mrm.minierp.features.clients.ClientsScreen
import com.mrm.minierp.features.clients.ClientDetailScreen
import com.mrm.minierp.features.settings.SettingsScreen
import com.mrm.minierp.features.quotes.QuotesScreen
import com.mrm.minierp.features.quotes.QuoteDetailScreen
import com.mrm.minierp.database.MiniErpDatabase
import com.mrm.minierp.database.DatabaseDriverFactory
import com.mrm.minierp.database.ClientRepository
import com.mrm.minierp.database.QuoteRepository
import com.mrm.minierp.database.InvoiceRepository
import com.mrm.minierp.database.SettingsManager
import com.mrm.minierp.database.CompanyRepository
import com.mrm.minierp.features.invoices.InvoicesScreen
import com.mrm.minierp.features.invoices.InvoicesScreen
import com.mrm.minierp.features.invoices.InvoiceDetailScreen

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        var showUpdateDialog by remember { mutableStateOf(false) }
        var latestVersion by remember { mutableStateOf<String?>(null) }
        var forceRefresh by remember { mutableStateOf(0) }
        
        // Estado para la ruta de almacenamiento (para reactividad)
        var currentStoragePath by remember { mutableStateOf(SettingsManager.storagePath) }
        
        // Inicializar persistencia
        val database by remember(currentStoragePath) {
            derivedStateOf {
                val driver = DatabaseDriverFactory().createDriver(currentStoragePath)
                MiniErpDatabase(driver)
            }
        }
        val clientRepository = remember(database) { ClientRepository(database) }
        val quoteRepository = remember(database) { QuoteRepository(database) }
        val invoiceRepository = remember(database) { InvoiceRepository(database) }
        val companyRepository = remember(database) { CompanyRepository(database) }

        // Comprobar actualización al iniciar
        LaunchedEffect(Unit) {
            latestVersion = UpdateManager.getLatestVersion()
            if (latestVersion != null && UpdateManager.isNewerVersion(UpdateManager.APP_VERSION, latestVersion!!)) {
                showUpdateDialog = true
            }
        }

        val scope = rememberCoroutineScope()
        val startDest = if (currentStoragePath == null) "settings" else "dashboard"

        val onNavigateToDashboard = { 
            navController.navigate("dashboard") { 
                popUpTo("dashboard") { inclusive = true } 
            } 
        }

        NavHost(
            navController = navController,
            startDestination = startDest
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onNavigateToClients = { navController.navigate("clients") },
                    onNavigateToQuotes = { navController.navigate("quotes") },
                    onNavigateToInvoices = { navController.navigate("invoices") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("quotes") {
                QuotesScreen(
                    quoteRepository = quoteRepository,
                    clientRepository = clientRepository,
                    onBack = { navController.popBackStack() },
                    onAddQuote = { navController.navigate("quotes/new") },
                    onEditQuote = { quoteId -> navController.navigate("quotes/$quoteId") }
                )
            }
            composable("quotes/new") {
                QuoteDetailScreen(
                    clientRepository = clientRepository,
                    quoteRepository = quoteRepository,
                    companyRepository = companyRepository,
                    onSave = { quote ->
                        scope.launch {
                            quoteRepository.saveQuote(quote)
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() },
                    onCreateClient = { navController.navigate("clients/new") },
                    invoiceRepository = invoiceRepository,
                    onNavigateToInvoice = { id -> navController.navigate("invoices/$id") },
                    onNavigateToDashboard = onNavigateToDashboard
                )
            }
            composable("quotes/{quoteId}") { backStackEntry ->
                val quoteId = backStackEntry.arguments?.getString("quoteId")?.toIntOrNull()
                val quote = quoteId?.let { quoteRepository.getQuoteById(it) }
                QuoteDetailScreen(
                    clientRepository = clientRepository,
                    quoteRepository = quoteRepository,
                    companyRepository = companyRepository,
                    quote = quote,
                    onSave = { updatedQuote ->
                        scope.launch {
                            quoteRepository.saveQuote(updatedQuote)
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() },
                    onDelete = { quoteToDelete ->
                        scope.launch {
                            quoteRepository.deleteQuote(quoteToDelete.id)
                            navController.popBackStack()
                        }
                    },
                    onCreateClient = { navController.navigate("clients/new") },
                    onGenerateInvoice = { quoteId -> navController.navigate("invoices/new/$quoteId") },
                    invoiceRepository = invoiceRepository,
                    onNavigateToInvoice = { id -> navController.navigate("invoices/$id") },
                    onNavigateToDashboard = onNavigateToDashboard
                )
            }
            composable("invoices") {
                InvoicesScreen(
                    invoiceRepository = invoiceRepository,
                    clientRepository = clientRepository,
                    onBack = { navController.popBackStack() },
                    onAddInvoice = { navController.navigate("invoices/new") },
                    onEditInvoice = { invoiceId -> navController.navigate("invoices/$invoiceId") }
                )
            }
            composable("invoices/new") {
                InvoiceDetailScreen(
                    clientRepository = clientRepository,
                    invoiceRepository = invoiceRepository,
                    companyRepository = companyRepository,
                    quoteRepository = quoteRepository,
                    onSave = { invoice ->
                        scope.launch {
                            invoiceRepository.saveInvoice(invoice)
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() },
                    onCreateClient = { navController.navigate("clients/new") },
                    onNavigateToQuote = { quoteId -> navController.navigate("quotes/$quoteId") },
                    onNavigateToDashboard = onNavigateToDashboard
                )
            }
            composable("invoices/new/{fromQuoteId}") { backStackEntry ->
                val fromQuoteId = backStackEntry.arguments?.getString("fromQuoteId")?.toIntOrNull()
                InvoiceDetailScreen(
                    clientRepository = clientRepository,
                    invoiceRepository = invoiceRepository,
                    companyRepository = companyRepository,
                    quoteRepository = quoteRepository,
                    fromQuoteId = fromQuoteId,
                    onSave = { invoice ->
                        scope.launch {
                            invoiceRepository.saveInvoice(invoice)
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() },
                    onCreateClient = { navController.navigate("clients/new") },
                    onNavigateToQuote = { quoteId -> navController.navigate("quotes/$quoteId") },
                    onNavigateToDashboard = onNavigateToDashboard
                )
            }
            composable("invoices/{invoiceId}") { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getString("invoiceId")?.toIntOrNull()
                val invoice = invoiceId?.let { invoiceRepository.getInvoiceById(it) }
                InvoiceDetailScreen(
                    clientRepository = clientRepository,
                    invoiceRepository = invoiceRepository,
                    companyRepository = companyRepository,
                    quoteRepository = quoteRepository,
                    invoice = invoice,
                    onSave = { updatedInvoice ->
                        scope.launch {
                            invoiceRepository.saveInvoice(updatedInvoice)
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() },
                    onDelete = { invoiceToDelete ->
                        scope.launch {
                            invoiceRepository.deleteInvoice(invoiceToDelete.id)
                            navController.popBackStack()
                        }
                    },
                    onCreateClient = { navController.navigate("clients/new") },
                    onNavigateToQuote = { quoteId -> navController.navigate("quotes/$quoteId") },
                    onNavigateToDashboard = onNavigateToDashboard
                )
            }
            composable("settings") {
                SettingsScreen(
                    companyRepository = companyRepository,
                    onStoragePathChanged = { newPath ->
                        SettingsManager.storagePath = newPath
                        currentStoragePath = newPath
                    },
                    onBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate("dashboard") {
                                popUpTo("settings") { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable("clients") {
                ClientsScreen(
                    repository = clientRepository,
                    forceRefresh = forceRefresh,
                    onBack = { navController.popBackStack() },
                    onAddClient = { navController.navigate("clients/new") },
                    onClientClick = { id -> navController.navigate("clients/$id") }
                )
            }
            composable("clients/{id}") { backStackEntry ->
                val idStr = backStackEntry.arguments?.getString("id")
                if (idStr == "new") {
                    ClientDetailScreen(
                        onSave = { client -> 
                            scope.launch {
                                clientRepository.saveClient(client)
                                forceRefresh++
                                navController.popBackStack() 
                            }
                        },
                        onCancel = { navController.popBackStack() },
                        onNavigateToDashboard = onNavigateToDashboard
                    )
                } else {
                    val id = idStr?.toIntOrNull()
                    if (id != null) {
                        val client = remember(id) { clientRepository.getClientById(id) }
                        if (client != null) {
                            ClientDetailScreen(
                                client = client,
                                onSave = { updatedClient -> 
                                    clientRepository.updateClient(updatedClient)
                                    forceRefresh++
                                    navController.popBackStack() 
                                },
                                onCancel = { navController.popBackStack() },
                                onDelete = { clientToDelete ->
                                    clientRepository.deleteClient(clientToDelete.id)
                                    forceRefresh++
                                    navController.popBackStack()
                                },
                                onNavigateToDashboard = onNavigateToDashboard
                            )
                        } else {
                            // Si por algún motivo no se encontró el cliente, no hacer un popBackStack
                            // directamente en el bloque de composición, se usa un LaunchedEffect.
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                        }
                    }
                }
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
