package com.mrm.minierp.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.mrm.minierp.AndroidContextProvider
import com.mrm.minierp.models.*
import java.io.File
import java.io.FileOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class PdfGenerator actual constructor() {

    @OptIn(ExperimentalEncodingApi::class)
    actual fun generateInvoicePdf(company: Company, client: Client, invoice: Invoice) {
        generateAndroidPdf(
            title = "FACTURA",
            number = invoice.number,
            date = invoice.date.toString(),
            company = company,
            client = client,
            lines = invoice.lines.map { PdfLine(it.concept, it.quantity, it.unitPrice, it.iva, it.totalWithoutIva) },
            totalAmount = invoice.totalAmount,
            notes = invoice.notes
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    actual fun generateQuotePdf(company: Company, client: Client, quote: Quote) {
        generateAndroidPdf(
            title = "PRESUPUESTO",
            number = quote.number,
            date = quote.date.toString(),
            company = company,
            client = client,
            lines = quote.lines.map { PdfLine(it.concept, it.quantity, it.unitPrice, it.iva, it.totalWithoutIva) },
            totalAmount = quote.totalAmount,
            notes = quote.notes
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateAndroidPdf(
        title: String,
        number: String,
        date: String,
        company: Company,
        client: Client,
        lines: List<PdfLine>,
        totalAmount: Double,
        notes: String
    ) {
        val context = AndroidContextProvider.context ?: return
        val pdfDocument = PdfDocument()
        val paint = Paint()
        
        // A4: 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        var yPos = 40f
        
        // --- CABECERA (LOGO Y EMPRESA) ---
        // Logo
        company.logoBase64?.let { base64 ->
            try {
                val bytes = Base64.decode(base64)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val scaled = scaleBitmap(bitmap, 100, 50)
                    canvas.drawBitmap(scaled, 40f, yPos, paint)
                    yPos += 60f
                }
            } catch (e: Exception) {}
        }
        
        // Datos Empresa (Izquierda)
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText(company.name, 40f, yPos, paint)
        yPos += 15f
        paint.isFakeBoldText = false
        paint.textSize = 10f
        if (company.nif.isNotBlank()) { canvas.drawText("CIF: ${company.nif}", 40f, yPos, paint); yPos += 12f }
        if (company.phone.isNotBlank()) { canvas.drawText("Tel: ${company.phone}", 40f, yPos, paint); yPos += 12f }
        if (company.address.isNotBlank()) { canvas.drawText(company.address, 40f, yPos, paint); yPos += 12f }
        
        // Datos Cliente (Derecha)
        var yClient = 40f
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 10f
        canvas.drawText("CLIENTE", 555f, yClient, paint); yClient += 15f
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText(client.name, 555f, yClient, paint); yClient += 15f
        paint.isFakeBoldText = false
        paint.textSize = 10f
        if (client.taxId.isNotBlank()) { canvas.drawText("NIF: ${client.taxId}", 555f, yClient, paint); yClient += 12f }
        if (client.address.isNotBlank()) { canvas.drawText(client.address, 555f, yClient, paint); yClient += 12f }
        if (client.phone.isNotBlank()) { canvas.drawText("Tel: ${client.phone}", 555f, yClient, paint); yClient += 12f }
        
        yPos = Math.max(yPos, yClient) + 30f
        paint.textAlign = Paint.Align.LEFT
        
        // --- TITULO CENTRAL ---
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("$title $number", 297f, yPos, paint)
        yPos += 20f
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Fecha: $date", 297f, yPos, paint)
        yPos += 30f
        
        // --- TABLA DE LINEAS (Sencilla) ---
        paint.textAlign = Paint.Align.LEFT
        paint.isFakeBoldText = true
        canvas.drawText("Concepto", 40f, yPos, paint)
        canvas.drawText("Total", 500f, yPos, paint)
        yPos += 5f
        canvas.drawLine(40f, yPos, 555f, yPos, paint)
        yPos += 15f
        paint.isFakeBoldText = false
        
        lines.forEach { line ->
            if (yPos > 800) {
                // TODO: Soporte multipagina en Android es manual. Aquí lo simplificamos a 1 pag por ahora o cortamos.
            }
            canvas.drawText(line.concept, 40f, yPos, paint)
            canvas.drawText("${String.format("%.2f", line.total)} €", 500f, yPos, paint)
            yPos += 15f
        }
        
        canvas.drawLine(40f, yPos, 555f, yPos, paint)
        yPos += 20f
        
        // --- TOTALES ---
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("TOTAL: ${String.format("%.2f", totalAmount)} €", 555f, yPos, paint)
        
        pdfDocument.finishPage(page)
        
        val file = File(context.cacheDir, "doc_${number}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            openAndroidPdf(context, file)
        } catch (e: Exception) {}
        
        pdfDocument.close()
    }
    
    private fun scaleBitmap(bm: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        var width = bm.width
        var height = bm.height
        val ratio = width.toFloat() / height.toFloat()
        if (width > maxWidth) {
            width = maxWidth
            height = (width / ratio).toInt()
        }
        if (height > maxHeight) {
            height = maxHeight
            width = (height * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bm, width, height, true)
    }

    private fun openAndroidPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private class PdfLine(
    val concept: String,
    val quantity: Double,
    val unitPrice: Double,
    val iva: Int,
    val total: Double
)
