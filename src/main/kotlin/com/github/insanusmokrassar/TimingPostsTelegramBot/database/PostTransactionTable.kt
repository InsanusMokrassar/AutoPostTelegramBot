package com.github.insanusmokrassar.TimingPostsTelegramBot.database

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage

object PostTransactionTable {
    private val messages = ArrayList<PostMessage>()
    var inTransaction: Boolean = false
        private set


    fun startTransaction() {
        if (inTransaction) {
            throw IllegalStateException("Already in transaction")
        }
        messages.clear()
        inTransaction = true
    }

    fun addMessageId(message: PostMessage) {
        if (!inTransaction) {
            throw IllegalStateException("Not in transaction")
        }

        messages.add(message)
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
    }
}