package dev.inmo.AutoPostTelegramBot.utils.CallbackQueryReceivers

import dev.inmo.AutoPostTelegramBot.checkedCallbacksQueriesFlow
import dev.inmo.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.types.update.CallbackQueryUpdate
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