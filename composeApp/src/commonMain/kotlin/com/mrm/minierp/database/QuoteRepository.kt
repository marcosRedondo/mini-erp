package com.mrm.minierp.database

import com.mrm.minierp.models.*
import com.mrm.minierp.models.QuoteWithClient
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate

class QuoteRepository(private val database: MiniErpDatabase) {
    private val queries = database.appDatabaseQueries

    fun getAllQuotes(): List<Quote> {
        return queries.selectAllQuotes().executeAsList().map { entity ->
            val lines = queries.selectLinesForQuote(entity.id).executeAsList().map { lineEntity ->
                QuoteLine(
                    id = lineEntity.id.toInt(),
                    quantity = lineEntity.quantity,
                    concept = lineEntity.concept,
                    detail = lineEntity.detail,
                    sublines = lineEntity.sublines?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                    iva = lineEntity.iva.toInt(),
                    unitPrice = lineEntity.unitPrice
                )
            }
            Quote(
                id = entity.id.toInt(),
                clientId = entity.clientId.toInt(),
                number = entity.number,
                date = entity.date.toLocalDate(),
                totalAmount = entity.totalAmount ?: 0.0,
                lines = lines
            )
        }
    }

    fun saveQuote(quote: Quote): Long {
        val quoteId: Long
        if (quote.id == 0) {
            queries.insertQuote(
                clientId = quote.clientId.toLong(),
                number = quote.number,
                date = quote.date.toString(),
                totalAmount = quote.totalAmount
            )
            quoteId = queries.selectLastInsertedQuoteId().executeAsOne().MAX ?: 0L
        } else {
            queries.updateQuote(
                clientId = quote.clientId.toLong(),
                number = quote.number,
                date = quote.date.toString(),
                totalAmount = quote.totalAmount,
                id = quote.id.toLong()
            )
            quoteId = quote.id.toLong()
        }

        // Update lines: delete all and insert new ones (simpler for now)
        queries.deleteLinesForQuote(quoteId)
        quote.lines.forEach { line ->
            queries.insertQuoteLine(
                quoteId = quoteId,
                quantity = line.quantity,
                concept = line.concept,
                detail = line.detail,
                sublines = if (line.sublines.isEmpty()) null else line.sublines.joinToString("\n"),
                iva = line.iva.toLong(),
                unitPrice = line.unitPrice
            )
        }
        return quoteId
    }

    fun deleteQuote(id: Int) {
        queries.deleteQuote(id.toLong())
    }

    fun getRecentQuotesWithClient(limit: Int = 10): List<QuoteWithClient> {
        return queries.selectRecentQuotesWithClient(limit.toLong()).executeAsList().map { row ->
            mapToQuoteWithClient(row.id, row.clientId, row.number, row.date, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getQuotesPaged(limit: Int, offset: Int): List<QuoteWithClient> {
        return queries.selectQuotesPaged(limit.toLong(), offset.toLong()).executeAsList().map { row ->
            mapToQuoteWithClient(row.id, row.clientId, row.number, row.date, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getQuotesFilteredPaged(clientIds: List<Int>, limit: Int, offset: Int): List<QuoteWithClient> {
        return queries.selectQuotesFilteredPaged(clientIds.map { it.toLong() }, limit.toLong(), offset.toLong()).executeAsList().map { row ->
            mapToQuoteWithClient(row.id, row.clientId, row.number, row.date, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getQuotesAdvancedPaged(
        clientIds: List<Int>,
        numberSearch: String?,
        contentSearch: String?,
        limit: Int,
        offset: Int
    ): List<QuoteWithClient> {
        val numSearch = if (numberSearch.isNullOrBlank()) null else "%$numberSearch%"
        val contSearch = if (contentSearch.isNullOrBlank()) null else "%$contentSearch%"
        
        return queries.selectQuotesAdvancedPaged(
            clientIdsEmpty = clientIds.isEmpty(),
            clientIds = clientIds.map { it.toLong() },
            numberSearch = numSearch,
            contentSearch = contSearch,
            limit = limit.toLong(),
            offset = offset.toLong()
        ).executeAsList().map { row ->
            mapToQuoteWithClient(row.id, row.clientId, row.number, row.date, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getTotalQuotesCount(clientIds: List<Int>? = null, numberSearch: String? = null, contentSearch: String? = null): Long {
        if (numberSearch.isNullOrBlank() && contentSearch.isNullOrBlank()) {
            return if (clientIds.isNullOrEmpty()) {
                queries.countQuotes().executeAsOne()
            } else {
                queries.countQuotesFiltered(clientIds.map { it.toLong() }).executeAsOne()
            }
        }
        
        val numSearch = if (numberSearch.isNullOrBlank()) null else "%$numberSearch%"
        val contSearch = if (contentSearch.isNullOrBlank()) null else "%$contentSearch%"
        
        return queries.countQuotesAdvanced(
            clientIdsEmpty = clientIds.isNullOrEmpty(),
            clientIds = clientIds?.map { it.toLong() } ?: emptyList(),
            numberSearch = numSearch,
            contentSearch = contSearch
        ).executeAsOne()
    }

    private fun mapToQuoteWithClient(
        id: Long,
        clientId: Long,
        number: String,
        dateString: String,
        totalAmount: Double?,
        clientName: String,
        clientTaxId: String?
    ): QuoteWithClient {
        val lines = queries.selectLinesForQuote(id).executeAsList().map { lineEntity ->
            QuoteLine(
                id = lineEntity.id.toInt(),
                quantity = lineEntity.quantity,
                concept = lineEntity.concept,
                detail = lineEntity.detail,
                sublines = lineEntity.sublines?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                iva = lineEntity.iva.toInt(),
                unitPrice = lineEntity.unitPrice
            )
        }
        return QuoteWithClient(
            quote = Quote(
                id = id.toInt(),
                clientId = clientId.toInt(),
                number = number,
                date = dateString.toLocalDate(),
                totalAmount = totalAmount ?: 0.0,
                lines = lines
            ),
            clientName = clientName,
            clientTaxId = clientTaxId ?: ""
        )
    }

    fun getQuoteById(id: Int): Quote? {
        val entity = queries.selectAllQuotes().executeAsList().find { it.id == id.toLong() } ?: return null
        val lines = queries.selectLinesForQuote(entity.id).executeAsList().map { lineEntity ->
            QuoteLine(
                id = lineEntity.id.toInt(),
                quantity = lineEntity.quantity,
                concept = lineEntity.concept,
                detail = lineEntity.detail,
                sublines = lineEntity.sublines?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                iva = lineEntity.iva.toInt(),
                unitPrice = lineEntity.unitPrice
            )
        }
        return Quote(
            id = entity.id.toInt(),
            clientId = entity.clientId.toInt(),
            number = entity.number,
            date = entity.date.toLocalDate(),
            totalAmount = entity.totalAmount ?: 0.0,
            lines = lines
        )
    }

    fun getNextQuoteNumber(clientId: Int, year: Int): String {
        val yearStr = "$year%"
        val count = queries.countQuotesForClientInYear(clientId.toLong(), yearStr).executeAsOne()
        val nextNum = (count + 1).toString().padStart(4, '0')
        return "$year-$nextNum"
    }
}
