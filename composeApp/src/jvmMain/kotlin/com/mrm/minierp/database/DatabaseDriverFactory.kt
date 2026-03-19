package com.mrm.minierp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(dbPath: String?): SqlDriver {
        val databaseFile = if (dbPath != null) {
            File(dbPath, "minierp.db")
        } else {
            File(System.getProperty("user.home"), ".minierp/minierp.db")
        }
        
        databaseFile.parentFile?.mkdirs()
        
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        
        // Inicializar esquema
        try {
            // Versión actual del esquema generado por SqlDelight
            val currentVersion = MiniErpDatabase.Schema.version
            
            // Aquí en modo desarrollo, si falla el create, simplemente lo ignoramos,
            // pero lo ideal sería llamar a Schema.migrate si detectamos versión antigua.
            // Para un ERP, mejor forzar que las tablas existan:
            MiniErpDatabase.Schema.create(driver)
            println("DEBUG: Database schema created successfully (Version $currentVersion)")
        } catch (e: Exception) {
            // Si ya existe, no hacemos nada (por ahora)
            // println("DEBUG: Database already exists or error during creation: ${e.message}")
        }
        
        return driver
    }
}
