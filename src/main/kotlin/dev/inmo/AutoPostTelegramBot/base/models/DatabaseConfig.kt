package dev.inmo.AutoPostTelegramBot.base.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.sql.Database

@Serializable
data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String,
    val initAutomatically: Boolean = true
) {
    @Transient
    private lateinit var _database: Database
    val database: Database
        get() = try {
            _database
        } catch (e: UninitializedPropertyAccessException) {
            Database.connect(
                url,
                driver,
                username,
                password
            ).also {
                _database = it
            }
        }

    init {
        if (initAutomatically) {
            database // init database
        }
    }

    @Deprecated(
        "Deprecated due to the replacement by lateinit database field with the same functionality",
        ReplaceWith("database")
    )
    fun connect(): Database {
        return database
    }
}
