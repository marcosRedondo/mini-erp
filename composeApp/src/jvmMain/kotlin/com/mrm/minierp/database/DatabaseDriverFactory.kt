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
        
        // Crear tablas si no existen
        try {
            MiniErpDatabase.Schema.create(driver)
        } catch (e: Exception) {
            // La base de datos probablemente ya existe
        }
        
        return driver
    }
}
