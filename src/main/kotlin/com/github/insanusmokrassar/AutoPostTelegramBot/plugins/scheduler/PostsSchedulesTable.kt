package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginName
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.PostsUsedTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

typealias PostIdPostTime = Pair<Int, DateTime>

private val PostsSchedulesTableScope = NewDefaultCoroutineScope(4)

class PostsSchedulesTable : Table() {
    private val postIdColumn = integer("postId").primaryKey()
    private val postTimeColumn = datetime("postTime")

    val postTimeRegisteredChannel = BroadcastChannel<PostIdPostTime>(Channel.CONFLATED)
    val postTimeChangedChannel = BroadcastChannel<PostIdPostTime>(Channel.CONFLATED)
    val postTimeRemovedChannel = BroadcastChannel<Int>(Channel.CONFLATED)

    init {
        PostsTable.postRemovedChannel.subscribe {
            unregisterPost(it)
        }
    }

    private lateinit var lastEnableSubscription: ReceiveChannel<PostIdPostTime>
    private lateinit var lastDisableSubscription: ReceiveChannel<Int>

    internal var postsUsedTablePluginName: Pair<PostsUsedTable, PluginName>? = null
        set(value) {
            field ?.also {
                lastEnableSubscription.cancel()
                lastDisableSubscription.cancel()
            }
            field = value
            value ?.also {
                registeredPostsTimes().map { (postId, _) ->
                    postId
                }.minus(
                    value.first.getPluginLinks(value.second)
                ).forEach {
                    value.first.registerLink(it, value.second)
                }
                lastEnableSubscription = postTimeRegisteredChannel.subscribe {
                    value.first.registerLink(it.first, value.second)
                }
                lastDisableSubscription = postTimeRemovedChannel.subscribe {
                    value.first.unregisterLink(it, value.second)
                }
            }
        }

    fun postTime(postId: Int): DateTime? {
        return transaction {
            select {
                postIdColumn.eq(postId)
            }.firstOrNull() ?.get(postTimeColumn)
        }
    }

    fun registerPostTime(postId: Int, postTime: DateTime) {
        var updated = false
        var registered = false
        transaction {
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
        transaction {
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
        return transaction { selectAll().map { it[postIdColumn] to it[postTimeColumn] } }
    }

    fun registeredPostsTimes(period: Pair<DateTime, DateTime>): List<PostIdPostTime> {
        return transaction {
            select {
                postTimeColumn.between(period.first, period.second)
            }.map {
                it[postIdColumn] to it[postTimeColumn]
            }
        }
    }

    fun nearPost(): PostIdPostTime? {
        return transaction {
            selectAll().minBy {
                it[postTimeColumn]
            } ?.let {
                it[postIdColumn] to it[postTimeColumn]
            }
        }
    }
}