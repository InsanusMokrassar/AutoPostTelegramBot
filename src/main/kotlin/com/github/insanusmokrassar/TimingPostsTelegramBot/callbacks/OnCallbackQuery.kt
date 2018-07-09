package com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.InlineReceivers.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery

class OnCallbackQuery(
    bot: TelegramBot
) : UpdateCallback<CallbackQuery> {
    private val queriesMap = mapOf(
        getLikeReceiverPair(bot),
        getDislikeReceiverPair(bot),
        getDeleteReceiverPair(bot)
    )

    override fun invoke(updateId: Int, query: CallbackQuery) {
        query.data().toIObject().let {
            it.keys().mapNotNull {
                queriesMap[it]
            }.forEach {
                it(updateId, query)
            }
        }
    }
}
