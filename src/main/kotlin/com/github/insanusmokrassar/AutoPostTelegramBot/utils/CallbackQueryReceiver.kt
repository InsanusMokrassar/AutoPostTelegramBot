package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.callbackQueryListener
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

abstract class CallbackQueryReceiver(
    bot: TelegramBot
) {
    init {
        val botWR = WeakReference(bot)

        callbackQueryListener.subscribe {
            invoke(
                it.second,
                botWR.get()
            )
        }
    }

    protected abstract fun invoke(
        query: CallbackQuery,
        bot: TelegramBot?
    )
}