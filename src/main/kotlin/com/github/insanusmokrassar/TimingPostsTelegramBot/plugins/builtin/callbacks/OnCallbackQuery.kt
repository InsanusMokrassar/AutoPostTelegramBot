package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.callbacks

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.InlineReceivers.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.callbackQueryListener
import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.publishers.Publisher
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import kotlinx.coroutines.experimental.launch
import java.util.logging.Logger

private val logger = Logger.getLogger(Plugin::class.java.simpleName)

class OnCallbackQuery : Plugin {

    override fun init(
        baseConfig: FinalConfig,
        chooser: Chooser,
        publisher: Publisher,
        bot: TelegramBot
    ) {
        callbackQueryListener.broadcastChannel.openSubscription().also {
            val queriesMap = mapOf(
                getLikeReceiverPair(bot),
                getDislikeReceiverPair(bot),
                getDeleteReceiverPair(bot)
            )
            launch {
                while (isActive) {
                    val received = it.receive()
                    try {
                        invoke(
                            received.first,
                            received.second,
                            queriesMap
                        )
                    } catch (e: Exception) {
                        logger.throwing(
                            OnCallbackQuery::class.java.canonicalName,
                            "Perform message",
                            e
                        )
                    }
                }
                it.cancel()
            }
        }
    }

    private fun invoke(
        updateId: Int,
        query: CallbackQuery,
        queriesMap: Map<String, UpdateCallback<CallbackQuery>>
    ) {
        query.data().toIObject().let {
            it.keys().mapNotNull {
                queriesMap[it]
            }.forEach {
                it(updateId, query)
            }
        }
    }
}
