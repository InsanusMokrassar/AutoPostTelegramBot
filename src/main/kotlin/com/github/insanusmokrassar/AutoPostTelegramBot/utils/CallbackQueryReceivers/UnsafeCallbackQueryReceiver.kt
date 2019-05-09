package com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers

import com.github.insanusmokrassar.AutoPostTelegramBot.checkedCallbackQueryFlow
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

abstract class UnsafeCallbackQueryReceiver(
    executor: RequestsExecutor
) : CallbackQueryReceiver(
    executor
) {
    init {
        CoroutineScope(Dispatchers.Default).launch {
            checkedCallbackQueryFlow.collect(::invoke)
        }
    }
}