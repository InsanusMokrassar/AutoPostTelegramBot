package com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactions

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.mediumBroadcastCapacity
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.launch
import java.io.Closeable

class PostTransaction(
    private val postsTable: PostsTable,
    private val postsMessagesTable: PostsMessagesTable,
    val transactionStartedChannel: BroadcastChannel<Unit>,
    val transactionMessageAddedChannel: BroadcastChannel<Array<out PostMessage>>,
    val transactionMessageRemovedChannel: BroadcastChannel<PostMessage>,
    val transactionCompletedChannel: BroadcastChannel<Int>,
    private val scope: CoroutineScope
) : Closeable {
    private val messages = ArrayList<PostMessage>()

    var completed: Boolean = false
        private set

    init {
        scope.launch {
            transactionStartedChannel.send(Unit)
        }
    }

    fun addMessageId(vararg message: PostMessage) {
        if (completed) {
            throw IllegalStateException("Transaction already completed")
        }
        messages.addAll(message)

        scope.launch {
            transactionMessageAddedChannel.send(message)
        }
    }

    fun removeMessageId(message: PostMessage) {
        if (completed) {
            throw IllegalStateException("Transaction already completed")
        }
        messages.remove(message)

        scope.launch {
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
        val postId = postsTable.allocatePost()
        postsMessagesTable.addMessagesToPost(
            postId,
            *messagesIds
        )

        scope.launch {
            transactionCompletedChannel.send(postId)
        }
    }

    override fun close() {
        saveNewPost()
    }
}
