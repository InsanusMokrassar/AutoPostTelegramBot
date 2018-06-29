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

private const val dislikeIdentifier = "dislike"

fun getDislikeReceiverPair(
    bot: TelegramBot
) = Pair(dislikeIdentifier, DislikeReceiver(bot))

fun makeDislikeInline(postId: Int): String = "$dislikeIdentifier: $postId"
fun extractDislikeInline(from: String): Int = from.toIObject().get<String>(dislikeIdentifier).toInt()

class DislikeReceiver(
    bot: TelegramBot
) : UpdateCallback<CallbackQuery> {
    private val botWR = WeakReference(bot)

    override fun invoke(updateId: Int, messageIObject: IObject<Any>, query: CallbackQuery) {
        val postId = extractDislikeInline(
            query.data()
        )

        PostsLikesTable.userDislikePost(
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
                bot,
                query.message().messageId()
            )
        }
    }
}