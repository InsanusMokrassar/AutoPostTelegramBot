package com.github.insanusmokrassar.TimingPostsTelegramBot.publishers

import com.github.insanusmokrassar.TimingPostsTelegramBot.commands.deletePost
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders.Forwarder
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.*
import java.lang.ref.WeakReference

class PostPublisher(
    private val targetChatId: Long,
    private val sourceChatId: Long,
    bot: TelegramBot,
    private val forwardersList: List<Forwarder>
) : Publisher {
    private val botWR = WeakReference(bot)

    override fun publishPost(postId: Int) {
        val bot = botWR.get() ?: return

        val messagesToDelete = mutableListOf<Int>()

        try {
            bot.execute(
                SendMessage(
                    sourceChatId,
                    "Start post"
                )
            )?.message()?.messageId()?.let {
                messagesToDelete.add(it)
            }

            val messageToPost = PostsMessagesTable.getMessagesOfPost(postId).also {
                it.forEach { message ->
                    messagesToDelete.add(message.messageId)

                    bot.execute(
                        ForwardMessage(
                            sourceChatId,
                            sourceChatId,
                            message.messageId
                        ).disableNotification(
                            true
                        )
                    ).also {
                        messagesToDelete.add(it.message().messageId())
                        message.message = it.message()
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
                        sourceChatId,
                        "Post published. Rating: ${PostsLikesTable.getPostRating(postId)}"
                    ).parseMode(
                        ParseMode.Markdown
                    )
                )
                it.forEach {
                    bot.execute(
                        ForwardMessage(
                            sourceChatId,
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
        } finally {
            messagesToDelete.forEach {
                bot.executeAsync(
                    DeleteMessage(
                        sourceChatId,
                        it
                    )
                )
            }
        }
    }
}