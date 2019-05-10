package com.github.insanusmokrassar.AutoPostTelegramBot.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.MutableRatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.disableLikesForPost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.SchedulerPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribeChecking
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.Serializable

import java.lang.ref.WeakReference

@Serializable
class RatingTimerAutoDisablePlugin : Plugin {
    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val ratingPlugin: MutableRatingPlugin = pluginManager.plugins.firstOrNull {
            it is MutableRatingPlugin
        } as? MutableRatingPlugin ?:let {
            commonLogger.warning("Plugin $name was not load for the reason that rating plugin was not found")
            return
        }

        val schedulerPlugin: SchedulerPlugin = (pluginManager.plugins.firstOrNull {
            it is SchedulerPlugin
        } as? SchedulerPlugin) ?:let {
            commonLogger.warning("Plugin $name was not load for the reason that scheduler plugin was not found")
            return
        }

        schedulerPlugin.timerSchedulesTable.postTimeRegisteredChannel.subscribe(
            {
                commonLogger.throwing(
                    name,
                    "register post time scheduler registered",
                    it
                )
                true
            }
        ) {
            disableLikesForPost(
                it.first,
                ratingPlugin
            )
        }

        CoroutineScope(Dispatchers.Default).launch {
            ratingPlugin.allocateRatingAddedFlow().collect {
                schedulerPlugin.timerSchedulesTable.unregisterPost(it.first)
            }
        }
    }
}