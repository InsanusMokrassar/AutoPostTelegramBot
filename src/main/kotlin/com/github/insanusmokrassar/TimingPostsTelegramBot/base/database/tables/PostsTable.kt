package com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.exceptions.CreationException
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.exceptions.NoRowFoundException
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

private const val countOfSubscriptions = 256

typealias PostIdMessageId = Pair<Int, Int>

object PostsTable : Table() {
    val postAllocatedChannel = BroadcastChannel<Int>(countOfSubscriptions)
    val postRemovedChannel = BroadcastChannel<Int>(countOfSubscriptions)
    val postMessageRegisteredChannel = BroadcastChannel<PostIdMessageId>(countOfSubscriptions)

    internal val id = integer("id").primaryKey().autoIncrement()
    private val postRegisteredMessageId = integer("postRegistered").nullable()

    @Throws(CreationException::class)
    fun allocatePost(): Int {
        return transaction {
            insert {  }[id] ?.also {
                launch {
                    postAllocatedChannel.send(it)
                }
            }
        } ?: throw CreationException("Can't allocate new post")
    }

    fun postRegistered(postId: Int, messageId: Int): Boolean {
        return transaction {
            postRegisteredMessage(postId) ?.let {
                false
            } ?:let {
                update({ id.eq(postId) }) {
                    it[postRegisteredMessageId] = messageId
                } > 0
            }
        }.also {
            if (it) {
                launch {
                    postMessageRegisteredChannel.send(postId to messageId)
                }
            }
        }
    }

    fun postRegisteredMessage(postId: Int): Int? {
        return transaction {
            select { id.eq(postId) }.firstOrNull() ?.get(postRegisteredMessageId)
        }
    }

    fun removePost(postId: Int) {
        transaction {
            PostsMessagesTable.getMessagesOfPost(postId).forEach {
                PostsMessagesTable.removeMessageOfPost(it.messageId)
            }
            deleteWhere { id.eq(postId) }
            launch {
                postRemovedChannel.send(postId)
            }
        }
    }

    fun getAll(): List<Int> {
        return transaction {
            selectAll().map { it[id] }
        }
    }

    @Throws(NoRowFoundException::class)
    fun findPost(messageId: Int): Int {
        return transaction {
            select {
                postRegisteredMessageId.eq(messageId)
            }.firstOrNull() ?.get(id) ?: throw NoRowFoundException("Can't find row for message $messageId")
        }
    }
}