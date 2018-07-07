package com.github.insanusmokrassar.TimingPostsTelegramBot.publishers

import com.github.insanusmokrassar.TimingPostsTelegramBot.commands.deletePost
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders.Forwarder
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.*
import java.lang.ref.WeakReference

private typealias ChatIdMessageIdPair = Pair<Long, Int>

class PostPublisher(
    private val targetChatId: Long,
    private val sourceChatId: Long,
    bot: TelegramBot,
    private val logsChatId: Long = sourceChatId,
    private val forwardersList: List<Forwarder>
) : Publisher {
    private val botWR = WeakReference(bot)

    override fun publishPost(postId: Int) {
        val bot = botWR.get() ?: return

        val messagesToDelete = mutableListOf<ChatIdMessageIdPair>()

        try {
            bot.execute(
                SendMessage(
                    logsChatId,
                    "Start post"
                )
            )?.message() ?.let {
                messagesToDelete.add(it.chat().id() to it.messageId())
            }

            val messageToPost = PostsMessagesTable.getMessagesOfPost(postId).also {
                if (it.isEmpty()) {
                    PostsTable.removePost(postId)
                    return
                }
                it.forEach {
                    message ->
                    messagesToDelete.add(sourceChatId to message.messageId)

                    bot.execute(
                        ForwardMessage(
                            logsChatId,
                            sourceChatId,
                            message.messageId
                        ).disableNotification(
                            true
                        )
                    ) ?.message() ?.also {
                        messagesToDelete.add(it.chat().id() to it.messageId())
                        message.message = it
                    }
                }
            }

            val mapOfExecution = mutableListOf<Pair<Forwarder, MutableList<PostMessage>>>()

            var forwarder: Forwarder? = null

            messageToPost.forEach { message ->
                if (forwarder?.canForward(message) != true) {
                    val iterator = forwardersList.iterator()
                    while (forwarder?.canForward(message) != true) {
                        if (!iterator.hasNext()) {
                            return@forEach
                        }
                        forwarder = iterator.next()
                    }
                }
                if (mapOfExecution.lastOrNull()?.first != forwarder) {
                    forwarder?.let {
                        mapOfExecution.add(
                            it to mutableListOf()
                        )
                    }
                }
                mapOfExecution.last().second.add(message)
            }

            mapOfExecution.flatMap {
                it.first.forward(
                    bot,
                    targetChatId,
                    *it.second.toTypedArray()
                )
            }.let {
                bot.execute(
                    SendMessage(
                        logsChatId,
                        "Post published. Rating: ${PostsLikesTable.getPostRating(postId)}"
                    ).parseMode(
                        ParseMode.Markdown
                    )
                )
                it.forEach {
                    bot.execute(
                        ForwardMessage(
                            logsChatId,
                            targetChatId,
                            it
                        )
                    )
                }
            }

            deletePost(
                bot,
                sourceChatId,
                postId
            )
        } catch (e: Exception) {
            e.printStackTrace()
            bot.executeAsync(
                SendMessage(
                    sourceChatId,
                    "Can't publish post:\n```$e```"
                ).parseMode(
                    ParseMode.Markdown
                )
            )
        } finally {
            messagesToDelete.forEach {
                bot.executeAsync(
                    DeleteMessage(
                        it.first,
                        it.second
                    )
                )
            }
        }
    }
}