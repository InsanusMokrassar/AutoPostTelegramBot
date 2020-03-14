package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransaction
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import java.util.concurrent.ConcurrentHashMap

object CommonKnownPostsTransactions {
    private val usersTransactions = ConcurrentHashMap<ChatIdentifier, PostTransaction>()
    private lateinit var postsTable: PostsBaseInfoTable
    private lateinit var postsMessagesTable: PostsMessagesInfoTable

    @Synchronized
    operator fun contains(chatIdentifier: ChatIdentifier): Boolean = usersTransactions[chatIdentifier] ?.let {
        !it.completed
    } ?: false

    @Synchronized
    fun startTransaction(chatIdentifier: ChatIdentifier): PostTransaction? = if (chatIdentifier in this) {
        null
    } else {
        PostTransaction(postsTable, postsMessagesTable).also {
            usersTransactions[chatIdentifier] = it
        }
    }

    @Synchronized
    operator fun get(chatIdentifier: ChatIdentifier): PostTransaction? = usersTransactions[chatIdentifier] ?.let {
        if (it.completed) {
            usersTransactions.remove(chatIdentifier)
            null
        } else {
            it
        }
    }

    @Synchronized
    fun getOrStart(chatIdentifier: ChatIdentifier): PostTransaction? = if (chatIdentifier in this) {
        usersTransactions[chatIdentifier]
    } else {
        startTransaction(chatIdentifier)
    }

    @Synchronized
    internal fun updatePostsAndPostsMessagesTables(
        postsTable: PostsBaseInfoTable,
        postsMessagesTable: PostsMessagesInfoTable
    ) {
        this.postsTable = postsTable
        this.postsMessagesTable = postsMessagesTable
    }
}
