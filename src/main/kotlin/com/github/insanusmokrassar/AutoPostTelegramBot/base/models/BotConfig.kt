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
    private var longPollingConfig: LongPollingConfig? = null
) {
    fun longPollingConfig(): LongPollingConfig = longPollingConfig ?.let {
        it.copy(
            responseAwaitMillis = it.responseAwaitMillis ?: clientConfig ?.readTimeout
        )
    } ?: LongPollingConfig(
        null,
        clientConfig ?.readTimeout
    ).also {
        longPollingConfig = it
    }

    fun createBot(): RequestsExecutor = KtorRequestsExecutor(
        botToken,
        OkHttp.create(clientConfig ?.builder ?: {})
    )
}
