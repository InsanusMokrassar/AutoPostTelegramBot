package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.BasePlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.commands.*
import com.pengrad.telegrambot.TelegramBot
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.ref.WeakReference

class SchedulerPlugin : Plugin {
    val timerSchedulesTable = PostsSchedulesTable()

    private lateinit var enableTimerCommand: EnableTimerCommand
    private lateinit var getSchedulesCommand: GetSchedulesCommand
    private lateinit var disableTimerCommand: DisableTimerCommand

    private lateinit var scheduler: Scheduler

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(timerSchedulesTable)
        }
    }

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
        (pluginManager.plugins.firstOrNull { it is BasePlugin } as? BasePlugin)?.also {
            timerSchedulesTable.postsUsedTablePluginName = it.postsUsedTable to name
        }

        scheduler = Scheduler(
            timerSchedulesTable,
            pluginManager.plugins.firstOrNull { it is Publisher } as? Publisher ?: return
        )
        val botWR = WeakReference(bot)

        enableTimerCommand = EnableTimerCommand(
            timerSchedulesTable,
            botWR,
            baseConfig.logsChatId
        )
        getSchedulesCommand = GetSchedulesCommand(
            timerSchedulesTable,
            botWR,
            baseConfig.sourceChatId
        )
        disableTimerCommand = DisableTimerCommand(
            timerSchedulesTable,
            botWR
        )
    }
}