package com.mrm.minierp.database

import com.mrm.minierp.models.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate

class DeliveryNoteRepository(private val database: MiniErpDatabase) {
    private val queries = database.appDatabaseQueries

    fun getAllDeliveryNotes(): List<DeliveryNote> {
        return queries.selectAllDeliveryNotes().executeAsList().map { entity ->
            val lines = queries.selectLinesForDeliveryNote(entity.id).executeAsList().map { lineEntity ->
                DeliveryNoteLine(
                    id = lineEntity.id.toInt(),
                    quantity = lineEntity.quantity,
                    concept = lineEntity.concept,
                    detail = lineEntity.detail,
                    sublines = lineEntity.sublines?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                    unitPrice = lineEntity.unitPrice
                )
            }
            DeliveryNote(
                id = entity.id.toInt(),
                clientId = entity.clientId.toInt(),
                quoteId = entity.quoteId?.toInt(),
                invoiceId = entity.invoiceId?.toInt(),
                number = entity.number,
                date = entity.date.toLocalDate(),
                totalAmount = entity.totalAmount ?: 0.0,
                notes = entity.notes ?: "",
                lines = lines
            )
        }
    }

    fun saveDeliveryNote(deliveryNote: DeliveryNote): Long {
        val deliveryNoteId: Long
        if (deliveryNote.id == 0) {
            queries.insertDeliveryNote(
                clientId = deliveryNote.clientId.toLong(),
                quoteId = deliveryNote.quoteId?.toLong(),
                invoiceId = deliveryNote.invoiceId?.toLong(),
                number = deliveryNote.number,
                date = deliveryNote.date.toString(),
                totalAmount = deliveryNote.totalAmount,
                notes = deliveryNote.notes
            )
            deliveryNoteId = queries.selectLastInsertedDeliveryNoteId().executeAsOne().MAX ?: 0L
        } else {
            queries.updateDeliveryNote(
                clientId = deliveryNote.clientId.toLong(),
                quoteId = deliveryNote.quoteId?.toLong(),
                invoiceId = deliveryNote.invoiceId?.toLong(),
                number = deliveryNote.number,
                date = deliveryNote.date.toString(),
                totalAmount = deliveryNote.totalAmount,
                notes = deliveryNote.notes,
                id = deliveryNote.id.toLong()
            )
            deliveryNoteId = deliveryNote.id.toLong()
        }

        queries.deleteLinesForDeliveryNote(deliveryNoteId)
        deliveryNote.lines.forEach { line ->
            queries.insertDeliveryNoteLine(
                deliveryNoteId = deliveryNoteId,
                quantity = line.quantity,
                concept = line.concept,
                detail = line.detail,
                sublines = if (line.sublines.isEmpty()) null else line.sublines.joinToString("\n"),
                unitPrice = line.unitPrice
            )
        }
        return deliveryNoteId
    }

    fun deleteDeliveryNote(id: Int) {
        queries.deleteDeliveryNote(id.toLong())
    }

    fun getRecentDeliveryNotesWithClient(limit: Int = 10): List<DeliveryNoteWithClient> {
        return queries.selectRecentDeliveryNotesWithClient(limit.toLong()).executeAsList().map { row ->
            mapToDeliveryNoteWithClient(row.id, row.clientId, row.quoteId, row.invoiceId, row.number, row.date, row.notes, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getDeliveryNotesPaged(limit: Int, offset: Int): List<DeliveryNoteWithClient> {
        return queries.selectDeliveryNotesPaged(limit.toLong(), offset.toLong()).executeAsList().map { row ->
            mapToDeliveryNoteWithClient(row.id, row.clientId, row.quoteId, row.invoiceId, row.number, row.date, row.notes, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getDeliveryNotesFilteredPaged(clientIds: List<Int>, limit: Int, offset: Int): List<DeliveryNoteWithClient> {
        return queries.selectDeliveryNotesFilteredPaged(clientIds.map { it.toLong() }, limit.toLong(), offset.toLong()).executeAsList().map { row ->
            mapToDeliveryNoteWithClient(row.id, row.clientId, row.quoteId, row.invoiceId, row.number, row.date, row.notes, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getDeliveryNotesAdvancedPaged(
        clientIds: List<Int>,
        numberSearch: String?,
        contentSearch: String?,
        limit: Int,
        offset: Int
    ): List<DeliveryNoteWithClient> {
        val numSearch = if (numberSearch.isNullOrBlank()) null else "%$numberSearch%"
        val contSearch = if (contentSearch.isNullOrBlank()) null else "%$contentSearch%"
        
        return queries.selectDeliveryNotesAdvancedPaged(
            clientIdsEmpty = clientIds.isEmpty(),
            clientIds = clientIds.map { it.toLong() },
            numberSearch = numSearch,
            contentSearch = contSearch,
            limit = limit.toLong(),
            offset = offset.toLong()
        ).executeAsList().map { row ->
            mapToDeliveryNoteWithClient(row.id, row.clientId, row.quoteId, row.invoiceId, row.number, row.date, row.notes, row.totalAmount, row.clientName, row.clientTaxId)
        }
    }

    fun getTotalDeliveryNotesCount(clientIds: List<Int>? = null, numberSearch: String? = null, contentSearch: String? = null): Long {
        if (numberSearch.isNullOrBlank() && contentSearch.isNullOrBlank()) {
            return if (clientIds.isNullOrEmpty()) {
                queries.countDeliveryNotes().executeAsOne()
            } else {
                queries.countDeliveryNotesFiltered(clientIds.map { it.toLong() }).executeAsOne()
            }
        }
        
        val numSearch = if (numberSearch.isNullOrBlank()) null else "%$numberSearch%"
        val contSearch = if (contentSearch.isNullOrBlank()) null else "%$contentSearch%"
        
        return queries.countDeliveryNotesAdvanced(
            clientIdsEmpty = clientIds.isNullOrEmpty(),
            clientIds = clientIds?.map { it.toLong() } ?: emptyList(),
            numberSearch = numSearch,
            contentSearch = contSearch
        ).executeAsOne()
    }

    private fun mapToDeliveryNoteWithClient(
        id: Long,
        clientId: Long,
        quoteId: Long?,
        invoiceId: Long?,
        number: String,
        dateString: String,
        notes: String?,
        totalAmount: Double?,
        clientName: String,
        clientTaxId: String?
    ): DeliveryNoteWithClient {
        val lines = queries.selectLinesForDeliveryNote(id).executeAsList().map { lineEntity ->
            DeliveryNoteLine(
                id = lineEntity.id.toInt(),
                quantity = lineEntity.quantity,
                concept = lineEntity.concept,
                detail = lineEntity.detail,
                sublines = lineEntity.sublines?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                unitPrice = lineEntity.unitPrice
            )
        }
        return DeliveryNoteWithClient(
            deliveryNote = DeliveryNote(
                id = id.toInt(),
                clientId = clientId.toInt(),
                quoteId = quoteId?.toInt(),
                invoiceId = invoiceId?.toInt(),
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

    fun getDeliveryNoteById(id: Int): DeliveryNote? {
        val entity = queries.selectDeliveryNoteById(id.toLong()).executeAsOneOrNull() ?: return null
        val lines = queries.selectLinesForDeliveryNote(entity.id).executeAsList().map { lineEntity ->
            DeliveryNoteLine(
                id = lineEntity.id.toInt(),
                quantity = lineEntity.quantity,
                concept = lineEntity.concept,
                detail = lineEntity.detail,
                sublines = lineEntity.sublines?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                unitPrice = lineEntity.unitPrice
            )
        }
        return DeliveryNote(
            id = entity.id.toInt(),
            clientId = entity.clientId.toInt(),
            quoteId = entity.quoteId?.toInt(),
            invoiceId = entity.invoiceId?.toInt(),
            number = entity.number,
            date = entity.date.toLocalDate(),
            totalAmount = entity.totalAmount ?: 0.0,
            notes = entity.notes ?: "",
            lines = lines
        )
    }

    fun getNextDeliveryNoteNumber(clientId: Int, year: Int): String {
        val yearStr = "$year%"
        val count = queries.countDeliveryNotesForClientInYear(clientId.toLong(), yearStr).executeAsOne()
        val nextNum = (count + 1).toString().padStart(4, '0')
        return "A$year-$nextNum"
    }

    fun getDeliveryNoteByQuoteId(quoteId: Int): DeliveryNote? {
        val entity = queries.selectDeliveryNoteByQuoteId(quoteId.toLong()).executeAsOneOrNull() ?: return null
        val lines = queries.selectLinesForDeliveryNote(entity.id).executeAsList().map { lineEntity ->
            DeliveryNoteLine(
                id = lineEntity.id.toInt(),
                quantity = lineEntity.quantity,
                concept = lineEntity.concept,
                detail = lineEntity.detail,
                sublines = lineEntity.sublines?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                unitPrice = lineEntity.unitPrice
            )
        }
        return DeliveryNote(
            id = entity.id.toInt(),
            clientId = entity.clientId.toInt(),
            quoteId = entity.quoteId?.toInt(),
            invoiceId = entity.invoiceId?.toInt(),
            number = entity.number,
            date = entity.date.toLocalDate(),
            totalAmount = entity.totalAmount ?: 0.0,
            notes = entity.notes ?: "",
            lines = lines
        )
    }
}
