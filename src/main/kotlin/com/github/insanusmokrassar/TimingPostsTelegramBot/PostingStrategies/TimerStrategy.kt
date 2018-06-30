package com.github.insanusmokrassar.TimingPostsTelegramBot.PostingStrategies

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders.Forwarder
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.*
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

class TimerStrategy (
    private val targetChatId: Long,
    private val sourceChatId: Long,
    bot: TelegramBot,
    private val forwardersList: List<Forwarder>,
    private val delayMs: Long = 60 * 60 *1000
) {
    private val botWR = WeakReference(bot)

    val job = launch {
        while (isActive) {
            val bot = botWR.get() ?: return@launch
            synchronized(PostsTable) {
                synchronized(PostsMessagesTable) {
                    synchronized(PostsLikesTable) {
                        try {
                            PostsLikesTable.getMostRated().min()?.let { postId ->
                                val messagesToDelete = mutableListOf<Int>()

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

                                mapOfExecution.forEach {
                                    it.first.forward(
                                        bot,
                                        targetChatId,
                                        *it.second.toTypedArray()
                                    )
                                }

                                PostsTable.postRegisteredMessage(postId)?.let {
                                    bot.executeAsync(
                                        DeleteMessage(
                                            sourceChatId,
                                            it
                                        )
                                    )
                                }
                                PostsTable.removePost(postId)
                                messagesToDelete.forEach {
                                    bot.executeAsync(
                                        DeleteMessage(
                                            sourceChatId,
                                            it
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            delay(delayMs)
        }
    }
}
