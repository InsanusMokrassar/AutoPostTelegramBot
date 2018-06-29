package com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables

object PostTransactionTable {
    private val messageIds = ArrayList<Int>()
    var inTransaction: Boolean = false
        private set


    fun startTransaction() {
        if (inTransaction) {
            throw IllegalStateException("Already in transaction")
        }
        messageIds.clear()
        inTransaction = true
    }

    fun addMessageId(messageId: Int) {
        if (!inTransaction) {
            throw IllegalStateException("Not in transaction")
        }

        messageIds.add(messageId)
    }

    fun completeTransaction(): List<Int> {
        return listOf(*messageIds.toTypedArray()).also {
            messageIds.clear()
            inTransaction = false
        }
    }

    fun saveWithPostId(postId: Int = PostsTable.allocatePost()) {
        PostsMessagesTable.addMessagesToPost(
            postId,
            *completeTransaction().toIntArray()
        )
    }
}