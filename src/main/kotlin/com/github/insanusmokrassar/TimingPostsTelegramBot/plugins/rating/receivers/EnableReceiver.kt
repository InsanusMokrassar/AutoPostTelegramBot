package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.receivers

import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.exceptions.NoRowFoundException
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.CallbackQueryReceivers.CallbackQueryReceiver
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.refreshRegisteredMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.realMessagesListener
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.queryAnswer
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

private const val enableIdentifier = "enableRatings"

fun makeEnableInline(postId: Int): String = "$enableIdentifier: $postId"
fun extractEnableInline(from: String): Int? = try {
    from.toIObject().get<String>(enableIdentifier).toInt()
} catch (e: ReadException) {
    null
}

private fun makeTextToApproveEnable(postId: Int) =
    "Please, write to me `${makeEnableInline(postId)}` if you want to enable ratings for this post"

class EnableReceiver(
    bot: TelegramBot,
    sourceChatId: Long,
    postsLikesTable: PostsLikesTable,
    postsLikesMessagesTable: PostsLikesMessagesTable
) : CallbackQueryReceiver(bot) {
    private val awaitApprove = HashMap<Long, Int>()

    init {
        realMessagesListener.broadcastChannel.openSubscription().also {
            launch {
                val botWR = WeakReference(bot)
                while (isActive) {
                    val message = it.receive()

                    val userId = message.second.chat().id()

                    val bot = botWR.get() ?: break
                    awaitApprove[userId] ?.let {
                        if (extractEnableInline(message.second.text()) == it) {
                            awaitApprove.remove(userId)
                            refreshRegisteredMessage(
                                sourceChatId,
                                bot,
                                it,
                                postsLikesTable,
                                postsLikesMessagesTable
                            )

                            bot.executeAsync(
                                SendMessage(
                                    userId,
                                    "Rating was enabled"
                                ).parseMode(
                                    ParseMode.Markdown
                                )
                            )
                        }
                    } ?:let {
                        val forwardFrom = message.second.forwardFromChat()
                        if (forwardFrom != null && forwardFrom.id() == sourceChatId) {
                            try {
                                val postId = PostsTable.findPost(
                                    message.second.forwardFromMessageId()
                                )
                                bot.executeAsync(
                                    SendMessage(
                                        userId,
                                        makeTextToApproveEnable(
                                            postId
                                        )
                                    ).parseMode(
                                        ParseMode.Markdown
                                    ),
                                    onResponse = {
                                        _, _ ->
                                        awaitApprove[userId] = postId
                                    }
                                )
                            } catch (e: NoRowFoundException) { }
                        }
                    }
                }
                it.cancel()
            }
        }
    }

    override fun invoke(
        query: CallbackQuery,
        bot: TelegramBot?
    ) {
        bot ?: return
        extractDisableInline(query.data())?.let {
            awaitApprove[query.from().id().toLong()] = it
            bot.queryAnswer(
                query.id(),
                makeTextToApproveEnable(it),
                true
            )
        }
    }
}