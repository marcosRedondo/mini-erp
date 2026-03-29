package com.mrm.minierp.utils

import com.mrm.minierp.models.Client
import com.mrm.minierp.models.Company
import com.mrm.minierp.models.Invoice
import com.mrm.minierp.models.Quote

expect class PdfGenerator() {
    fun generateInvoicePdf(company: Company, client: Client, invoice: Invoice)
    fun generateQuotePdf(company: Company, client: Client, quote: Quote)
}
