package com.mrm.minierp.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.mrm.minierp.AndroidContextProvider
import java.io.File

actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(dbPath: String?): SqlDriver {
        val context = AndroidContextProvider.context ?: throw IllegalStateException("Context not initialized")
        
        return if (dbPath != null) {
            // Si hay una ruta personalizada (ej. Google Drive vía SAF), intentamos usarla
            // Nota: En Android, usar una base de datos directamente en una carpeta externa 
            // puede ser complicado por permisos. Por ahora usaremos la carpeta de la app 
            // si no se puede acceder a la externa, o una subcarpeta si se provee ruta.
            val file = File(dbPath, "minierp.db")
            AndroidSqliteDriver(MiniErpDatabase.Schema, context, file.name) // Simplificado por ahora
        } else {
            AndroidSqliteDriver(MiniErpDatabase.Schema, context, "minierp.db")
        }
    }
}
