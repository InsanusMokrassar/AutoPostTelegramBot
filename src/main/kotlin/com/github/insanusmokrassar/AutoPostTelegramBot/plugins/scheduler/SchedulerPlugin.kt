package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.AutoPostTelegramBot
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.BasePlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.commands.*
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.ref.WeakReference

@Serializable
class SchedulerPlugin : Plugin {
    @Transient
    val timerSchedulesTable = PostsSchedulesTable()

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

    override suspend fun onInit(bot: AutoPostTelegramBot) {
        (bot.pluginManager.plugins.firstOrNull { it is BasePlugin } as? BasePlugin)?.also {
            timerSchedulesTable.postsUsedTablePluginName = it.postsUsedTable to name
        }

        scheduler = Scheduler(
            timerSchedulesTable,
            bot.pluginManager.plugins.firstOrNull { it is Publisher } as? Publisher ?: return
        )
        val executorWR = WeakReference(bot.executor)

        enableTimerCommand = EnableTimerCommand(
            timerSchedulesTable,
            executorWR,
            bot.config.logsChatId
        )
        getSchedulesCommand = GetSchedulesCommand(
            timerSchedulesTable,
            executorWR,
            bot.config.sourceChatId
        )
        disableTimerCommand = DisableTimerCommand(
            timerSchedulesTable,
            executorWR
        )
    }
}