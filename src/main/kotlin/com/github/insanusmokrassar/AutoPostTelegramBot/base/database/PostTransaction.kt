package com.github.insanusmokrassar.AutoPostTelegramBot.base.database

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import java.io.Closeable
import java.lang.IllegalStateException

val transactionStartedChannel = BroadcastChannel<Unit>(Channel.CONFLATED)
val transactionMessageAddedChannel = BroadcastChannel<Array<out PostMessage>>(Channel.CONFLATED)
val transactionMessageRemovedChannel = BroadcastChannel<PostMessage>(Channel.CONFLATED)
val transactionCompletedChannel = BroadcastChannel<Int>(Channel.CONFLATED)

class PostTransaction : Closeable {
    private val messages = ArrayList<PostMessage>()

    var completed: Boolean = false
        private set

    init {
        launch {
            transactionStartedChannel.send(Unit)
        }
    }

    fun addMessageId(vararg message: PostMessage) {
        if (completed) {
            throw IllegalStateException("Transaction already completed")
        }
        messages.addAll(message)

        launch {
            transactionMessageAddedChannel.send(message)
        }
    }

    fun removeMessageId(message: PostMessage) {
        if (completed) {
            throw IllegalStateException("Transaction already completed")
        }
        messages.remove(message)

        launch {
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

        launch {
            transactionCompletedChannel.send(postId)
        }
    }

    override fun close() {
        saveNewPost()
    }
}