package com.mrm.minierp.features.quotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import com.mrm.minierp.components.VerticalScrollbar
import com.mrm.minierp.database.QuoteRepository
import com.mrm.minierp.database.ClientRepository
import com.mrm.minierp.models.QuoteWithClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesScreen(
    quoteRepository: QuoteRepository,
    clientRepository: ClientRepository,
    onBack: () -> Unit,
    onAddQuote: () -> Unit,
    onEditQuote: (Int) -> Unit
) {
    val pageSize = 10
    val quotes = remember { mutableStateListOf<QuoteWithClient>() }
    val selectedClientIds = remember { mutableStateListOf<Int>() }
    var numberSearchQuery by remember { mutableStateOf("") }
    var contentSearchQuery by remember { mutableStateOf("") }
    
    var offset by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Función para cargar la siguiente página
    fun loadNextPage() {
        if (isLoading || !hasMore) return
        isLoading = true
        
        val newQuotes = quoteRepository.getQuotesAdvancedPaged(
            clientIds = selectedClientIds.toList(),
            numberSearch = numberSearchQuery,
            contentSearch = contentSearchQuery,
            limit = pageSize,
            offset = offset
        )
        
        if (newQuotes.size < pageSize) {
            hasMore = false
        }
        
        quotes.addAll(newQuotes)
        offset += newQuotes.size
        isLoading = false
    }
    
    // Reiniciar y cargar
    fun resetAndLoad() {
        quotes.clear()
        offset = 0
        hasMore = true
        loadNextPage()
    }
    
    // Cargar al inicio y cuando cambien los filtros
    LaunchedEffect(selectedClientIds.toList(), numberSearchQuery, contentSearchQuery) {
        resetAndLoad()
    }
    
    // Detectar scroll al final
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItemIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItemsCount = lazyListState.layoutInfo.totalItemsCount
            hasMore && !isLoading && lastVisibleItemIndex >= totalItemsCount - 2
        }
    }
    
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            loadNextPage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            Icons.Default.FilterList, 
                            contentDescription = "Filtrar por cliente",
                            tint = if (selectedClientIds.isNotEmpty()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddQuote,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Presupuesto")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Sección de Búsqueda
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = numberSearchQuery,
                    onValueChange = { numberSearchQuery = it },
                    label = { Text("ID", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (numberSearchQuery.isNotEmpty()) {
                        { IconButton(onClick = { numberSearchQuery = "" }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)) } }
                    } else null,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                )
                OutlinedTextField(
                    value = contentSearchQuery,
                    onValueChange = { contentSearchQuery = it },
                    label = { Text("Contenido / Detalle", fontSize = 12.sp) },
                    modifier = Modifier.weight(1.5f),
                    leadingIcon = { Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (contentSearchQuery.isNotEmpty()) {
                        { IconButton(onClick = { contentSearchQuery = "" }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)) } }
                    } else null,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                )
            }

            // Fila de Filtros Activos
            if (selectedClientIds.isNotEmpty()) {
                val clients = remember(selectedClientIds.toList()) { 
                    selectedClientIds.mapNotNull { id -> clientRepository.getClientById(id) } 
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtros:", style = MaterialTheme.typography.labelMedium)
                    clients.forEach { client ->
                        InputChip(
                            selected = true,
                            onClick = { selectedClientIds.remove(client.id) },
                            label = { Text(client.name, fontSize = 10.sp) },
                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                    TextButton(onClick = { selectedClientIds.clear() }) {
                        Text("Limpiar", fontSize = 10.sp)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            if (quotes.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (selectedClientIds.isEmpty()) "No hay presupuestos recientes" else "No hay resultados para estos filtros",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(quotes) { item ->
                            QuoteItem(
                                item = item,
                                onClick = { onEditQuote(item.quote.id) }
                            )
                        }
                        
                        // Indicador de carga al final
                        if (isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        
                        // Espacio extra al final
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                    
                    VerticalScrollbar(
                        lazyListState = lazyListState,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
            }
        }
    }

    if (showFilterDialog) {
        ClientFilterDialog(
            clientRepository = clientRepository,
            selectedIds = selectedClientIds.toList(),
            onDismiss = { showFilterDialog = false },
            onFiltersChanged = { newIds ->
                selectedClientIds.clear()
                selectedClientIds.addAll(newIds)
                showFilterDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFilterDialog(
    clientRepository: com.mrm.minierp.database.ClientRepository,
    selectedIds: List<Int>,
    onDismiss: () -> Unit,
    onFiltersChanged: (List<Int>) -> Unit
) {
    val clients = remember { clientRepository.getAllClients() }
    val currentSelected = remember { mutableStateListOf<Int>().apply { addAll(selectedIds) } }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrar por Clientes") },
        text = {
            if (clients.isEmpty()) {
                Text("No hay clientes registrados.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(clients) { client ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (currentSelected.contains(client.id)) {
                                        currentSelected.remove(client.id)
                                    } else {
                                        currentSelected.add(client.id)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = currentSelected.contains(client.id),
                                onCheckedChange = { checked ->
                                    if (checked) currentSelected.add(client.id)
                                    else currentSelected.remove(client.id)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(client.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onFiltersChanged(currentSelected.toList()) }) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun QuoteItem(
    item: QuoteWithClient,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BLOQUE 1: Izquierda (Número y Cliente) - Ancho fijo para consistencia
            Column(modifier = Modifier.width(180.dp)) {
                Text(
                    text = item.quote.number,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // BLOQUE 2: Centro (Detalle del presupuesto) - Toma el resto del espacio
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalAlignment = Alignment.Start
            ) {
                item.quote.lines.take(3).forEach { line ->
                    Text(
                        text = "${line.quantity.toInt()}x ${line.concept} ${line.unitPrice}€",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.quote.lines.size > 3) {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // BLOQUE 3: Derecha (Fecha e Icono) - Ancho fijo
            Column(
                modifier = Modifier.width(120.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = item.quote.date.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
