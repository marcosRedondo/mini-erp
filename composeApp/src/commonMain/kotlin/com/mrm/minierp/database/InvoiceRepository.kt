package com.mrm.minierp.database

import com.mrm.minierp.models.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate

class InvoiceRepository(private val database: MiniErpDatabase) {
    private val queries = database.appDatabaseQueries

    fun getAllInvoices(): List<Invoice> {
        return queries.selectAllInvoices().executeAsList().map { entity ->
            val lines = queries.selectLinesForInvoice(entity.id).executeAsList().map { lineEntity ->
                InvoiceLine(
                    id = lineEntity.id.toInt(),
                    quantity = lineEntity.quantity,
                    concept = lineEntity.concept,
                    detail = lineEntity.detail,
                    sublines = lineEntity.sublines?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                    iva = lineEntity.iva.toInt(),
                    unitPrice = lineEntity.unitPrice
                )
            }
            Invoice(
                id = entity.id.toInt(),
                clientId = entity.clientId.toInt(),
                quoteId = entity.quoteId?.toInt(),
                number = entity.number,
                date = entity.date.toLocalDate(),
                totalAmount = entity.totalAmount ?: 0.0,
                notes = entity.notes ?: "",
                lines = lines
            )
        }
    }

    fun saveInvoice(invoice: Invoice): Long {
        val invoiceId: Long
        if (invoice.id == 0) {
            queries.insertInvoice(
                clientId = invoice.clientId.toLong(),
                quoteId = invoice.quoteId?.toLong(),
                number = invoice.number,
                date = invoice.date.toString(),
                totalAmount = invoice.totalAmount,
                notes = invoice.notes
            )
            invoiceId = queries.selectLastInsertedInvoiceId().executeAsOne().MAX ?: 0L
        } else {
            queries.updateInvoice(
                clientId = invoice.clientId.toLong(),
                quoteId = invoice.quoteId?.toLong(),
                number = invoice.number,
                date = invoice.date.toString(),
                totalAmount = invoice.totalAmount,
                notes = invoice.notes,
                id = invoice.id.toLong()
            )
            invoiceId = invoice.id.toLong()
        }

        queries.deleteLinesForInvoice(invoiceId)
        invoice.lines.forEach { line ->
            queries.insertInvoiceLine(
                invoiceId = invoiceId,
                quantity = line.quantity,
                concept = line.concept,
                detail = line.detail,
                sublines = if (line.sublines.isEmpty()) null else line.sublines.joinToString("\n"),
                iva = line.iva.toLong(),
                unitPrice = line.unitPrice
            )
        }
        return invoiceId
    }

    fun deleteInvoice(id: Int) {
        queries.deleteInvoice(id.toLong())
    }

    fun getRecentInvoicesWithClient(limit: Int = 10): List<InvoiceWithClient> {
        return queries.selectRecentInvoicesWithClient(limit.toLong()).executeAsList().map { row ->
            mapToInvoiceWithClient(row.id, row.clientId, row.quoteId, row.number, row.date, row.notes, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getInvoicesPaged(limit: Int, offset: Int): List<InvoiceWithClient> {
        return queries.selectInvoicesPaged(limit.toLong(), offset.toLong()).executeAsList().map { row ->
            mapToInvoiceWithClient(row.id, row.clientId, row.quoteId, row.number, row.date, row.notes, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getInvoicesFilteredPaged(clientIds: List<Int>, limit: Int, offset: Int): List<InvoiceWithClient> {
        return queries.selectInvoicesFilteredPaged(clientIds.map { it.toLong() }, limit.toLong(), offset.toLong()).executeAsList().map { row ->
            mapToInvoiceWithClient(row.id, row.clientId, row.quoteId, row.number, row.date, row.notes, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getInvoicesAdvancedPaged(
        clientIds: List<Int>,
        numberSearch: String?,
        contentSearch: String?,
        limit: Int,
        offset: Int
    ): List<InvoiceWithClient> {
        val numSearch = if (numberSearch.isNullOrBlank()) null else "%$numberSearch%"
        val contSearch = if (contentSearch.isNullOrBlank()) null else "%$contentSearch%"
        
        return queries.selectInvoicesAdvancedPaged(
            clientIdsEmpty = clientIds.isEmpty(),
            clientIds = clientIds.map { it.toLong() },
            numberSearch = numSearch,
            contentSearch = contSearch,
            limit = limit.toLong(),
            offset = offset.toLong()
        ).executeAsList().map { row ->
            mapToInvoiceWithClient(row.id, row.clientId, row.quoteId, row.number, row.date, row.notes, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getTotalInvoicesCount(clientIds: List<Int>? = null, numberSearch: String? = null, contentSearch: String? = null): Long {
        if (numberSearch.isNullOrBlank() && contentSearch.isNullOrBlank()) {
            return if (clientIds.isNullOrEmpty()) {
                queries.countInvoices().executeAsOne()
            } else {
                queries.countInvoicesFiltered(clientIds.map { it.toLong() }).executeAsOne()
            }
        }
        
        val numSearch = if (numberSearch.isNullOrBlank()) null else "%$numberSearch%"
        val contSearch = if (contentSearch.isNullOrBlank()) null else "%$contentSearch%"
        
        return queries.countInvoicesAdvanced(
            clientIdsEmpty = clientIds.isNullOrEmpty(),
            clientIds = clientIds?.map { it.toLong() } ?: emptyList(),
            numberSearch = numSearch,
            contentSearch = contSearch
        ).executeAsOne()
    }

    private fun mapToInvoiceWithClient(
        id: Long,
        clientId: Long,
        quoteId: Long?,
        number: String,
        dateString: String,
        notes: String?,
        totalAmount: Double?,
        clientName: String,
        clientTaxId: String?
    ): InvoiceWithClient {
        val lines = queries.selectLinesForInvoice(id).executeAsList().map { lineEntity ->
            InvoiceLine(
                id = lineEntity.id.toInt(),
                quantity = lineEntity.quantity,
                concept = lineEntity.concept,
                detail = lineEntity.detail,
                sublines = lineEntity.sublines?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                iva = lineEntity.iva.toInt(),
                unitPrice = lineEntity.unitPrice
            )
        }
        return InvoiceWithClient(
            invoice = Invoice(
                id = id.toInt(),
                clientId = clientId.toInt(),
                quoteId = quoteId?.toInt(),
                number = number,
                date = dateString.toLocalDate(),
                totalAmount = totalAmount ?: 0.0,
                notes = notes ?: "",
                lines = lines
            ),
            clientName = clientName,
            clientTaxId = clientTaxId ?: ""
        )
    }

    fun getInvoiceById(id: Int): Invoice? {
        val entity = queries.selectAllInvoices().executeAsList().find { it.id == id.toLong() } ?: return null
        val lines = queries.selectLinesForInvoice(entity.id).executeAsList().map { lineEntity ->
            InvoiceLine(
                id = lineEntity.id.toInt(),
                quantity = lineEntity.quantity,
                concept = lineEntity.concept,
                detail = lineEntity.detail,
                sublines = lineEntity.sublines?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                iva = lineEntity.iva.toInt(),
                unitPrice = lineEntity.unitPrice
            )
        }
        return Invoice(
            id = entity.id.toInt(),
            clientId = entity.clientId.toInt(),
            quoteId = entity.quoteId?.toInt(),
            number = entity.number,
            date = entity.date.toLocalDate(),
            totalAmount = entity.totalAmount ?: 0.0,
            notes = entity.notes ?: "",
            lines = lines
        )
    }

    fun getNextInvoiceNumber(clientId: Int, year: Int): String {
        val yearStr = "$year%"
        val count = queries.countInvoicesForClientInYear(clientId.toLong(), yearStr).executeAsOne()
        val nextNum = (count + 1).toString().padStart(4, '0')
        return "F$year-$nextNum"
    }

    fun getInvoiceByQuoteId(quoteId: Int): Invoice? {
        val entity = queries.selectInvoiceByQuoteId(quoteId.toLong()).executeAsOneOrNull() ?: return null
        val lines = queries.selectLinesForInvoice(entity.id).executeAsList().map { lineEntity ->
            InvoiceLine(
                id = lineEntity.id.toInt(),
                quantity = lineEntity.quantity,
                concept = lineEntity.concept,
                detail = lineEntity.detail,
                sublines = lineEntity.sublines?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                iva = lineEntity.iva.toInt(),
                unitPrice = lineEntity.unitPrice
            )
        }
        return Invoice(
            id = entity.id.toInt(),
            clientId = entity.clientId.toInt(),
            quoteId = entity.quoteId?.toInt(),
            number = entity.number,
            date = entity.date.toLocalDate(),
            totalAmount = entity.totalAmount ?: 0.0,
            notes = entity.notes ?: "",
            lines = lines
        )
    }
}
