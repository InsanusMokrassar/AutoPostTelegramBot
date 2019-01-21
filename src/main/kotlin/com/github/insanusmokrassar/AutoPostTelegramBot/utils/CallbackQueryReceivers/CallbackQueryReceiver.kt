package com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers

import com.github.insanusmokrassar.AutoPostTelegramBot.callbackQueryListener
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.CallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.update.CallbackQueryUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.Update
import java.lang.ref.WeakReference

private val CallbackQueryReceiversScope = NewDefaultCoroutineScope()

abstract class CallbackQueryReceiver(
    executor: RequestsExecutor
) {
    protected val executorWR = WeakReference(executor)

    init {
        callbackQueryListener.subscribe(
            scope = CallbackQueryReceiversScope,
            by = ::invoke
        )
    }

    abstract suspend fun invoke(update: CallbackQueryUpdate)
}