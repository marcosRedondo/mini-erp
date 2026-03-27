package com.mrm.minierp.models

import kotlinx.datetime.LocalDate

data class Quote(
    val id: Int = 0,
    val clientId: Int,
    val number: String,
    val date: LocalDate,
    val expirationDate: LocalDate? = null,
    val totalAmount: Double = 0.0,
    val notes: String = "",
    val lines: List<QuoteLine> = emptyList()
)

data class QuoteLine(
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

data class QuoteWithClient(
    val quote: Quote,
    val clientName: String,
    val clientTaxId: String
)
