package dev.inmo.AutoPostTelegramBot.base.models

import dev.inmo.tgbotapi.bot.Ktor.KtorRequestsExecutor
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.utils.TelegramAPIUrlsKeeper
import dev.inmo.tgbotapi.utils.telegramBotAPIDefaultUrl
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class BotConfig(
    val botToken: String,
    val apiUrl: String = telegramBotAPIDefaultUrl,
    val clientConfig: HttpClientConfig? = null,
    val webhookConfig: WebhookConfig? = null,
    private var longPollingConfig: LongPollingConfig? = null
) {
    @Transient
    val telegramAPIUrlsKeeper = TelegramAPIUrlsKeeper(botToken, apiUrl)

    fun longPollingConfig(): LongPollingConfig = longPollingConfig ?.let {
        it.copy(
            responseAwaitMillis = it.responseAwaitMillis ?: clientConfig ?.readTimeout
        )
    } ?: LongPollingConfig(
        clientConfig ?.readTimeout
    ).also {
        longPollingConfig = it
    }

    fun createBot(): RequestsExecutor = KtorRequestsExecutor(
        telegramAPIUrlsKeeper,
        HttpClient(OkHttp.create(clientConfig ?.builder ?: {}))
    )
}
