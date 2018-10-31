package com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers

import com.github.insanusmokrassar.AutoPostTelegramBot.callbackQueryListener
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribeChecking
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import java.lang.ref.WeakReference

abstract class UnsafeCallbackQueryReceiver(
    bot: TelegramBot
) : CallbackQueryReceiver {
    init {
        val botWR = WeakReference(bot)

        callbackQueryListener.subscribeChecking {
            invoke(
                it.second,
                botWR.get() ?: return@subscribeChecking false
            )
            true
        }
    }
}