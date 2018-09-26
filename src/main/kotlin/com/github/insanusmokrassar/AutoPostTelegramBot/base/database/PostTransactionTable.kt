package com.github.insanusmokrassar.AutoPostTelegramBot.base.database

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import java.io.Closeable

@Deprecated("Please, use PostTransaction class")
object PostTransactionTable : Closeable {
    val transactionStartedChannel by lazy {
        com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactionStartedChannel
    }
    val transactionMessageAddedChannel by lazy {
        com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactionMessageAddedChannel
    }
    val transactionMessageRemovedChannel by lazy {
        com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactionMessageRemovedChannel
    }
    val transactionCompletedChannel by lazy {
        com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactionCompletedChannel
    }

    private var transaction = PostTransaction()

    var inTransaction: Boolean = false
        private set

    fun startTransaction() {
        if (transaction.completed) {
            transaction = PostTransaction()
        } else {
            throw IllegalStateException("Already in transaction")
        }
    }

    fun addMessageId(vararg message: PostMessage) {
        if (!transaction.completed) {
            transaction.addMessageId(*message)
        } else {
            throw IllegalStateException("Not in transaction")
        }
    }

    fun removeMessageId(message: PostMessage) {
        if (!transaction.completed) {
            transaction.removeMessageId(message)
        } else {
            throw IllegalStateException("Not in transaction")
        }
    }

    @Throws(NothingToSaveException::class)
    fun saveNewPost() {
        if (!transaction.completed) {
            transaction.let {
                transaction = PostTransaction()
                it.saveNewPost()
            }
        } else {
            throw IllegalStateException("Not in transaction")
        }
    }
    override fun close() {
        if (!transaction.completed) {
            transaction.close()
        } else {
            throw IllegalStateException("Not in transaction")
        }
    }
}