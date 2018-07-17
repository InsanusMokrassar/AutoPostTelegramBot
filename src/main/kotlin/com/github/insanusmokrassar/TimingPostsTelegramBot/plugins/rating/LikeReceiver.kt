package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating

import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.CallbackQueryReceivers.CallbackQueryReceiver
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.queryAnswer
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery

const val like = "\uD83D\uDC4D"

internal fun makeLikeText(likes: Int) = "$like $likes"

private const val likeIdentifier = "like"

fun makeLikeInline(postId: Int): String = "$likeIdentifier: $postId"
fun extractLikeInline(from: String): Int? = try {
    from.toIObject().get<String>(likeIdentifier).toInt()
} catch (e: ReadException) {
    null
}

class LikeReceiver(
    bot: TelegramBot,
    private val postsLikesTable: PostsLikesTable
) : CallbackQueryReceiver(bot) {
    override fun invoke(
        query: CallbackQuery,
        bot: TelegramBot?
    ) {
        extractLikeInline(
            query.data()
        )?.let {
            postId ->

            postsLikesTable.userLikePost(
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