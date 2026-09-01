package com.mrm.minierp.models

import kotlinx.datetime.LocalDate

data class DeliveryNote(
    val id: Int = 0,
    val clientId: Int,
    val quoteId: Int? = null,
    val invoiceId: Int? = null,
    val number: String,
    val date: LocalDate,
    val totalAmount: Double = 0.0,
    val notes: String = "",
    val lines: List<DeliveryNoteLine> = emptyList()
)

data class DeliveryNoteLine(
    val id: Int = 0,
    val quantity: Double,
    val concept: String,
    val detail: String? = null,
    val sublines: List<String> = emptyList(),
    val unitPrice: Double
) {
    val total: Double get() = quantity * unitPrice
}

data class DeliveryNoteWithClient(
    val deliveryNote: DeliveryNote,
    val clientName: String,
    val clientTaxId: String
)
