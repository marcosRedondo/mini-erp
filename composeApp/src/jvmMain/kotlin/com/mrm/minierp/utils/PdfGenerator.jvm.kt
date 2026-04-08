package com.mrm.minierp.utils

import com.lowagie.text.*
import com.lowagie.text.pdf.*
import com.mrm.minierp.models.Client
import com.mrm.minierp.models.Company
import com.mrm.minierp.models.Invoice
import com.mrm.minierp.models.Quote
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.awt.Desktop
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class PdfGenerator actual constructor() {

    @OptIn(ExperimentalEncodingApi::class)
    actual fun generateInvoicePdf(company: Company, client: Client, invoice: Invoice) {
        generatePdf(
            title = "FACTURA",
            number = invoice.number,
            date = invoice.date.toString(),
            company = company,
            client = client,
            lines = invoice.lines.map { line ->
                PdfLine(line.concept, line.quantity, line.unitPrice, line.iva, line.totalWithoutIva, line.sublines)
            },
            totalAmount = invoice.totalAmount,
            subtotalAmount = invoice.lines.sumOf { it.totalWithoutIva },
            ivaBreakdown = invoice.lines.groupBy { it.iva }.mapValues { (_, lines) ->
                val base = lines.sumOf { it.totalWithoutIva }
                val quota = lines.sumOf { it.ivaAmount }
                Pair(base, quota)
            },
            notes = invoice.notes,
            expirationDate = null
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    actual fun generateQuotePdf(company: Company, client: Client, quote: Quote) {
        generatePdf(
            title = "PRESUPUESTO",
            number = quote.number,
            date = quote.date.toString(),
            company = company,
            client = client,
            lines = quote.lines.map { line ->
                PdfLine(line.concept, line.quantity, line.unitPrice, line.iva, line.totalWithoutIva, line.sublines)
            },
            totalAmount = quote.totalAmount,
            subtotalAmount = quote.lines.sumOf { it.totalWithoutIva },
            ivaBreakdown = quote.lines.groupBy { it.iva }.mapValues { (_, lines) ->
                val base = lines.sumOf { it.totalWithoutIva }
                val quota = lines.sumOf { it.ivaAmount }
                Pair(base, quota)
            },
            notes = quote.notes,
            expirationDate = quote.expirationDate?.toString()
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generatePdf(
        title: String,
        number: String,
        date: String,
        company: Company,
        client: Client,
        lines: List<PdfLine>,
        totalAmount: Double,
        subtotalAmount: Double,
        ivaBreakdown: Map<Int, Pair<Double, Double>>,
        notes: String,
        expirationDate: String?
    ) {
        val tempFile = File.createTempFile("documento_${number}_", ".pdf")
        val document = Document(PageSize.A4, 36f, 36f, 180f, 36f)
        val writer = PdfWriter.getInstance(document, FileOutputStream(tempFile))
        
        val event = HeaderFooterEvent(company, client, title, number, date, expirationDate)
        writer.pageEvent = event
        
        document.open()
        
        // Tabla de líneas
        val table = PdfPTable(5).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(45f, 10f, 15f, 10f, 20f))
            headerRows = 1
        }
        
        // Cabeceras de tabla
        listOf("Concepto", "Cant.", "Precio/u", "IVA", "Total").forEach { header ->
            table.addCell(PdfPCell(Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f))).apply {
                backgroundColor = java.awt.Color.LIGHT_GRAY
                horizontalAlignment = Element.ALIGN_CENTER
                paddingBottom = 5f
            })
        }
        
        // Contenido de la tabla
        lines.forEach { line ->
            // Celda de concepto con sublíneas
            val conceptCell = PdfPCell().apply {
                paddingLeft = 5f
                paddingRight = 5f
                paddingTop = 3f
                paddingBottom = 5f
                
                addElement(Phrase(line.concept, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)))
                line.sublines.forEach { subline ->
                    if (subline.isNotBlank()) {
                        addElement(Paragraph("  • $subline", FontFactory.getFont(FontFactory.HELVETICA, 8f)).apply { 
                            multipliedLeading = 0.8f 
                        })
                    }
                }
            }
            table.addCell(conceptCell)
            table.addCell(PdfPCell(Phrase(line.quantity.toDisplayString(), FontFactory.getFont(FontFactory.HELVETICA, 10f))).apply { 
                horizontalAlignment = Element.ALIGN_CENTER 
                paddingTop = 3f
                paddingBottom = 3f
            })
            table.addCell(PdfPCell(Phrase(formatCurrency(line.unitPrice), FontFactory.getFont(FontFactory.HELVETICA, 10f))).apply { 
                horizontalAlignment = Element.ALIGN_RIGHT 
                paddingLeft = 5f
                paddingRight = 5f
                paddingTop = 3f
                paddingBottom = 3f
            })
            table.addCell(PdfPCell(Phrase("${line.iva}%", FontFactory.getFont(FontFactory.HELVETICA, 10f))).apply { 
                horizontalAlignment = Element.ALIGN_CENTER 
                paddingTop = 3f
                paddingBottom = 3f
            })
            table.addCell(PdfPCell(Phrase(formatCurrency(line.total), FontFactory.getFont(FontFactory.HELVETICA, 10f))).apply { 
                horizontalAlignment = Element.ALIGN_RIGHT 
                paddingLeft = 5f
                paddingRight = 5f
                paddingTop = 3f
                paddingBottom = 3f
            })
        }
        
        document.add(table)
        
        // Totales al final
        document.add(Paragraph("\n"))
        val totalsTable = PdfPTable(4).apply {
            widthPercentage = 100f
            setSpacingBefore(10f)
            setWidths(floatArrayOf(40f, 20f, 20f, 20f))
        }
        
        // Cabecera de totales
        listOf("Observaciones", "Base Imponible", "Cuota IVA", "Importe").forEach { header ->
            totalsTable.addCell(PdfPCell(Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f))).apply {
                backgroundColor = java.awt.Color.LIGHT_GRAY
                horizontalAlignment = Element.ALIGN_CENTER
            })
        }
        
        // Observaciones (celda combinada)
        val notesCell = PdfPCell(Phrase(notes, FontFactory.getFont(FontFactory.HELVETICA, 8f))).apply {
            rowspan = ivaBreakdown.size + 2
            setPadding(5f)
        }
        totalsTable.addCell(notesCell)
        
        ivaBreakdown.keys.sorted().forEach { iva ->
            val amounts = ivaBreakdown[iva]!!
            totalsTable.addCell(PdfPCell(Phrase(formatCurrency(amounts.first), FontFactory.getFont(FontFactory.HELVETICA, 9f))).apply { horizontalAlignment = Element.ALIGN_RIGHT })
            totalsTable.addCell(PdfPCell(Phrase("$iva%", FontFactory.getFont(FontFactory.HELVETICA, 9f))).apply { horizontalAlignment = Element.ALIGN_CENTER })
            totalsTable.addCell(PdfPCell(Phrase(formatCurrency(amounts.second), FontFactory.getFont(FontFactory.HELVETICA, 9f))).apply { horizontalAlignment = Element.ALIGN_RIGHT })
        }
        
        // Fila final de Total
        totalsTable.addCell(PdfPCell(Phrase("TOTAL EUR:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f))).apply { 
            colspan = 2
            horizontalAlignment = Element.ALIGN_RIGHT
            backgroundColor = java.awt.Color.WHITE
            paddingTop = 5f
            paddingBottom = 5f
        })
        totalsTable.addCell(PdfPCell(Phrase(formatCurrency(totalAmount), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, Font.BOLD, java.awt.Color.BLUE))).apply { 
            horizontalAlignment = Element.ALIGN_RIGHT
            backgroundColor = java.awt.Color.WHITE
            paddingTop = 5f
            paddingBottom = 5f
        })
        
        document.add(totalsTable)
        
        document.close()
        
        // Abrir el PDF
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(tempFile)
        }
    }

    private fun formatCurrency(amount: Double): String {
        return "%,.2f €".format(Locale.getDefault(), amount)
    }

    private fun Double.toDisplayString(): String {
        return if (this % 1.0 == 0.0) this.toLong().toString() else "%.2f".format(Locale.getDefault(), this)
    }
}

private class PdfLine(
    val concept: String,
    val quantity: Double,
    val unitPrice: Double,
    val iva: Int,
    val total: Double,
    val sublines: List<String> = emptyList()
)

private class HeaderFooterEvent(
    val company: Company,
    val client: Client,
    val docTitle: String,
    val docNumber: String,
    val docDate: String,
    val expirationDate: String?
) : PdfPageEventHelper() {

    @OptIn(ExperimentalEncodingApi::class)
    override fun onEndPage(writer: PdfWriter, document: Document) {
        val cb = writer.directContent
        
        // Tabla de cabecera que se repite
        val headerTable = PdfPTable(2).apply {
            totalWidth = document.right() - document.left()
            setWidths(floatArrayOf(50f, 50f))
        }
        
        // Columna Izquierda: Empresa
        val companyCell = PdfPCell().apply {
            border = Rectangle.NO_BORDER
            // Logo
            company.logoBase64?.let { base64 ->
                try {
                    val bytes = Base64.decode(base64)
                    val img = Image.getInstance(bytes)
                    img.scaleToFit(100f, 50f)
                    addElement(img)
                } catch (e: Exception) {}
            }
            addElement(Paragraph(company.name, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f)))
            if (company.nif.isNotBlank()) addElement(Paragraph("CIF: ${company.nif}", FontFactory.getFont(FontFactory.HELVETICA, 9f)))
            if (company.phone.isNotBlank()) addElement(Paragraph("Tel: ${company.phone}", FontFactory.getFont(FontFactory.HELVETICA, 9f)))
            if (company.address.isNotBlank()) addElement(Paragraph(company.address, FontFactory.getFont(FontFactory.HELVETICA, 9f)))
        }
        headerTable.addCell(companyCell)
        
        // Columna Derecha: Cliente
        val clientCell = PdfPCell().apply {
            border = Rectangle.NO_BORDER
            horizontalAlignment = Element.ALIGN_RIGHT
            addElement(Paragraph("CLIENTE:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f)).apply { alignment = Element.ALIGN_RIGHT })
            addElement(Paragraph(client.name, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f)).apply { alignment = Element.ALIGN_RIGHT })
            if (client.taxId.isNotBlank()) addElement(Paragraph("CIF/NIF: ${client.taxId}", FontFactory.getFont(FontFactory.HELVETICA, 9f)).apply { alignment = Element.ALIGN_RIGHT })
            if (client.phone.isNotBlank()) addElement(Paragraph("Tel: ${client.phone}", FontFactory.getFont(FontFactory.HELVETICA, 9f)).apply { alignment = Element.ALIGN_RIGHT })
            if (client.address.isNotBlank()) addElement(Paragraph(client.address, FontFactory.getFont(FontFactory.HELVETICA, 9f)).apply { alignment = Element.ALIGN_RIGHT })
            if (client.email.isNotBlank()) addElement(Paragraph(client.email, FontFactory.getFont(FontFactory.HELVETICA, 9f)).apply { alignment = Element.ALIGN_RIGHT })
        }
        headerTable.addCell(clientCell)
        
        // Escribir cabecera en el documento
        headerTable.writeSelectedRows(0, -1, document.left(), document.top() + 140f, cb)
        
        // Título y Pagina
        val titleTable = PdfPTable(1).apply {
            totalWidth = document.right() - document.left()
        }
        
        val pageText = "Página ${writer.pageNumber} de ${writer.pageNumber}" 
        val titleCell = PdfPCell().apply {
            border = Rectangle.NO_BORDER
            horizontalAlignment = Element.ALIGN_CENTER
            paddingBottom = 0f
            addElement(Paragraph(pageText, FontFactory.getFont(FontFactory.HELVETICA, 8f)).apply { 
                alignment = Element.ALIGN_CENTER 
                leading = 8f
            })
            addElement(Paragraph("$docTitle $docNumber", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, Font.BOLD, java.awt.Color.DARK_GRAY)).apply { 
                alignment = Element.ALIGN_CENTER 
                leading = 20f
            })
            
            val infoPara = Paragraph("Fecha: $docDate", FontFactory.getFont(FontFactory.HELVETICA, 10f)).apply { 
                alignment = Element.ALIGN_CENTER 
                leading = 12f
            }
            if (!expirationDate.isNullOrBlank()) {
                infoPara.add(Phrase("   -   Vencimiento: $expirationDate", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Font.BOLD, java.awt.Color.RED)))
            }
            addElement(infoPara)
        }
        titleTable.addCell(titleCell)
        titleTable.writeSelectedRows(0, -1, document.left(), document.top() + 50f, cb)
    }
}
