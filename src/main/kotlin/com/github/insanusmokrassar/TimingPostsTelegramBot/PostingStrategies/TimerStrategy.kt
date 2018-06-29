package com.github.insanusmokrassar.TimingPostsTelegramBot.PostingStrategies

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders.Forwarder
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
                        PostsLikesTable.getMostRated().min() ?.let {
                            postId ->
                            val messagesToDelete = mutableListOf<Int>()

                            bot.execute(
                                SendMessage(
                                    sourceChatId,
                                    "Start post"
                                )
                            ) ?. message() ?. messageId() ?.let {
                                messagesToDelete.add(it)
                            }

                            val messageToPost = PostsMessagesTable.getMessagesOfPost(postId).mapNotNull {
                                messageId ->
                                messagesToDelete.add(messageId)

                                bot.execute(
                                    ForwardMessage(
                                        sourceChatId,
                                        sourceChatId,
                                        messageId
                                    )
                                ).let {
                                    it.message() ?.also {
                                        messagesToDelete.add(it.messageId())
                                    }
                                }
                            }

                            val mapOfExecution = mutableListOf<Pair<Forwarder, MutableList<Message>>>()

                            var iterator = forwardersList.iterator()
                            var forwarder: Forwarder? = null

                            messageToPost.forEach {
                                message ->
                                while (forwarder ?. canForward(message) != true) {
                                    if (!iterator.hasNext()) {
                                        iterator = forwardersList.iterator()
                                    }
                                    iterator.next().let {
                                        mapOfExecution.add(
                                            it to mutableListOf()
                                        )
                                        forwarder = it
                                    }
                                }
                                mapOfExecution.first().second.add(message)
                            }

                            mapOfExecution.forEach {
                                it.first(
                                    bot,
                                    targetChatId,
                                    it.second
                                )
                            }

                            PostsTable.postRegisteredMessage(postId) ?.let {
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
                    }
                }
            }
            delay(delayMs)
        }
    }
}
