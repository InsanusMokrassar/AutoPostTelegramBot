package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.CallbackQueryReceivers

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.queryAnswer
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extractDislikeInline
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery

class DislikeReceiver : CallbackQueryReceiverPlugin() {
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