package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginName
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.PostsUsedTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

typealias PostIdPostTime = Pair<Int, DateTime>

class PostsSchedulesTable : Table() {
    private val postId = integer("postId").primaryKey()
    private val postTime = datetime("postTime")

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
                registeredPostsTimes().map {
                    (postId, _) ->
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

    fun registeredPostsTimes(): List<PostIdPostTime> {
        return transaction { selectAll().map { it[postId] to it[postTime] } }
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