package com.github.insanusmokrassar.TimingPostsTelegramBot

class Config (
    val targetChatId: String? = null,
    val sourceChatId: String? = null,
    val adminChatId: String? = null,
    val botToken: String? = null
) {
    val finalConfig: FinalConfig
        get() = FinalConfig(
            targetChatId ?: throw IllegalArgumentException("Target chat id can't be null"),
            sourceChatId ?: throw IllegalArgumentException("Source chat id can't be null"),
            botToken ?: throw IllegalArgumentException("Bot token can't be null"),
            adminChatId
        )
}

class FinalConfig (
    val targetChatId: String,
    val sourceChatId: String,
    val botToken: String,
    val adminChatId: String? = null
)
