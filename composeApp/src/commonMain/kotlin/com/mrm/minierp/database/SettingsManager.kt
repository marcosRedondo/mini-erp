package com.mrm.minierp.database

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

object SettingsManager {
    private val settings: Settings = Settings()
    
    private const val KEY_STORAGE_PATH = "storage_path"
    
    var storagePath: String?
        get() = settings.getStringOrNull(KEY_STORAGE_PATH)
        set(value) {
            if (value != null) {
                settings[KEY_STORAGE_PATH] = value
            } else {
                settings.remove(KEY_STORAGE_PATH)
            }
        }
}
