package dev.inmo.AutoPostTelegramBot.base.database.tables

import dev.inmo.AutoPostTelegramBot.base.models.PostId
import dev.inmo.AutoPostTelegramBot.base.models.PostMessage
import dev.inmo.AutoPostTelegramBot.mediumBroadcastCapacity
import dev.inmo.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import dev.inmo.tgbotapi.types.MediaGroupIdentifier
import dev.inmo.tgbotapi.types.MessageIdentifier
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

typealias PostIdToMessagesIds = Pair<PostId, Collection<MessageIdentifier>>

@Deprecated("Deprecated due to replacement into config")
lateinit var PostsMessagesTable: PostsMessagesInfoTable
    internal set

class PostsMessagesInfoTable(private val database: Database) : Table() {
    private val coroutinesScope = NewDefaultCoroutineScope()
    private val newMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(mediumBroadcastCapacity)

    private val removedMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(mediumBroadcastCapacity)
    private val removedMessageOfPost = BroadcastChannel<PostIdMessageId>(mediumBroadcastCapacity)

    val newMessagesOfPostFlow = newMessagesOfPost.asFlow()
    val removedMessagesOfPostFlow = removedMessagesOfPost.asFlow()
    val removedMessageOfPostFlow = removedMessageOfPost.asFlow()

    private val messageIdColumn: Column<MessageIdentifier> = long("messageId")
    override val primaryKey: PrimaryKey = PrimaryKey(messageIdColumn)
    private val mediaGroupIdColumn: Column<MediaGroupIdentifier?> = text("mediaGroupId").nullable()
    private val postIdColumn: Column<PostId> = integer("postId")

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(this@PostsMessagesInfoTable)
        }
    }

    fun getMessagesOfPost(postId: PostId): List<PostMessage> {
        return transaction(database) {
            select {
                postIdColumn.eq(postId)
            }.map {
                PostMessage(
                    it[messageIdColumn],
                    it[mediaGroupIdColumn]
                )
            }
        }
    }

    fun findPostByMessageId(messageId: MessageIdentifier): PostId? {
        return transaction(database) {
            select {
                messageIdColumn.eq(messageId)
            }.firstOrNull() ?.get(postIdColumn)
        }
    }

    fun addMessagesToPost(postId: PostId, vararg messages: PostMessage) {
        transaction(database) {
            messages.map {
                message ->
                insert {
                    it[postIdColumn] = postId
                    it[messageIdColumn] = message.messageId
                    it[mediaGroupIdColumn] = message.mediaGroupId
                }
                message.messageId
            }.let {
                coroutinesScope.launch {
                    newMessagesOfPost.send(PostIdToMessagesIds(postId, it))
                }
            }
        }
    }

    fun removePostMessages(postId: PostId) {
        getMessagesOfPost(postId).let {
            transaction(database) {
                it.mapNotNull {
                    postMessage ->
                    if (removePostMessage(postId, postMessage.messageId)) {
                        postMessage.messageId
                    } else {
                        null
                    }
                }
            }
        }.also {
            notifyMessagesRemoved(postId, it)
        }
    }

    fun removePostMessage(postId: PostId, messageId: MessageIdentifier): Boolean {
        return transaction(database) {
            deleteWhere { messageIdColumn.eq(messageId) } > 0
        }.also {
            if (it) {
                notifyMessagesRemoved(postId, listOf(messageId))
            }
        }
    }

    private fun notifyMessagesRemoved(postId: PostId, messagesIds: List<MessageIdentifier>) {
        if (messagesIds.isNotEmpty()) {
            coroutinesScope.launch {
                removedMessagesOfPost.send(postId to messagesIds.toList())
            }
            coroutinesScope.launch {
                messagesIds.forEach { messageId ->
                    removedMessageOfPost.send(PostIdMessageId(postId, messageId))
                }
            }
        }
    }
}