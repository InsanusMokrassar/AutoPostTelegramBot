package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.abstracts.toInputFile
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class WebhookConfig(
    val url: String,
    val port: Int,
    @Optional
    val certificatePath: String? = null,
    @Optional
    val maxConnections: Int? = null,
    @Optional
    val privateKeyConfig: WebhookPrivateKeyConfig? = null
) {
    suspend fun setWebhook(
        requestsExecutor: RequestsExecutor,
        filter: UpdatesFilter,
        scope: CoroutineScope = NewDefaultCoroutineScope(4)
    ): Job = requestsExecutor.setWebhook(
        url,
        port,
        filter,
        certificatePath ?.let { File(it).toInputFile() },
        privateKeyConfig,
        scope,
        maxConnections
    )
}