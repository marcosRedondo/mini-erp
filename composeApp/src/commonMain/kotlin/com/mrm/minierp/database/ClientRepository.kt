package com.mrm.minierp.database

import androidx.compose.runtime.*
import com.mrm.minierp.models.Client
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ClientRepository(private val database: MiniErpDatabase) {
    var lastCreatedClientId by mutableStateOf<Int?>(null)
        private set

    private val _clientCreated = MutableSharedFlow<Int>(replay = 1)
    val clientCreated = _clientCreated.asSharedFlow()

    private val queries = database.appDatabaseQueries

    fun getAllClients(): List<Client> {
        return queries.selectAllClients().executeAsList().map { entity ->
            Client(
                id = entity.id.toInt(),
                name = entity.name,
                taxId = entity.taxId ?: "",
                address = entity.address ?: "",
                phone = entity.phone ?: "",
                email = entity.email ?: "",
                notes = entity.notes ?: "",
                isVip = entity.is_vip == 1L
            )
        }
    }

    suspend fun saveClient(client: Client): Int {
        queries.insertClient(
            name = client.name,
            taxId = client.taxId,
            address = client.address,
            phone = client.phone,
            email = client.email,
            notes = client.notes,
            is_vip = if (client.isVip) 1L else 0L
        )
        val newId = queries.selectLastInsertedClientId().executeAsOne().lastId?.toInt() ?: 0
        lastCreatedClientId = if (newId > 0) newId else null
        _clientCreated.emit(newId)
        return newId
    }

    fun clearLastCreatedClientId() {
        lastCreatedClientId = null
    }

    fun deleteClient(id: Int) {
        queries.deleteClient(id.toLong())
    }

    fun getClientById(id: Int): Client? {
        val entity = queries.selectClientById(id.toLong()).executeAsOneOrNull() ?: return null
        return Client(
            id = entity.id.toInt(),
            name = entity.name,
            taxId = entity.taxId ?: "",
            address = entity.address ?: "",
            phone = entity.phone ?: "",
            email = entity.email ?: "",
            notes = entity.notes ?: "",
            isVip = entity.is_vip == 1L
        )
    }

    fun updateClient(client: Client) {
        queries.updateClient(
            name = client.name,
            taxId = client.taxId,
            address = client.address,
            phone = client.phone,
            email = client.email,
            notes = client.notes,
            is_vip = if (client.isVip) 1L else 0L,
            id = client.id.toLong()
        )
    }
}
