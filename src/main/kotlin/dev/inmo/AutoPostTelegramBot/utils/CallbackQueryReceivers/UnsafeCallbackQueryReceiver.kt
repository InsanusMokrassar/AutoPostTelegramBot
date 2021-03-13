package dev.inmo.AutoPostTelegramBot.utils.CallbackQueryReceivers

import dev.inmo.AutoPostTelegramBot.flowFilter
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.bot.RequestsExecutor
import kotlinx.coroutines.*

abstract class UnsafeCallbackQueryReceiver(
    executor: RequestsExecutor
) : CallbackQueryReceiver(
    executor
) {
    init {
        CoroutineScope(Dispatchers.Default).launch {
            flowFilter.callbackQueryFlow.collectWithErrors {
                invoke(it)
            }
        }
    }
}