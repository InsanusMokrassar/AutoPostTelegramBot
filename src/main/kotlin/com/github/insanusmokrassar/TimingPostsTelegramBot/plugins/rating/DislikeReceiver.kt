package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.CallbackQueryReceivers.CallbackQueryReceiverPlugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.queryAnswer
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extractDislikeInline
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery

class DislikeReceiver : CallbackQueryReceiverPlugin() {
    override val version: PluginVersion = 0L
    override fun invoke(
        query: CallbackQuery,
        bot: TelegramBot?
    ) {
        extractDislikeInline(
            query.data()
        ) ?.let {
            postId ->

            PostsLikesTable.userDislikePost(
                query.from().id().toLong(),
                postId
            )

            bot ?.queryAnswer(
                query.id(),
                "Voted"
            )
        }
    }
}