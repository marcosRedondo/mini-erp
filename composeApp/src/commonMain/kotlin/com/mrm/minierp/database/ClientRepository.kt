package com.mrm.minierp.database

import com.mrm.minierp.models.Client

class ClientRepository(private val database: MiniErpDatabase) {
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

    fun saveClient(client: Client) {
        queries.insertClient(
            name = client.name,
            taxId = client.taxId,
            address = client.address,
            phone = client.phone,
            email = client.email,
            notes = client.notes,
            is_vip = if (client.isVip) 1L else 0L
        )
    }

    fun deleteClient(id: Int) {
        queries.deleteClient(id.toLong())
    }
}
