package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.updates.retrieving.startGettingOfUpdatesByLongPolling
import com.github.insanusmokrassar.TelegramBotAPI.types.ALL_UPDATES_LIST
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.Update
import com.github.insanusmokrassar.TelegramBotAPI.updateshandlers.UpdateReceiver
import com.github.insanusmokrassar.TelegramBotAPI.updateshandlers.UpdatesFilter
import com.github.insanusmokrassar.TelegramBotAPI.utils.ExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable

@Serializable
data class LongPollingConfig(
    val oneTimeLimit: Byte? = null,
    val responseAwaitMillis: Long? = null
) {
    fun applyTo(
        bot: RequestsExecutor,
        updatesFilter: UpdatesFilter,
        exceptionsReceiver: ExceptionHandler<Unit>? = null
    ): Job {
        return bot.startGettingOfUpdatesByLongPolling(
            updatesFilter,
            (responseAwaitMillis ?.div(1000)) ?.toInt() ?: 30,
            exceptionsReceiver
        )
    }
}
