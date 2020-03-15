package com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers

import com.github.insanusmokrassar.AutoPostTelegramBot.checkedCallbacksQueriesFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.types.update.CallbackQueryUpdate
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

private val CallbackQueryReceiversScope = NewDefaultCoroutineScope()

abstract class CallbackQueryReceiver(
    executor: RequestsExecutor
) {
    protected val executorWR = WeakReference(executor)

    init {
        CallbackQueryReceiversScope.launch {
            checkedCallbacksQueriesFlow.collectWithErrors(action = ::invoke)
        }
    }

    abstract suspend fun invoke(update: CallbackQueryUpdate)
}