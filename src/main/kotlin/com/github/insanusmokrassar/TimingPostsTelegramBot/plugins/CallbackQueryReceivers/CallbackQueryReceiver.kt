package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.CallbackQueryReceivers

import com.github.insanusmokrassar.TimingPostsTelegramBot.callbackQueryListener
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

abstract class CallbackQueryReceiver(
    bot: TelegramBot
) {
    init {
        val botWR = WeakReference(bot)

        callbackQueryListener.openSubscription().also {
            launch {
                while (isActive) {
                    val received = it.receive()
                    try {
                        invoke(
                            received.second,
                            botWR.get()
                        )
                    } catch (e: Exception) {

                    }
                }
                it.cancel()
            }
        }
    }

    protected abstract fun invoke(
        query: CallbackQuery,
        bot: TelegramBot?
    )
}