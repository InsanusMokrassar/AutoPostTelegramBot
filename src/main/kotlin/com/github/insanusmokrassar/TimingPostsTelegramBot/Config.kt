package com.github.insanusmokrassar.TimingPostsTelegramBot

class Config (
    val targetChatId: String? = null,
    val sourceChatId: String? = null,
    val adminChatId: String? = null,
    val botToken: String? = null,
    val databaseConfig: DatabaseConfig? = null
) {
    val finalConfig: FinalConfig
        @Throws(IllegalArgumentException::class)
        get() = FinalConfig(
            targetChatId ?: throw IllegalArgumentException("Target chat id (field \"targetChatId\") can't be null"),
            sourceChatId ?: throw IllegalArgumentException("Source chat id (field \"sourceChatId\") can't be null"),
            botToken ?: throw IllegalArgumentException("Bot token (field \"botToken\") can't be null"),
            databaseConfig ?: throw IllegalArgumentException("Database config (field \"databaseConfig\") can't be null"),
            adminChatId
        )
}

data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String
)

class FinalConfig (
    val targetChatId: String,
    val sourceChatId: String,
    val botToken: String,
    val databaseConfig: DatabaseConfig,
    val adminChatId: String? = null
)
