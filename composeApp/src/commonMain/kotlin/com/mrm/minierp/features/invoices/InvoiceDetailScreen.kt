package com.mrm.minierp.features.invoices

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
import com.mrm.minierp.database.InvoiceRepository
import com.mrm.minierp.database.CompanyRepository
import com.mrm.minierp.database.QuoteRepository
import com.mrm.minierp.utils.PdfGenerator
import com.mrm.minierp.models.*
import kotlinx.datetime.*
import androidx.compose.ui.draw.alpha
import com.mrm.minierp.components.VerticalScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    clientRepository: ClientRepository,
    invoiceRepository: InvoiceRepository,
    companyRepository: CompanyRepository,
    quoteRepository: QuoteRepository,
    invoice: Invoice? = null,
    fromQuoteId: Int? = null,
    onSave: (Invoice) -> Unit,
    onCancel: () -> Unit,
    onDelete: (Invoice) -> Unit = {},
    onCreateClient: () -> Unit,
    onNavigateToQuote: (Int) -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    val pdfGenerator = remember { PdfGenerator() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    var clients by remember { mutableStateOf<List<Client>>(clientRepository.getAllClients()) }
    var selectedClient by remember { 
        mutableStateOf<Client?>(invoice?.let { i -> clients.find { it.id == i.clientId } }) 
    }
    
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var invoiceDate by remember { mutableStateOf(invoice?.date?.toString() ?: today.toString()) }
    var notes by remember { mutableStateOf(invoice?.notes ?: "") }
    var invoiceNumber by remember { mutableStateOf(invoice?.number ?: "") }
    var currentQuoteId by remember { mutableStateOf(invoice?.quoteId ?: fromQuoteId) }
    
    var nextKey by remember { mutableStateOf(0L) }
    
    val invoiceLines = remember { 
        mutableStateListOf<InvoiceLineUIState>().apply {
            if (invoice != null) {
                addAll(invoice.lines.map { 
                    InvoiceLineUIState(
                        key = nextKey++, 
                        line = it, 
                        quantityText = if (it.quantity == 0.0) "" else it.quantity.toDisplayString(), 
                        unitPriceText = if (it.unitPrice == 0.0) "" else it.unitPrice.toDisplayString()
                    ) 
                })
            } else if (isEmpty()) {
                add(InvoiceLineUIState(
                    key = nextKey++, 
                    line = InvoiceLine(quantity = 1.0, concept = "", unitPrice = 0.0), 
                    quantityText = "1", 
                    unitPriceText = ""
                ))
            }
        }
    }

    val totalAmount = invoiceLines.sumOf { it.line.totalWithIva }
    val subtotalAmount = invoiceLines.sumOf { it.line.totalWithoutIva }
    val ivaBreakdown = invoiceLines.groupBy { it.line.iva }.mapValues { (_, states) ->
        val base = states.sumOf { it.line.totalWithoutIva }
        val quota = states.sumOf { it.line.ivaAmount }
        Pair(base, quota)
    }
    
    // Cargar datos desde el presupuesto si viene de uno
    LaunchedEffect(fromQuoteId) {
        if (fromQuoteId != null && invoice == null) {
            val quote = quoteRepository.getQuoteById(fromQuoteId)
            if (quote != null) {
                selectedClient = clients.find { it.id == quote.clientId }
                notes = quote.notes
                invoiceLines.clear()
                quote.lines.forEach { qLine ->
                    invoiceLines.add(InvoiceLineUIState(
                        key = nextKey++,
                        line = InvoiceLine(
                            quantity = qLine.quantity,
                            concept = qLine.concept,
                            detail = qLine.detail,
                            sublines = qLine.sublines,
                            iva = qLine.iva,
                            unitPrice = qLine.unitPrice
                        ),
                        quantityText = qLine.quantity.toDisplayString(),
                        unitPriceText = qLine.unitPrice.toDisplayString()
                    ))
                }
            }
        }
    }
    
    LaunchedEffect(selectedClient, invoiceDate) {
        val client = selectedClient
        if (client != null && invoice == null) {
            try {
                val year = invoiceDate.toLocalDate().year
                invoiceNumber = invoiceRepository.getNextInvoiceNumber(client.id, year)
            } catch (e: Exception) {
            }
        }
    }

    var associatedQuote by remember { mutableStateOf<Quote?>(null) }
    LaunchedEffect(currentQuoteId) {
        if (currentQuoteId != null) {
            associatedQuote = quoteRepository.getQuoteById(currentQuoteId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (invoice == null) "Nueva Factura" else "Editar Factura", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                    val canSave = client != null && invoiceNumber.isNotBlank() && invoiceLines.isNotEmpty()

                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Ir al Dashboard",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    if (invoice != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar factura",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val client = selectedClient
                            if (invoice != null && client != null) {
                                val company = companyRepository.getCompany()
                                pdfGenerator.generateInvoicePdf(company, client, invoice)
                            }
                        },
                        enabled = invoice != null && selectedClient != null
                    ) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = "Imprimir factura",
                            tint = if (invoice != null && selectedClient != null) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (client != null && invoiceNumber.isNotBlank()) {
                                try {
                                    onSave(Invoice(
                                        id = invoice?.id ?: 0,
                                        clientId = client.id,
                                        quoteId = currentQuoteId,
                                        number = invoiceNumber,
                                        date = invoiceDate.toLocalDate(),
                                        totalAmount = totalAmount,
                                        notes = notes,
                                        lines = invoiceLines.map { it.line }
                                    ))
                                } catch (e: Exception) {
                                }
                            }
                        },
                        enabled = canSave
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Guardar factura",
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
            if (currentQuoteId != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable {
                        onNavigateToQuote(currentQuoteId!!)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Factura asociada a presupuesto.",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            associatedQuote?.let {
                                Text(
                                    "Presupuesto Nº ${it.number}",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = "Ver presupuesto",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            ClientSelector(
                clients = clients,
                selectedClient = selectedClient,
                onClientSelected = { selectedClient = it },
                onCreateClient = onCreateClient,
                enabled = invoice == null
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    readOnly = false,
                    label = { Text("Nº Factura", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                    value = invoiceDate,
                    onValueChange = { invoiceDate = it },
                    label = { Text("Fecha") },
                    modifier = Modifier.weight(1.5f),
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) })
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Líneas de factura", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }

                val scrollState = rememberScrollState()
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(end = 12.dp)
                            .padding(bottom = 60.dp)
                    ) {
                        invoiceLines.forEachIndexed { index, state ->
                            key(state.key) {
                                InvoiceLineRow(
                                    line = state.line,
                                    quantityText = state.quantityText,
                                    unitPriceText = state.unitPriceText,
                                    onLineChange = { updatedLine, newQuantityText, newUnitPriceText -> 
                                        invoiceLines[index] = InvoiceLineUIState(state.key, updatedLine, newQuantityText, newUnitPriceText)
                                    },
                                    onDelete = {
                                        invoiceLines.removeAt(index)
                                    }
                                )
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                invoiceLines.add(
                                    InvoiceLineUIState(
                                        key = nextKey++, 
                                        line = InvoiceLine(quantity = 1.0, concept = "", unitPrice = 0.0), 
                                        quantityText = "1", 
                                        unitPriceText = ""
                                    )
                                )
                            },
                            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Añadir línea principal")
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Observaciones") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp).padding(bottom = 16.dp),
                            placeholder = { Text("Añade cualquier observación o condición especial en la factura...") },
                            maxLines = 10
                        )
                    }
                    
                    VerticalScrollbar(
                        scrollState = scrollState,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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

        if (showDeleteDialog && invoice != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar Factura") },
                text = { Text("¿Estás seguro de que quieres eliminar la factura ${invoice.number}? Esta acción no se puede deshacer.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDelete(invoice)
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
fun InvoiceLineRow(
    line: InvoiceLine,
    quantityText: String,
    unitPriceText: String,
    onLineChange: (InvoiceLine, String, String) -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactTextField(
                    value = line.concept,
                    onValueChange = { onLineChange(line.copy(concept = it), quantityText, unitPriceText) },
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { localFocusManager.moveFocus(FocusDirection.Next) })
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }

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
                                    onLineChange(line.copy(sublines = newSublines), quantityText, unitPriceText)
                                },
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp
                            )
                            IconButton(onClick = {
                                val newSublines = line.sublines.toMutableList()
                                newSublines.removeAt(sIndex)
                                onLineChange(line.copy(sublines = newSublines), quantityText, unitPriceText)
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Eliminar sublínea",
                                    modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = {
                    val newSublines = line.sublines.toMutableList()
                    newSublines.add("")
                    onLineChange(line.copy(sublines = newSublines), quantityText, unitPriceText)
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Añadir detalle/sublínea", fontSize = 11.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cant.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CompactTextField(
                        value = quantityText,
                        onValueChange = { 
                            val parsed = it.replace(",", ".").toDoubleOrNull() ?: 0.0
                            onLineChange(line.copy(quantity = parsed), it, unitPriceText) 
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { localFocusManager.moveFocus(FocusDirection.Next) })
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Precio/u", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CompactTextField(
                        value = unitPriceText,
                        onValueChange = { 
                            val parsed = it.replace(",", ".").toDoubleOrNull() ?: 0.0
                            onLineChange(line.copy(unitPrice = parsed), quantityText, it) 
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        textAlign = TextAlign.End,
                        fontSize = 12.sp,
                        suffix = "€",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { localFocusManager.moveFocus(FocusDirection.Next) })
                    )
                }

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
                                    onClick = { onLineChange(line.copy(iva = iva), quantityText, unitPriceText); showIvaMenu = false }
                                )
                            }
                        }
                    }
                }

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
        } 
    } 
}

private fun formatCurrency(amount: Double): String {
    val integerPart = amount.toLong()
    val decimalPart = ((amount - integerPart) * 100).toLong().coerceIn(0, 99)
    return "$integerPart,${decimalPart.toString().padStart(2, '0')} €"
}

private fun Double.toDisplayString(): String {
    return if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
}

data class InvoiceLineUIState(
    val key: Long,
    val line: InvoiceLine,
    val quantityText: String,
    val unitPriceText: String
)

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
