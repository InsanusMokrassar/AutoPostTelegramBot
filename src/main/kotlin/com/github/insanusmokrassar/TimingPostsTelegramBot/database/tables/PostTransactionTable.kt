package com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables

object PostTransactionTable {
    private val messageIds = ArrayList<Int>()
    private var mediaGroupId: String? = null
    var inTransaction: Boolean = false
        private set


    fun startTransaction(mediaGroupId: String?) {
        if (inTransaction) {
            throw IllegalStateException("Already in transaction")
        }
        this.mediaGroupId = mediaGroupId
        messageIds.clear()
        inTransaction = true
    }

    fun addMessageId(messageId: Int, mediaGroupId: String?) {
        if (!inTransaction) {
            throw IllegalStateException("Not in transaction")
        }
        if (mediaGroupId != this.mediaGroupId) {
            throw IllegalArgumentException("Currently in transaction with other media group id")
        }

        messageIds.add(messageId)
    }

    fun completeTransaction(): List<Int> {
        return listOf(*messageIds.toTypedArray()).also {
            messageIds.clear()
            mediaGroupId = null
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