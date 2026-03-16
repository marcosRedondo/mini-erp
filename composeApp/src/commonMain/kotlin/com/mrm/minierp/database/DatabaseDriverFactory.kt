package com.mrm.minierp.database

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory() {
    fun createDriver(dbPath: String?): SqlDriver
}
