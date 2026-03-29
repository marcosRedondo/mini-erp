package com.mrm.minierp.database

import com.mrm.minierp.models.Company

class CompanyRepository(private val database: MiniErpDatabase) {
    private val queries = database.appDatabaseQueries

    fun getCompany(): Company {
        val row = queries.getCompany().executeAsOneOrNull() ?: return Company("")
        return Company(
            name = row.name,
            logoBase64 = row.logoBase64,
            nif = row.nif ?: "",
            phone = row.phone ?: "",
            address = row.address ?: ""
        )
    }

    fun updateCompany(company: Company) {
        queries.updateCompany(
            name = company.name,
            logoBase64 = company.logoBase64,
            nif = company.nif,
            phone = company.phone,
            address = company.address
        )
    }
}
