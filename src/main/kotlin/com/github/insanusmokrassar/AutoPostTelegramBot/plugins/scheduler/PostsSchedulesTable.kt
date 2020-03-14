package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

typealias PostIdPostTime = Pair<Int, DateTime>

private val PostsSchedulesTableScope = NewDefaultCoroutineScope(4)

class PostsSchedulesTable(
    private val db: Database? = null
) : Table() {
    private val postIdColumn = integer("postId").primaryKey()
    private val postTimeColumn = datetime("postTime")

    private val postTimeRegisteredChannel = BroadcastChannel<PostIdPostTime>(Channel.CONFLATED)
    private val postTimeChangedChannel = BroadcastChannel<PostIdPostTime>(Channel.CONFLATED)
    private val postTimeRemovedChannel = BroadcastChannel<Int>(Channel.CONFLATED)

    val postTimeRegisteredFlow = postTimeRegisteredChannel.asFlow()
    val postTimeChangedFlow = postTimeChangedChannel.asFlow()
    val postTimeRemovedFlow = postTimeRemovedChannel.asFlow()

    init {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(this@PostsSchedulesTable)
        }
    }

    fun postTime(postId: Int): DateTime? {
        return transaction(db) {
            select {
                postIdColumn.eq(postId)
            }.firstOrNull() ?.get(postTimeColumn)
        }
    }

    fun registerPostTime(postId: Int, postTime: DateTime) {
        var updated = false
        var registered = false
        transaction(db) {
            postTime(postId) ?.also {
                update(
                    {
                        postIdColumn.eq(postId)
                    }
                ) {
                    it[postTimeColumn] = postTime
                }
                updated = true
            } ?:also {
                insert {
                    it[postIdColumn] = postId
                    it[postTimeColumn] = postTime
                }
                registered = true
            }
        }
        when {
            updated -> PostsSchedulesTableScope.launch {
                postTimeChangedChannel.send(postId to postTime)
            }
            registered -> PostsSchedulesTableScope.launch {
                postTimeRegisteredChannel.send(postId to postTime)
            }
        }
    }

    fun unregisterPost(postId: Int) {
        transaction(db) {
            deleteWhere {
                postIdColumn.eq(postId)
            } > 0
        }.also {
            if (it) {
                PostsSchedulesTableScope.launch {
                    postTimeRemovedChannel.send(postId)
                }
            }
        }
    }

    fun registeredPostsTimes(): List<PostIdPostTime> {
        return transaction(db) { selectAll().map { it[postIdColumn] to it[postTimeColumn] }.sortedBy { (_, time) -> time.millis } }
    }

    fun registeredPostsTimes(period: Pair<DateTime, DateTime>): List<PostIdPostTime> {
        return transaction(db) {
            select {
                postTimeColumn.between(period.first, period.second)
            }.map {
                it[postIdColumn] to it[postTimeColumn]
            }.sortedBy { (_, time) ->
                time.millis
            }
        }
    }

    fun nearPost(): PostIdPostTime? {
        return transaction(db) {
            registeredPostsTimes().minBy { (_, time) ->
                time.millis
            }
        }
    }
}