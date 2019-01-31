package com.github.insanusmokrassar.AutoPostTelegramBot.base.database

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.UnlimitedBroadcastChannel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.Closeable

val transactionStartedChannel = UnlimitedBroadcastChannel<Unit>()
val transactionMessageAddedChannel = UnlimitedBroadcastChannel<Array<out PostMessage>>()
val transactionMessageRemovedChannel = UnlimitedBroadcastChannel<PostMessage>()
val transactionCompletedChannel = UnlimitedBroadcastChannel<Int>()

val PostTransactionsScope = NewDefaultCoroutineScope()

class PostTransaction : Closeable {
    private val messages = ArrayList<PostMessage>()

    var completed: Boolean = false
        private set

    init {
        PostTransactionsScope.launch {
            transactionStartedChannel.send(Unit)
        }
    }

    fun addMessageId(vararg message: PostMessage) {
        if (completed) {
            throw IllegalStateException("Transaction already completed")
        }
        messages.addAll(message)

        PostTransactionsScope.launch {
            transactionMessageAddedChannel.send(message)
        }
    }

    fun removeMessageId(message: PostMessage) {
        if (completed) {
            throw IllegalStateException("Transaction already completed")
        }
        messages.remove(message)

        PostTransactionsScope.launch {
            transactionMessageRemovedChannel.send(message)
        }
    }

    private fun completeTransaction(): List<PostMessage> {
        if (completed) {
            throw IllegalStateException("Transaction already completed")
        }
        return listOf(*messages.toTypedArray()).also {
            messages.clear()
            completed = true
        }
    }

    @Throws(NothingToSaveException::class)
    fun saveNewPost() {
        val messagesIds = completeTransaction().toTypedArray()
        if (messagesIds.isEmpty()) {
            throw NothingToSaveException("No messages for saving")
        }
        val postId = PostsTable.allocatePost()
        PostsMessagesTable.addMessagesToPost(
            postId,
            *messagesIds
        )

        PostTransactionsScope.launch {
            transactionCompletedChannel.send(postId)
        }
    }

    override fun close() {
        saveNewPost()
    }
}