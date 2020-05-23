package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.sendToLogger
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.updates.retrieving.setWebhookInfoAndStartListenWebhooks
import com.github.insanusmokrassar.TelegramBotAPI.requests.abstracts.toInputFile
import com.github.insanusmokrassar.TelegramBotAPI.requests.webhook.SetWebhook
import com.github.insanusmokrassar.TelegramBotAPI.updateshandlers.UpdatesFilter
import com.github.insanusmokrassar.TelegramBotAPI.updateshandlers.webhook.WebhookPrivateKeyConfig
import com.github.insanusmokrassar.TelegramBotAPI.utils.ExceptionHandler
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class WebhookConfig(
    val url: String,
    val port: Int,
    val certificatePath: String? = null,
    val maxConnections: Int? = null,
    val privateKeyConfig: WebhookPrivateKeyConfig? = null
) {
    suspend fun startWebhookServer(
        bot: RequestsExecutor,
        filter: UpdatesFilter,
        scope: CoroutineScope = NewDefaultCoroutineScope(4),
        exceptionsHandler: ExceptionHandler<Unit>? = null
    ): ApplicationEngine = certificatePath ?.let { File(it).toInputFile() } ?.let {
        bot.setWebhookInfoAndStartListenWebhooks(
            port,
            CIO,
            SetWebhook(
                url,
                it,
                allowedUpdates = filter.allowedUpdates,
                maxAllowedConnections = maxConnections
            ),
            exceptionsHandler ?: {},
            privateKeyConfig = privateKeyConfig,
            scope = scope,
            block = filter.asUpdateReceiver
        )
    } ?: bot.setWebhookInfoAndStartListenWebhooks(
        port,
        CIO,
        SetWebhook(
            url,
            allowedUpdates = filter.allowedUpdates,
            maxAllowedConnections = maxConnections
        ),
        exceptionsHandler ?: {},
        privateKeyConfig = privateKeyConfig,
        scope = scope,
        block = filter.asUpdateReceiver
    )
}