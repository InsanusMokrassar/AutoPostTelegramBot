package dev.inmo.AutoPostTelegramBot.base.models

import dev.inmo.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import dev.inmo.micro_utils.coroutines.ExceptionHandler
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.setWebhookInfoAndStartListenWebhooks
import dev.inmo.tgbotapi.requests.abstracts.toInputFile
import dev.inmo.tgbotapi.requests.webhook.SetWebhook
import dev.inmo.tgbotapi.updateshandlers.UpdatesFilter
import dev.inmo.tgbotapi.updateshandlers.webhook.WebhookPrivateKeyConfig
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