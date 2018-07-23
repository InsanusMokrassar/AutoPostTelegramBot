package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

private const val broadcastsCount = 256
typealias PostIdPostTime = Pair<Int, DateTime>

class PostsSchedulesTable : Table() {
    private val postId = integer("postId").primaryKey()
    private val postTime = datetime("postTime")

    val postTimeRegisteredChannel = BroadcastChannel<PostIdPostTime>(broadcastsCount)
    val postTimeChangedChannel = BroadcastChannel<PostIdPostTime>(broadcastsCount)
    val postTimeRemovedChannel = BroadcastChannel<Int>(broadcastsCount)

    init {
        PostsTable.postRemovedChannel.subscribe {
            unregisterPost(it)
        }
    }

    fun postTime(postId: Int): DateTime? {
        return transaction {
            select {
                this@PostsSchedulesTable.postId.eq(postId)
            }.firstOrNull() ?.get(postTime)
        }
    }

    fun registerPostTime(postId: Int, postTime: DateTime) {
        transaction {
            postTime(postId) ?.also {
                update(
                    {
                        this@PostsSchedulesTable.postId.eq(postId)
                    }
                ) {
                    it[this@PostsSchedulesTable.postTime] = postTime
                }
                launch {
                    postTimeChangedChannel.send(postId to postTime)
                }
            } ?:also {
                insert {
                    it[this@PostsSchedulesTable.postId] = postId
                    it[this@PostsSchedulesTable.postTime] = postTime
                }
                launch {
                    postTimeRegisteredChannel.send(postId to postTime)
                }
            }
        }
    }

    fun unregisterPost(postId: Int) {
        transaction {
            deleteWhere {
                this@PostsSchedulesTable.postId.eq(postId)
            } > 0
        }.also {
            if (it) {
                launch {
                    postTimeRemovedChannel.send(postId)
                }
            }
        }
    }

    fun nearPost(): PostIdPostTime? {
        return transaction {
            selectAll().sortedBy {
                it[postTime]
            }.firstOrNull() ?.let {
                it[postId] to it[postTime]
            }
        }
    }
}