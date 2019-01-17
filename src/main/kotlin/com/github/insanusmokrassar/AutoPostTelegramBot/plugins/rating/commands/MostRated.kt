package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.ForwardMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

private val MostRatedScope = NewDefaultCoroutineScope(1)

class MostRated(
    private val botWR: WeakReference<RequestsExecutor>,
    private val postsLikesTable: PostsLikesTable
): Command() {
    override val commandRegex: Regex = Regex("^/mostRated$")

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val bot = botWR.get() ?: return
        val mostRated = postsLikesTable.getMostRated()
        val chatId = message.chat.id

        /*message.chat.username() ?.let {
                username ->
            bot.executeAsync(
                SendMessage(
                    chatId,
                    "Most rated posts"
                ).replyMarkup(
                    InlineKeyboardMarkup(
                        *mostRated.mapNotNull {
                            PostsTable.postRegisteredMessage(it)
                        }.mapIndexed {
                                index, id ->
                            InlineKeyboardButton(
                                (index + 1).toString()
                            ).url(
                                makeLinkToMessage(
                                    username,
                                    id
                                )
                            )
                        }.toTable(4)
                    )
                )
            )
        }*/
        MostRatedScope.launch {
            mostRated.mapNotNull {
                PostsTable.postRegisteredMessage(it)
            }.forEach {
                bot.execute(
                    ForwardMessage(
                        chatId,
                        chatId,
                        it
                    )
                )
            }
        }
    }
}