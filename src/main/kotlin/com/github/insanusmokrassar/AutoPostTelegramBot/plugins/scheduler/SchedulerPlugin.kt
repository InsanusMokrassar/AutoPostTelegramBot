package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.pengrad.telegrambot.TelegramBot
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.ref.WeakReference

class SchedulerPlugin : Plugin {
    override val version: PluginVersion = 0L

    val timerSchedulesTable = PostsSchedulesTable()

    private lateinit var enableTimerCommand: EnableTimerCommand

    private lateinit var scheduler: Scheduler

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(timerSchedulesTable)
        }
    }

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
        scheduler = Scheduler(
            timerSchedulesTable,
            pluginManager.plugins.firstOrNull { it is Publisher } as? Publisher ?: return
        )
        enableTimerCommand = EnableTimerCommand(
            timerSchedulesTable,
            WeakReference(bot),
            baseConfig.logsChatId
        )
    }
}