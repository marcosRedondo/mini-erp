package com.mrm.minierp.database

import com.mrm.minierp.models.Quote
import com.mrm.minierp.models.QuoteWithClient
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate

class QuoteRepository(private val database: MiniErpDatabase) {
    private val queries = database.appDatabaseQueries

    fun getAllQuotes(): List<Quote> {
        return queries.selectAllQuotes().executeAsList().map { entity ->
            Quote(
                id = entity.id.toInt(),
                clientId = entity.clientId.toInt(),
                number = entity.number,
                date = entity.date.toLocalDate(),
                totalAmount = entity.totalAmount ?: 0.0
            )
        }
    }

    fun saveQuote(quote: Quote) {
        if (quote.id == 0) {
            queries.insertQuote(
                clientId = quote.clientId.toLong(),
                number = quote.number,
                date = quote.date.toString(),
                totalAmount = quote.totalAmount
            )
        } else {
            queries.updateQuote(
                clientId = quote.clientId.toLong(),
                number = quote.number,
                date = quote.date.toString(),
                totalAmount = quote.totalAmount,
                id = quote.id.toLong()
            )
        }
    }

    fun deleteQuote(id: Int) {
        queries.deleteQuote(id.toLong())
    }

    fun getRecentQuotesWithClient(limit: Int = 10): List<QuoteWithClient> {
        return queries.selectRecentQuotesWithClient(limit.toLong()).executeAsList().map { row ->
            QuoteWithClient(
                quote = Quote(
                    id = row.id.toInt(),
                    clientId = row.clientId.toInt(),
                    number = row.number,
                    date = row.date.toLocalDate(),
                    totalAmount = row.totalAmount ?: 0.0
                ),
                clientName = row.clientName,
                clientTaxId = row.clientTaxId ?: ""
            )
        }
    }

    fun getQuoteById(id: Int): Quote? {
        // Podríamos añadir una query específica en el SQL, o filtrar aquí si son pocos
        return queries.selectAllQuotes().executeAsList().find { it.id == id.toLong() }?.let { entity ->
            Quote(
                id = entity.id.toInt(),
                clientId = entity.clientId.toInt(),
                number = entity.number,
                date = entity.date.toLocalDate(),
                totalAmount = entity.totalAmount ?: 0.0
            )
        }
    }

    fun getNextQuoteNumber(clientId: Int, year: Int): String {
        val yearStr = "$year%"
        val count = queries.countQuotesForClientInYear(clientId.toLong(), yearStr).executeAsOne()
        val nextNum = (count + 1).toString().padStart(4, '0')
        return "$year-$nextNum"
    }
}
