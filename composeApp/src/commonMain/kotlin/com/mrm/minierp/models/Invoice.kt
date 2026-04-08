package com.mrm.minierp.models

import kotlinx.datetime.LocalDate

data class Invoice(
    val id: Int = 0,
    val clientId: Int,
    val quoteId: Int? = null,
    val number: String,
    val date: LocalDate,
    val totalAmount: Double = 0.0,
    val notes: String = "",
    val lines: List<InvoiceLine> = emptyList()
)

data class InvoiceLine(
    val id: Int = 0,
    val quantity: Double,
    val concept: String,
    val detail: String? = null,
    val sublines: List<String> = emptyList(),
    val iva: Int = 21,
    val unitPrice: Double
) {
    val totalWithoutIva: Double get() = quantity * unitPrice
    val ivaAmount: Double get() = totalWithoutIva * (iva / 100.0)
    val totalWithIva: Double get() = totalWithoutIva + ivaAmount
}

data class InvoiceWithClient(
    val invoice: Invoice,
    val clientName: String,
    val clientTaxId: String
)
