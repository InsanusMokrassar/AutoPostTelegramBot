package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.commands.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.ref.WeakReference

@Serializable
class SchedulerPlugin : Plugin {
    @Transient
    lateinit var timerSchedulesTable: PostsSchedulesTable
        private set

    @Transient
    private lateinit var enableTimerCommand: EnableTimerCommand
    @Transient
    private lateinit var getSchedulesCommand: GetSchedulesCommand
    @Transient
    private lateinit var disableTimerCommand: DisableTimerCommand

    @Transient
    private lateinit var scheduler: Scheduler

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(timerSchedulesTable)
        }
    }

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        timerSchedulesTable = PostsSchedulesTable(baseConfig.databaseConfig.database)
        scheduler = Scheduler(
            timerSchedulesTable,
            pluginManager.findFirstPlugin() ?: return
        )
        val executorWR = WeakReference(executor)

        baseConfig.postsTable.postRemovedChannel.subscribe {
            timerSchedulesTable.unregisterPost(it)
        }

        enableTimerCommand = EnableTimerCommand(
            timerSchedulesTable,
            baseConfig.postsTable,
            baseConfig.postsMessagesTable,
            executorWR,
            baseConfig.logsChatId
        )
        getSchedulesCommand = GetSchedulesCommand(
            timerSchedulesTable,
            baseConfig.postsMessagesTable,
            executorWR,
            baseConfig.sourceChatId
        )
        disableTimerCommand = DisableTimerCommand(
            timerSchedulesTable,
            executorWR
        )
    }
}