package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginName
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

typealias PostIdToPluginName = Pair<Int, PluginName>

private val PostsUsedTableScope = NewDefaultCoroutineScope()

@Deprecated("Will be removed for the reason of unnecessarily")
class PostsUsedTable internal constructor() : Table() {
    val registeredLinkChannel = BroadcastChannel<PostIdToPluginName>(Channel.CONFLATED)
    val unregisteredLinkChannel = BroadcastChannel<PostIdToPluginName>(Channel.CONFLATED)

    private val id = integer("id").primaryKey().autoIncrement()

    private val postId = integer("postId")
    private val pluginName = text("pluginName")

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(this@PostsUsedTable)
        }

        PostsTable.postRemovedChannel.subscribe {
            transaction {
                val wasRegistered = getLinks(it)
                if (deleteWhere { postId.eq(it) } > 0) {
                    PostsUsedTableScope.launch {
                        wasRegistered.minus(getLinks(it)).forEach { pluginName ->
                            unregisteredLinkChannel.send(it to pluginName)
                        }
                    }
                }
            }
        }
    }

    private fun PluginName.isLinked(postId: Int): Boolean {
        return transaction {
            select {
                this@PostsUsedTable.postId.eq(postId).and(pluginName.eq(this@isLinked))
            }.count() > 0
        }
    }

    fun registerLink(postId: Int, pluginName: PluginName): Boolean {
        return transaction {
            if (!pluginName.isLinked(postId)) {
                insert {
                    it[this@PostsUsedTable.pluginName] = pluginName
                    it[this@PostsUsedTable.postId] = postId
                }[id] != null
            } else {
                false
            }
        }.apply {
            if (this) {
                PostsUsedTableScope.launch {
                    registeredLinkChannel.send(postId to pluginName)
                }
            }
        }
    }

    fun unregisterLink(postId: Int, pluginName: PluginName): Boolean {
        return transaction {
            if (pluginName.isLinked(postId)) {
                deleteWhere {
                    this@PostsUsedTable.postId.eq(postId).and(this@PostsUsedTable.pluginName.eq(pluginName))
                } > 0
            } else {
                false
            }
        }.apply {
            if (this) {
                PostsUsedTableScope.launch {
                    unregisteredLinkChannel.send(postId to pluginName)
                }
            }
        }
    }

    fun getLinks(postId: Int): List<String> {
        return transaction {
            select {
                this@PostsUsedTable.postId.eq(postId)
            }.map {
                it[pluginName]
            }
        }
    }

    fun getPluginLinks(pluginName: PluginName): List<Int> {
        return transaction {
            select {
                this@PostsUsedTable.pluginName.eq(pluginName)
            }.map {
                it[postId]
            }
        }
    }
}