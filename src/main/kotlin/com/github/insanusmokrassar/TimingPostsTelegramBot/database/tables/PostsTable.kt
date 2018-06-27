package com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.exceptions.CreationException
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object PostsTable : Table() {
    internal val id = integer("id").primaryKey().autoIncrement()

    @Throws(CreationException::class)
    fun allocatePost(): Int {
        return transaction {
            insert {  }[id]
        } ?: throw CreationException("Can't allocate new post")
    }
}