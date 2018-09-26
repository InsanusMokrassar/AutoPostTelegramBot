package com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.CreationException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NoRowFoundException
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

typealias PostIdMessageId = Pair<Int, Int>

object PostsTable : Table() {
    val postAllocatedChannel = BroadcastChannel<Int>(Channel.CONFLATED)
    val postRemovedChannel = BroadcastChannel<Int>(Channel.CONFLATED)
    val postMessageRegisteredChannel = BroadcastChannel<PostIdMessageId>(Channel.CONFLATED)

    private val id = integer("id").primaryKey().autoIncrement()
    private val postRegisteredMessageId = integer("postRegistered").nullable()
    private val postDateTime = datetime("postDateTime").default(DateTime.now())

    @Throws(CreationException::class)
    fun allocatePost(): Int {
        return transaction {
            insert {
                it[postDateTime] = DateTime.now()
            }[id] ?.also {
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
            PostsMessagesTable.removePostMessages(postId)
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

    fun getPostCreationDateTime(postId: Int): DateTime? {
        return transaction {
            select {
                id.eq(postId)
            }.firstOrNull() ?.get(postDateTime)
        }
    }
}