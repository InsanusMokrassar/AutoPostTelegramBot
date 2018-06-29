package com.github.insanusmokrassar.TimingPostsTelegramBot.InlineReceivers

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.queryAnswer
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.refreshRegisteredMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import java.lang.ref.WeakReference

private const val likeIdentifier = "like"

fun getLikeReceiverPair(
    bot: TelegramBot
) = Pair(likeIdentifier, LikeReceiver(bot))

fun makeLikeInline(postId: Int): String = "$likeIdentifier: $postId"
fun extractLikeInline(from: String): Int = from.toIObject().get<String>(likeIdentifier).toInt()

class LikeReceiver(
    bot: TelegramBot
) : UpdateCallback<CallbackQuery> {
    private val botWR = WeakReference(bot)

    override fun invoke(updateId: Int, messageIObject: IObject<Any>, query: CallbackQuery) {
        val postId = extractLikeInline(
            query.data()
        )

        PostsLikesTable.userLikePost(
            query.from().id().toLong(),
            postId
        )

        botWR.get() ?. let {
            bot ->
            bot.queryAnswer(
                query.id(),
                "Voted"
            )
            refreshRegisteredMessage(
                query.message().chat(),
                postId,
                bot
            )
        }
    }
}