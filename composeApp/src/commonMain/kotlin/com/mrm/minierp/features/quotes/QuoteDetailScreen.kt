package com.mrm.minierp.features.quotes

import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import com.mrm.minierp.components.ClientSelector
import com.mrm.minierp.database.ClientRepository
import com.mrm.minierp.database.QuoteRepository
import com.mrm.minierp.models.*
import kotlinx.datetime.*
import androidx.compose.ui.draw.alpha
import com.mrm.minierp.components.VerticalScrollbar

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
    val focusManager = LocalFocusManager.current
    var clients by remember { mutableStateOf(clientRepository.getAllClients()) }
    var selectedClient by remember { 
        mutableStateOf(quote?.let { q -> clients.find { it.id == q.clientId } }) 
    }
    
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var quoteDate by remember { mutableStateOf(quote?.date?.toString() ?: today.toString()) }
    var quoteNumber by remember { mutableStateOf(quote?.number ?: "") }
    
    val quoteLines = remember { 
        mutableStateListOf<QuoteLine>().apply {
            if (quote != null) {
                addAll(quote.lines)
            } else if (isEmpty()) {
                // Empezar con una línea vacía si es nuevo
                add(QuoteLine(quantity = 1.0, concept = "", unitPrice = 0.0))
            }
        }
    }

    val totalAmount = quoteLines.sumOf { it.totalWithIva }
    val subtotalAmount = quoteLines.sumOf { it.totalWithoutIva }
    val ivaBreakdown = quoteLines.groupBy { it.iva }.mapValues { (_, lines) ->
        val base = lines.sumOf { it.totalWithoutIva }
        val quota = lines.sumOf { it.ivaAmount }
        Pair(base, quota)
    }
    
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
                title = { Text(if (quote == null) "Nuevo Presupuesto" else "Editar Presupuesto", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                    val canSave = client != null && quoteNumber.isNotBlank() && quoteLines.isNotEmpty()

                    // Botón Eliminar (solo al editar)
                    if (quote != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar presupuesto",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Botón Imprimir (deshabilitado por ahora)
                    IconButton(onClick = { /* TODO: Imprimir */ }, enabled = false) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = "Imprimir presupuesto",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                        )
                    }

                    // Botón Generar Factura (deshabilitado por ahora)
                    IconButton(onClick = { /* TODO: Generar factura */ }, enabled = false) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Generar factura",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                        )
                    }

                    // Botón Guardar (icono)
                    IconButton(
                        onClick = {
                            if (client != null && quoteNumber.isNotBlank()) {
                                try {
                                    onSave(Quote(
                                        id = quote?.id ?: 0,
                                        clientId = client.id,
                                        number = quoteNumber,
                                        date = quoteDate.toLocalDate(),
                                        totalAmount = totalAmount,
                                        lines = quoteLines.toList()
                                    ))
                                } catch (e: Exception) {
                                    // Manejar error de fecha
                                }
                            }
                        },
                        enabled = canSave
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Guardar presupuesto",
                            tint = if (canSave) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    readOnly = false,
                    label = { Text("Nº Presupuesto", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) })
                )

                OutlinedTextField(
                    value = quoteDate,
                    onValueChange = { quoteDate = it },
                    label = { Text("Fecha") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Tabla de Líneas
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Encabezado de líneas
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Líneas de presupuesto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }

                // Cuerpo de la tabla con scroll mejorado
                val scrollState = rememberScrollState()
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(end = 12.dp) // Espacio para el scrollbar
                            .padding(bottom = 60.dp)
                    ) {
                        quoteLines.forEachIndexed { index, line ->
                            QuoteLineRow(
                                line = line,
                                onLineChange = { updatedLine -> 
                                    quoteLines[index] = updatedLine
                                },
                                onDelete = {
                                    quoteLines.removeAt(index)
                                }
                            )
                        }
                        
                        // Botón añadir línea principal
                        OutlinedButton(
                            onClick = { 
                                quoteLines.add(QuoteLine(quantity = 1.0, concept = "", unitPrice = 0.0))
                            },
                            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Añadir línea principal")
                        }
                    }
                    
                    // Añadir el scrollbar visual
                    VerticalScrollbar(
                        scrollState = scrollState,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
            }

            // Pie de totales
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Desglose de IVA
                    ivaBreakdown.keys.sorted().forEach { type ->
                        val amounts = ivaBreakdown[type]!!
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("IVA $type%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text("Base: ${formatCurrency(amounts.first)}", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                            Text("Cuota: ${formatCurrency(amounts.second)}", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Total SIN IVA", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(formatCurrency(subtotalAmount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("TOTAL", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text(formatCurrency(totalAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
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

@Composable
fun QuoteLineRow(
    line: QuoteLine,
    onLineChange: (QuoteLine) -> Unit,
    onDelete: () -> Unit
) {
    var showIvaMenu by remember { mutableStateOf(false) }
    val ivas = listOf(4, 10, 21)
    val localFocusManager = LocalFocusManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Fila 1: Concepto + botón eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactTextField(
                    value = line.concept,
                    onValueChange = { onLineChange(line.copy(concept = it)) },
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { localFocusManager.moveFocus(FocusDirection.Next) })
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }

            // Fila 2: Cant | IVA | Precio/u | Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Cantidad
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cant.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CompactTextField(
                        value = if (line.quantity == 0.0) "" else line.quantity.toDisplayString(),
                        onValueChange = { onLineChange(line.copy(quantity = it.replace(",", ".").toDoubleOrNull() ?: 0.0)) },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { localFocusManager.moveFocus(FocusDirection.Next) })
                    )
                }

                // Precio unitario
                Column(modifier = Modifier.weight(1f)) {
                    Text("Precio/u", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CompactTextField(
                        value = if (line.unitPrice == 0.0) "" else line.unitPrice.toDisplayString(),
                        onValueChange = { onLineChange(line.copy(unitPrice = it.replace(",", ".").toDoubleOrNull() ?: 0.0)) },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        textAlign = TextAlign.End,
                        fontSize = 12.sp,
                        suffix = "€",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { localFocusManager.moveFocus(FocusDirection.Next) })
                    )
                }

                // IVA
                Column(modifier = Modifier.weight(1f)) {
                    Text("IVA", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box {
                        CompactTextField(
                            value = "${line.iva}%",
                            onValueChange = {},
                            readOnly = true,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showIvaMenu = true })
                        DropdownMenu(
                            expanded = showIvaMenu,
                            onDismissRequest = { showIvaMenu = false }
                        ) {
                            ivas.forEach { iva ->
                                DropdownMenuItem(
                                    text = { Text("$iva%") },
                                    onClick = { onLineChange(line.copy(iva = iva)); showIvaMenu = false }
                                )
                            }
                        }
                    }
                }

                // Subtotal
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Subtotal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(line.totalWithoutIva),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Sublíneas
            if (line.sublines.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    line.sublines.forEachIndexed { sIndex, subline ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null,
                                modifier = Modifier.size(14.dp).alpha(0.5f))
                            Spacer(Modifier.width(4.dp))
                            CompactTextField(
                                value = subline,
                                onValueChange = { newValue ->
                                    val newSublines = line.sublines.toMutableList()
                                    newSublines[sIndex] = newValue
                                    onLineChange(line.copy(sublines = newSublines))
                                },
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp
                            )
                            IconButton(onClick = {
                                val newSublines = line.sublines.toMutableList()
                                newSublines.removeAt(sIndex)
                                onLineChange(line.copy(sublines = newSublines))
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Eliminar sublínea",
                                    modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            // Botón añadir sublínea
            TextButton(
                onClick = {
                    val newSublines = line.sublines.toMutableList()
                    newSublines.add("")
                    onLineChange(line.copy(sublines = newSublines))
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Añadir detalle/sublínea", fontSize = 11.sp)
            }
        } // end Column
    } // end Card
}

private fun formatCurrency(amount: Double): String {
    val integerPart = amount.toLong()
    val decimalPart = ((amount - integerPart) * 100).toLong().coerceIn(0, 99)
    return "$integerPart,${decimalPart.toString().padStart(2, '0')} €"
}

/** Muestra el Double sin ".0" para números enteros. Ej: 1.0 -> "1", 1.5 -> "1.5" */
private fun Double.toDisplayString(): String {
    return if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
}

@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    readOnly: Boolean = false,
    fontSize: TextUnit = 11.sp,
    suffix: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        textStyle = TextStyle(
            fontSize = fontSize,
            textAlign = textAlign,
            color = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    innerTextField()
                }
                if (suffix != null) {
                    Text(suffix, fontSize = fontSize * 0.9, modifier = Modifier.padding(start = 1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    )
}

