package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database

@Serializable
data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String
) {
    fun connect(): Database {
        return Database.connect(
            url,
            driver,
            username,
            password
        )
    }
}
