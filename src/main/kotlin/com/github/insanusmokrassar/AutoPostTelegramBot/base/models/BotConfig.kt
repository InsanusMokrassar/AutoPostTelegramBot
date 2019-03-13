package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.TelegramBotAPI.bot.Ktor.KtorRequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val botToken: String,
    @Optional
    val clientConfig: HttpClientConfig? = null,
    @Optional
    val webhookConfig: WebhookConfig? = null
) {
    fun createBot(): RequestsExecutor = KtorRequestsExecutor(
        botToken,
        OkHttp.create(clientConfig ?.builder ?: {})
    )
}
