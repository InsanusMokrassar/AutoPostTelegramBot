package com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers

import com.github.insanusmokrassar.AutoPostTelegramBot.callbackQueryListener
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribeChecking
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import java.lang.ref.WeakReference

abstract class SafeCallbackQueryReceiver(
    bot: TelegramBot,
    checkChatId: Long
): CallbackQueryReceiver {
    init {
        val botWR = WeakReference(bot)

        callbackQueryListener.subscribeChecking {
            (_: Int, callbackQuery: CallbackQuery) ->
            if (callbackQuery.message().chat().id() == checkChatId) {
                invoke(
                    callbackQuery,
                    botWR.get() ?: return@subscribeChecking false
                )
            }
            true
        }
    }
}