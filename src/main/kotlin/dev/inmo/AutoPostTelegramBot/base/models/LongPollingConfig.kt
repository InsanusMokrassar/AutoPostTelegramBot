package dev.inmo.AutoPostTelegramBot.base.models

import dev.inmo.micro_utils.coroutines.ExceptionHandler
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.api.webhook.deleteWebhook
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.startGettingOfUpdatesByLongPolling
import dev.inmo.tgbotapi.updateshandlers.UpdatesFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable

@Serializable
data class LongPollingConfig(
    val responseAwaitMillis: Long? = null
) {
    suspend fun startLongPollingListening(
        bot: RequestsExecutor,
        updatesFilter: UpdatesFilter,
        scope: CoroutineScope,
        exceptionsReceiver: ExceptionHandler<Unit>? = null
    ): Job {
        bot.deleteWebhook()
        return bot.startGettingOfUpdatesByLongPolling(
            updatesFilter,
            (responseAwaitMillis ?.div(1000)) ?.toInt() ?: 30,
            exceptionsReceiver,
            scope
        )
    }
}
