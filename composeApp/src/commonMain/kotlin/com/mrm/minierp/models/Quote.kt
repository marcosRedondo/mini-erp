package com.mrm.minierp.models

import kotlinx.datetime.LocalDate

data class Quote(
    val id: Int = 0,
    val clientId: Int,
    val number: String,
    val date: LocalDate,
    val totalAmount: Double = 0.0
)

data class QuoteWithClient(
    val quote: Quote,
    val clientName: String,
    val clientTaxId: String
)
