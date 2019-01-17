package com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers

import com.github.insanusmokrassar.AutoPostTelegramBot.callbackQueryListener
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribeChecking
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import java.lang.ref.WeakReference

abstract class UnsafeCallbackQueryReceiver(
    executor: RequestsExecutor
) : CallbackQueryReceiver(
    executor
) {
    init {
        callbackQueryListener.subscribeChecking {
            invoke(it)
            true
        }
    }
}