package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.TelegramBotAPI.bot.Ktor.KtorRequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val botToken: String,
    val clientConfig: HttpClientConfig? = null,
    val webhookConfig: WebhookConfig? = null,
    val longPollingConfig: LongPollingConfig? = null
) {
    fun createBot(): RequestsExecutor = KtorRequestsExecutor(
        botToken,
        OkHttp.create(clientConfig ?.builder ?: {})
    )
}
