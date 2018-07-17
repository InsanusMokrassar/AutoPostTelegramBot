package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.publishers.Publisher
import com.pengrad.telegrambot.TelegramBot
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.ref.WeakReference

class SchedulerPlugin : Plugin {
    override val version: PluginVersion = 0L

    private val timerSchedulesTable = PostsSchedulesTable()

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
            WeakReference(bot)
        )
    }
}