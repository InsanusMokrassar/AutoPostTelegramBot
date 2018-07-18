package com.github.insanusmokrassar.AutoPostTelegramBot.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.disableLikesForPost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.SchedulerPlugin
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

class RatingTimerAutoDisablePlugin : Plugin {
    override val version: PluginVersion = 0L

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val ratingPlugin: RatingPlugin = pluginManager.plugins.firstOrNull {
            it is RatingPlugin
        } as? RatingPlugin ?:let {
            pluginLogger.warning("Plugin $name was not load for the reason that rating plugin was not found")
            return
        }

        val schedulerPlugin: SchedulerPlugin = (pluginManager.plugins.firstOrNull {
            it is SchedulerPlugin
        } as? SchedulerPlugin) ?:let {
            pluginLogger.warning("Plugin $name was not load for the reason that scheduler plugin was not found")
            return
        }

        val botWR = WeakReference(bot)
        val sourceChatId = baseConfig.sourceChatId

        schedulerPlugin.timerSchedulesTable.postTimeRegisteredChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val registered = it.receive()

                    val bot = botWR.get() ?: break

                    try {
                        disableLikesForPost(
                            registered.first,
                            bot,
                            sourceChatId,
                            ratingPlugin.postsLikesMessagesTable
                        )
                    } catch (e: Exception) {
                        pluginLogger.throwing(
                            name,
                            "register post time scheduler registered",
                            e
                        )
                    }
                }
                it.cancel()
            }
        }

        ratingPlugin.postsLikesMessagesTable.ratingMessageRegisteredChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val registered = it.receive()

                    try {
                        schedulerPlugin.timerSchedulesTable.unregisterPost(registered.first)
                    } catch (e: Exception) {
                        pluginLogger.throwing(
                            name,
                            "register post rating enabled",
                            e
                        )
                    }
                }
                it.cancel()
            }
        }
    }
}