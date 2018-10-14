package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.pengrad.telegrambot.TelegramBot

data class BotConfig(
    val botToken: String? = null,
    val clientConfig: HttpClientConfig? = null,
    val regen: RequestsRegenConfig? = null
) {
    fun createBot(): TelegramBot {
        return TelegramBot.Builder(
            botToken ?: throw IllegalArgumentException("Bot token (field \"botToken\") can't be null")
        ).apply {
            clientConfig ?.also {
                okHttpClient(
                    it.createClient()
                )
            }
        }.build().also {
            regen ?.also {
                _ ->
                regen.applyFor(it)
            }
        }
    }
}
