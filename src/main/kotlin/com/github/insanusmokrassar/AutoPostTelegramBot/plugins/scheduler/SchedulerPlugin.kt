package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.commands.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.SafeLazy
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.lang.ref.WeakReference

@Serializable
class SchedulerPlugin : Plugin {
    @Transient
    private val timerSchedulesTableLazy = SafeLazy<PostsSchedulesTable>(CoroutineScope(Dispatchers.Default))

    @Transient
    private lateinit var enableTimerCommand: EnableTimerCommand
    @Transient
    private lateinit var getSchedulesCommand: GetSchedulesCommand
    @Transient
    private lateinit var disableTimerCommand: DisableTimerCommand

    @Transient
    private lateinit var scheduler: Scheduler

    suspend fun getSchedulesTable() = timerSchedulesTableLazy.get()

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        timerSchedulesTableLazy.set(PostsSchedulesTable(baseConfig.databaseConfig.database))
        val timerSchedulesTable = timerSchedulesTableLazy.get()

        scheduler = Scheduler(
            timerSchedulesTable,
            pluginManager.findFirstPlugin() ?: return
        )
        val executorWR = WeakReference(executor)

        CoroutineScope(Dispatchers.Default).launch {
            baseConfig.postsTable.postRemovedChannel.asFlow().collectWithErrors {
                timerSchedulesTable.unregisterPost(it)
            }
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
            baseConfig.postsTable,
            executorWR
        )
    }
}