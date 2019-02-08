package com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactions

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.mediumBroadcastCapacity
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel

data class TransactionsController(
    private val postsTable: PostsTable,
    private val postsMessagesTable: PostsMessagesTable
) {
    private val postTransactionsScope = NewDefaultCoroutineScope()
    val transactionStartedChannel = BroadcastChannel<Unit>(mediumBroadcastCapacity)
    val transactionMessageAddedChannel = BroadcastChannel<Array<out PostMessage>>(mediumBroadcastCapacity)
    val transactionMessageRemovedChannel = BroadcastChannel<PostMessage>(mediumBroadcastCapacity)
    val transactionCompletedChannel = BroadcastChannel<Int>(mediumBroadcastCapacity)

    fun createTransaction(): PostTransaction = PostTransaction(
        postsTable,
        postsMessagesTable,
        transactionStartedChannel,
        transactionMessageAddedChannel,
        transactionMessageRemovedChannel,
        transactionCompletedChannel,
        postTransactionsScope
    )
}