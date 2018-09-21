package com.github.insanusmokrassar.AutoPostTelegramBot.base.database

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.launch
import java.io.Closeable

private const val broadcastSubscriptions = 256

object PostTransactionTable : Closeable {
    val transactionStartedChannel = BroadcastChannel<Unit>(broadcastSubscriptions)
    val transactionMessageAddedChannel = BroadcastChannel<Array<out PostMessage>>(broadcastSubscriptions)
    val transactionMessageRemovedChannel = BroadcastChannel<PostMessage>(broadcastSubscriptions)
    val transactionCompletedChannel = BroadcastChannel<Int>(broadcastSubscriptions)

    private val messages = ArrayList<PostMessage>()
    var inTransaction: Boolean = false
        private set

    fun startTransaction() {
        if (inTransaction) {
            throw IllegalStateException("Already in transaction")
        }
        messages.clear()
        inTransaction = true

        launch {
            transactionStartedChannel.send(Unit)
        }
    }

    fun addMessageId(vararg message: PostMessage) {
        if (!inTransaction) {
            throw IllegalStateException("Not in transaction")
        }

        messages.addAll(message)

        launch {
            transactionMessageAddedChannel.send(message)
        }
    }

    fun removeMessageId(message: PostMessage) {
        if (!inTransaction) {
            throw IllegalStateException("Not in transaction")
        }

        messages.remove(message)

        launch {
            transactionMessageRemovedChannel.send(message)
        }
    }

    private fun completeTransaction(): List<PostMessage> {
        return listOf(*messages.toTypedArray()).also {
            messages.clear()
            inTransaction = false
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

        launch {
            transactionCompletedChannel.send(postId)
        }
    }
    override fun close() {
        saveNewPost()
    }
}