package com.mrm.minierp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
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
            val currentVersion = MiniErpDatabase.Schema.version
            
            // Consultar versión actual del archivo DB (SQLite persistente)
            // PRAGMA user_version devuelve un long
            val result = driver.executeQuery(null, "PRAGMA user_version", { cursor ->
                val v = if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L
                QueryResult.Value(v)
            }, 0)
            var oldVersion = result.value ?: 0L
            
            // Si la versión es 0, comprobamos si ya existen tablas (de versiones anteriores sin user_version)
            if (oldVersion == 0L) {
                val tableExists = driver.executeQuery(null, 
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='ClientEntity'",
                    { cursor -> QueryResult.Value(cursor.next().value) }, 0).value
                if (tableExists == true) {
                    oldVersion = 1L // Asumimos que viene de la versión 1 inicial
                }
            }
            
            if (oldVersion == 0L) {
                // Crear desde cero
                MiniErpDatabase.Schema.create(driver)
                driver.execute(null, "PRAGMA user_version = $currentVersion", 0)
                println("DEBUG: Database created successfully (Version $currentVersion)")
            } else if (oldVersion < currentVersion) {
                // Migrar
                MiniErpDatabase.Schema.migrate(driver, oldVersion, currentVersion)
                driver.execute(null, "PRAGMA user_version = $currentVersion", 0)
                println("DEBUG: Database migrated from $oldVersion to $currentVersion")
            } else {
                println("DEBUG: Database version $oldVersion is up to date")
            }
        } catch (e: Exception) {
            println("DEBUG: Error initializing database: ${e.message}")
            e.printStackTrace()
        }
        
        return driver
    }
}
