package com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.CreationException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NoRowFoundException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageIdentifier
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

typealias PostIdMessageId = Pair<PostId, MessageIdentifier>


val PostsTableScope = NewDefaultCoroutineScope()

object PostsTable : Table() {
    val postAllocatedChannel = BroadcastChannel<PostId>(Channel.CONFLATED)
    val postRemovedChannel = BroadcastChannel<PostId>(Channel.CONFLATED)
    val postMessageRegisteredChannel = BroadcastChannel<PostIdMessageId>(Channel.CONFLATED)

    private val id = integer("id").primaryKey().autoIncrement()
    private val postRegisteredMessageId = long("postRegistered").nullable()
    private val postDateTime = datetime("postDateTime").default(DateTime.now())

    @Throws(CreationException::class)
    fun allocatePost(): PostId {
        return transaction {
            insert {
                it[postDateTime] = DateTime.now()
            }[id] ?.also {
                PostsTableScope.launch {
                    postAllocatedChannel.send(it)
                }
            }
        } ?: throw CreationException("Can't allocate new post")
    }

    /**
     * @return Old message identifier if available
     */
    fun postRegistered(postId: PostId, messageId: MessageIdentifier): MessageIdentifier? {
        return transaction {
            val previousMessageId = postRegisteredMessage(postId)
            update({ id.eq(postId) }) {
                it[postRegisteredMessageId] = messageId
            }
            previousMessageId
        }.also {
            PostsTableScope.launch {
                postMessageRegisteredChannel.send(postId to messageId)
            }
        }
    }

    fun postRegisteredMessage(postId: PostId): MessageIdentifier? {
        return transaction {
            select { id.eq(postId) }.firstOrNull() ?.get(postRegisteredMessageId)
        }
    }

    fun removePost(postId: PostId) {
        transaction {
            PostsMessagesTable.removePostMessages(postId)
            deleteWhere { id.eq(postId) }
        }.also {
            PostsTableScope.launch {
                postRemovedChannel.send(postId)
            }
        }
    }

    fun getAll(): List<PostId> {
        return transaction {
            selectAll().map { it[id] }
        }
    }

    @Throws(NoRowFoundException::class)
    fun findPost(messageId: MessageIdentifier): PostId {
        return transaction {
            select {
                postRegisteredMessageId.eq(messageId)
            }.firstOrNull() ?.get(id) ?: throw NoRowFoundException("Can't find row for message $messageId")
        }
    }

    fun getPostCreationDateTime(postId: PostId): DateTime? {
        return transaction {
            select {
                id.eq(postId)
            }.firstOrNull() ?.get(postDateTime)
        }
    }
}