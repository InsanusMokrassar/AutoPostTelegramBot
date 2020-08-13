package com.github.insanusmokrassar.AutoPostTelegramBot.base.database

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.*
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.extraLargeBroadcastCapacity
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import java.io.Closeable

private val transactionStartedChannel = BroadcastChannel<PostTransaction>(extraLargeBroadcastCapacity)
val transactionStartedFlow = transactionStartedChannel.asFlow()
private val transactionMessageAddedChannel = BroadcastChannel<Array<out PostMessage>>(extraLargeBroadcastCapacity)
val transactionMessageAddedFlow = transactionMessageAddedChannel.asFlow()
private val transactionMessageRemovedChannel = BroadcastChannel<PostMessage>(extraLargeBroadcastCapacity)
val transactionMessageRemovedFlow = transactionMessageRemovedChannel.asFlow()
private val transactionCompletedChannel = BroadcastChannel<PostId>(extraLargeBroadcastCapacity)
val transactionCompletedFlow = transactionCompletedChannel.asFlow()

val PostTransactionsScope = NewDefaultCoroutineScope()

class PostTransaction(
    private val postsTable: PostsBaseInfoTable = PostsTable,
    private val postsMessagesTable: PostsMessagesInfoTable = PostsMessagesTable
) : Closeable {
    private val messages = ArrayList<PostMessage>()

    var completed: Boolean = false
        private set

    init {
        PostTransactionsScope.launch {
            transactionStartedChannel.send(this@PostTransaction)
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
        val postId = postsTable.allocatePost()
        postsMessagesTable.addMessagesToPost(
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