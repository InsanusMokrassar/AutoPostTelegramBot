package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.CallbackQueryReceivers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.queryAnswer
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extractLikeInline
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery

class LikeReceiver : CallbackQueryReceiverPlugin() {
    override fun invoke(
        query: CallbackQuery,
        bot: TelegramBot?
    ) {
        extractLikeInline(
            query.data()
        ) ?.let {
            postId ->

            PostsLikesTable.userLikePost(
                query.from().id().toLong(),
                postId
            )

            bot ?. queryAnswer(
                query.id(),
                "Voted"
            )
        }
    }
}