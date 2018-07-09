package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.CallbackQueryReceivers

import com.github.insanusmokrassar.TimingPostsTelegramBot.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.callbackQueryListener
import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.publishers.Publisher
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

abstract class CallbackQueryReceiverPlugin : Plugin {
    override fun init(baseConfig: FinalConfig, chooser: Chooser, publisher: Publisher, bot: TelegramBot) {
        val botWR = WeakReference(bot)

        callbackQueryListener.broadcastChannel.openSubscription().also {
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