package com.github.insanusmokrassar.TimingPostsTelegramBot.base.database

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import org.h2.Driver
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

private const val tabsLevel: String = "    "

private class Record {
    var startTime: Long? = null
        private set
    var endTime: Long? = null
        private set

    var initMessage: String? = null
        private set
    var completeMessage: String? = null
        private set

    val recordTime: Long?
        get() = endTime ?.minus(startTime ?: 0)

    private val subRecords: MutableList<Record> = ArrayList()

    fun start(message: String) {
        startTime ?.let {
            throw IllegalStateException()
        }
        startTime = System.currentTimeMillis()
        initMessage = message
    }

    fun stop(message: String) {
        endTime ?.let {
            throw IllegalStateException()
        }
        endTime = System.currentTimeMillis()
        completeMessage = message
    }

    fun addSubrecord(subrecord: Record) {
        subRecords.add(subrecord)
    }

    fun getSubrecords(): List<Record> {
        return subRecords
    }

    fun prepareResults(tabs: String = ""): String {
        val builder = StringBuilder()
        builder.append("$tabs$initMessage: $startTime\n")
        val nextLevelTab = tabs + tabsLevel
        subRecords.joinToString(separator = "\n\n") {
            it.prepareResults(nextLevelTab)
        }.let {
            if (it.isNotEmpty()) {
                builder.append(it).append("\n")
            }
        }
        var subrecordsAverage: Long? = null
        subRecords.forEach {
            subrecordsAverage = try {
                (subrecordsAverage!! + it.recordTime!!) / 2
            } catch (e: Exception) {
                it.recordTime
            }
        }
        subrecordsAverage ?.let {
            builder.append("${tabs}Subrecords average time: $it ms\n")
        }
        builder.append("$tabs$completeMessage: $endTime\n")
        builder.append("${tabs}Result time: $recordTime ms")
        return builder.toString()
    }
}

private fun initDb() {
    Database.connect(
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        Driver::class.java.canonicalName,
        "sa",
        ""
    )

    transaction {
        SchemaUtils.createMissingTablesAndColumns(PostsLikesTable, PostsMessagesTable, PostsTable)
    }
}

private const val posts = 10
private const val messagesPerPost = 1000
private const val likesPerPost = 10
private const val dislikesPerPost = 12

fun main(args: Array<String>) {
    initDb()

    val records = listOf(
        testCreatingPosts(),
        testReadingPosts(),
        testCreatingMessages(),
        testCreatingMessagesByOneCalling()
    )

    records.joinToString("\n\n") {
        it.prepareResults()
    }.let {
        println(it)
    }
}

private fun testCreatingPosts(): Record {

    val record = Record()

    record.start("Start allocate posts")

    var i = 0
    while (i < posts) {
        PostsTable.allocatePost()
        i++
    }

    record.stop("Posts creating completed. Created: $i")

    return record
}

private fun testReadingPosts(): Record {
    val record = Record()

    record.start("Start read posts")

    val read = transaction {
        PostsTable.selectAll().map {
            it[PostsTable.id]
        }.size
    }

    record.stop("Posts reading completed. Read: $read")

    return record
}

private fun testCreatingMessages(): Record {
    val record = Record()

    val postsIds = transaction {
        PostsTable.selectAll().map {
            it[PostsTable.id]
        }
    }

    var id = 0

    record.start("Start insert of messages")

    postsIds.forEach {
        val subRecord = Record()

        subRecord.start("Start to add")

        var j = 0
        while (j < messagesPerPost) {
            PostsMessagesTable.addMessagesToPost(it, PostMessage(id))
            id++
            j++
        }

        subRecord.stop("Post message added")

        record.addSubrecord(subRecord)
    }

    record.stop("Messages inserted")
    return record
}

private fun testCreatingMessagesByOneCalling(): Record {
    val record = Record()

    val postsIds = transaction {
        PostsTable.selectAll().map {
            it[PostsTable.id]
        }
    }

    var id = transaction {
        PostsMessagesTable.selectAll().count()
    }

    record.start("Start insert of messages")

    postsIds.forEach {
        val subRecord = Record()

        subRecord.start("Start to add")

        PostsMessagesTable.addMessagesToPost(it, *(id until (id + messagesPerPost)).map { PostMessage(it) }.toTypedArray())
        id += messagesPerPost + 1

        subRecord.stop("Post message added")

        record.addSubrecord(subRecord)
    }

    record.stop("Messages inserted")
    return record
}
