package dev.inmo.AutoPostTelegramBot.plugins.base.commands

import dev.inmo.AutoPostTelegramBot.base.database.PostTransaction
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.tgbotapi.types.ChatIdentifier
import java.util.concurrent.ConcurrentHashMap

class PostsTransactions(
    private val postsTable: PostsBaseInfoTable,
    private val postsMessagesTable: PostsMessagesInfoTable
) {
    private val usersTransactions = ConcurrentHashMap<ChatIdentifier, PostTransaction>()

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
    fun doInTransaction(
        chatId: ChatIdentifier,
        completeIfNewOne: Boolean = true,
        block: (PostTransaction) -> Unit
    ) = get(chatId) ?.let(block) ?: startTransaction(chatId) ?.let {
        if (completeIfNewOne) {
            it.use(block)
        } else {
            it.let(block)
        }
    }
}

lateinit var CommonKnownPostsTransactions: PostsTransactions
    internal set
