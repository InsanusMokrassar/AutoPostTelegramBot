package dev.inmo.AutoPostTelegramBot.base.database.tables

import dev.inmo.AutoPostTelegramBot.base.database.exceptions.CreationException
import dev.inmo.AutoPostTelegramBot.base.database.exceptions.NoRowFoundException
import dev.inmo.AutoPostTelegramBot.base.models.PostId
import dev.inmo.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import dev.inmo.tgbotapi.types.MessageIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.jodatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

typealias PostIdMessageId = Pair<PostId, MessageIdentifier>

@Deprecated("Deprecated due to replacement into config")
lateinit var PostsTable: PostsBaseInfoTable
    internal set

@Deprecated("Deprecated due to replacement into config")
val PostsTableScope: CoroutineScope
    get() = PostsTable.coroutinesScope

class PostsBaseInfoTable(
    private val database: Database,
    private val postsMessageTable: PostsMessagesInfoTable
) : Table() {
    internal val coroutinesScope = NewDefaultCoroutineScope()
    val postAllocatedChannel = BroadcastChannel<PostId>(Channel.CONFLATED)
    val postRemovedChannel = BroadcastChannel<PostId>(Channel.CONFLATED)
    val postMessageRegisteredChannel = BroadcastChannel<PostIdMessageId>(Channel.CONFLATED)

    private val idColumn = integer("id").primaryKey().autoIncrement()
    private val postRegisteredMessageIdColumn = long("postRegistered").nullable()
    private val postDateTimeColumn = datetime("postDateTime").default(DateTime.now())

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(this@PostsBaseInfoTable)
        }
    }

    @Throws(CreationException::class)
    fun allocatePost(): PostId {
        return transaction(database) {
            insert {
                it[postDateTimeColumn] = DateTime.now()
            }[idColumn] ?.also {
                coroutinesScope.launch {
                    postAllocatedChannel.send(it)
                }
            }
        } ?: throw CreationException("Can't allocate new post")
    }

    /**
     * @return Old message identifier if available
     */
    fun postRegistered(postId: PostId, messageId: MessageIdentifier): MessageIdentifier? {
        return transaction(database) {
            val previousMessageId = postRegisteredMessage(postId)
            update({ idColumn.eq(postId) }) {
                it[postRegisteredMessageIdColumn] = messageId
            }
            previousMessageId
        }.also {
            coroutinesScope.launch {
                postMessageRegisteredChannel.send(postId to messageId)
            }
        }
    }

    fun postRegisteredMessage(postId: PostId): MessageIdentifier? {
        return transaction(database) {
            select { idColumn.eq(postId) }.firstOrNull() ?.get(postRegisteredMessageIdColumn)
        }
    }

    fun removePost(postId: PostId) {
        transaction(database) {
            postsMessageTable.removePostMessages(postId)
            deleteWhere { idColumn.eq(postId) }
        }.also {
            coroutinesScope.launch {
                postRemovedChannel.send(postId)
            }
        }
    }

    fun getAll(): List<PostId> {
        return transaction(database) {
            selectAll().map { it[idColumn] }
        }
    }

    @Throws(NoRowFoundException::class)
    fun findPost(messageId: MessageIdentifier): PostId {
        return transaction(database) {
            select {
                postRegisteredMessageIdColumn.eq(messageId)
            }.firstOrNull() ?.get(idColumn) ?: throw NoRowFoundException("Can't find row for message $messageId")
        }
    }

    fun getPostCreationDateTime(postId: PostId): DateTime? {
        return transaction(database) {
            select {
                idColumn.eq(postId)
            }.firstOrNull() ?.get(postDateTimeColumn)
        }
    }
}