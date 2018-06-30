package com.github.insanusmokrassar.TimingPostsTelegramBot.InlineReceivers

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.pengrad.telegrambot.request.DeleteMessage
import java.lang.ref.WeakReference

private const val deleteIdentifier = "delete"

fun getDeleteReceiverPair(
    bot: TelegramBot
) = Pair(deleteIdentifier, DeleteReceiver(bot))

fun makeDeleteInline(postId: Int): String = "$deleteIdentifier: $postId"
fun extractDeleteInline(from: String): Int = from.toIObject().get<String>(deleteIdentifier).toInt()

class DeleteReceiver(
    bot: TelegramBot
) : UpdateCallback<CallbackQuery> {
    private val botWR = WeakReference(bot)

    override fun invoke(updateId: Int, messageIObject: IObject<Any>, query: CallbackQuery) {
        val postId = extractDeleteInline(
            query.data()
        )

        val messagesToDelete = mutableListOf(
            *PostsMessagesTable.getMessagesOfPost(postId).map { it.messageId }.toTypedArray(),
            PostsTable.postRegisteredMessage(postId)
        )

        PostsTable.removePost(postId)

        botWR.get() ?.let {
            bot ->
            messagesToDelete.filterNotNull().forEach {
                bot.executeAsync(
                    DeleteMessage(
                        query.message().chat().id(),
                        it
                    )
                )
            }
            bot.executeAsync(
                AnswerCallbackQuery(
                    query.id()
                ).text(
                    "Post was deleted"
                )
            )
        }
    }
}