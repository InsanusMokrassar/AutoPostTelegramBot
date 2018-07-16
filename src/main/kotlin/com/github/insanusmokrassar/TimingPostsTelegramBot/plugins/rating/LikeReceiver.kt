package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating

import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.CallbackQueryReceivers.CallbackQueryReceiver
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.queryAnswer
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery

class LikeReceiver(bot: TelegramBot) : CallbackQueryReceiver(bot) {
    override fun invoke(
        query: CallbackQuery,
        bot: TelegramBot?
    ) {
        extractLikeInline(
            query.data()
        )?.let {
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