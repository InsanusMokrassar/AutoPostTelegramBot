package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.bot.UpdatesPoller
import com.github.insanusmokrassar.TelegramBotAPI.types.ALL_UPDATES_LIST
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.Update
import com.github.insanusmokrassar.TelegramBotAPI.updateshandlers.KtorUpdatesPoller
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.UpdateReceiver
import kotlinx.serialization.Serializable

@Serializable
data class LongPollingConfig(
    val oneTimeLimit: Byte? = null,
    val responseAwaitMillis: Long? = 30 * 1000
) {
    fun applyTo(
        bot: RequestsExecutor,
        updatesReceiver: UpdateReceiver<Update>,
        allowedUpdates: List<String> = ALL_UPDATES_LIST,
        exceptionsReceiver: (Exception) -> Boolean = { true }
    ): UpdatesPoller {
        return KtorUpdatesPoller(
            bot,
            (responseAwaitMillis ?.div(1000)) ?.toInt(),
            oneTimeLimit ?.toInt(),
            allowedUpdates,
            exceptionsReceiver,
            updatesReceiver
        )
    }
}
