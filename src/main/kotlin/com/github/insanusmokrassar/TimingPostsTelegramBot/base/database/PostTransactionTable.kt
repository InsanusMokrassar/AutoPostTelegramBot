package com.github.insanusmokrassar.TimingPostsTelegramBot.base.database

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.launch

private const val broadcastSubscriptions = 256

object PostTransactionTable {
    val transactionStartedChannel = BroadcastChannel<Unit>(broadcastSubscriptions)
    val transactionMessageAddedChannel = BroadcastChannel<PostMessage>(broadcastSubscriptions)
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

    fun addMessageId(message: PostMessage) {
        if (!inTransaction) {
            throw IllegalStateException("Not in transaction")
        }

        messages.add(message)

        launch {
            transactionMessageAddedChannel.send(message)
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
}