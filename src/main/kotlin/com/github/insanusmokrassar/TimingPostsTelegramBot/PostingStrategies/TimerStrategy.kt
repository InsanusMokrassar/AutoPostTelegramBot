package com.github.insanusmokrassar.TimingPostsTelegramBot.PostingStrategies

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.DeleteMessage
import com.pengrad.telegrambot.request.ForwardMessage
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

class TimerStrategy (
    private val targetChatId: Long,
    private val sourceChatId: Long,
    bot: TelegramBot,
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
                            PostsMessagesTable.getMessagesOfPost(postId).forEach {
                                messageId ->
                                bot.execute(
                                    ForwardMessage(
                                        targetChatId,
                                        sourceChatId,
                                        messageId
                                    )
                                )
                                bot.executeAsync(
                                    DeleteMessage(
                                        sourceChatId,
                                        messageId
                                    )
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
                        }
                    }
                }
            }
            delay(delayMs)
        }
    }
}
