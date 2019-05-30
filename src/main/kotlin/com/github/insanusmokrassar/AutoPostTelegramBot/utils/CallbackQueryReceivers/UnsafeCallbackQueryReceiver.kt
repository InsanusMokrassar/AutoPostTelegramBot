package com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers

import com.github.insanusmokrassar.AutoPostTelegramBot.flowFilter
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
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