package com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.exceptions.CreationException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object PostsTable : Table() {
    internal val id = integer("id").primaryKey().autoIncrement()
    private val postRegisteredMessageId = integer("postRegistered").nullable()

    @Throws(CreationException::class)
    fun allocatePost(): Int {
        return transaction {
            insert {  }[id]
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
        }
    }

    fun postRegisteredMessage(postId: Int): Int? {
        return transaction {
            select { id.eq(postId) }.first()[postRegisteredMessageId]
        }
    }

    fun removePost(postId: Int) {
        transaction {
            PostsMessagesTable.getMessagesOfPost(postId).forEach {
                PostsMessagesTable.removeMessageOfPost(it)
            }
            PostsLikesTable.clearPostMarks(postId)
            deleteWhere { id.eq(postId) }
        }
    }

    fun getAll(): List<Int> {
        return transaction {
            selectAll().map { it[id] }
        }
    }
}