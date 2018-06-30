package com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables

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

    fun completeTransaction(): List<PostMessage> {
        return listOf(*messages.toTypedArray()).also {
            messages.clear()
            inTransaction = false
        }
    }

    fun saveWithPostId(postId: Int = PostsTable.allocatePost()) {
        PostsMessagesTable.addMessagesToPost(
            postId,
            *completeTransaction().toTypedArray()
        )
    }
}