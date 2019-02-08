package com.github.insanusmokrassar.AutoPostTelegramBot.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.AutoPostTelegramBot
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.disableLikesForPost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.SchedulerPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribeChecking
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.serialization.Serializable

import java.lang.ref.WeakReference

@Serializable
class RatingTimerAutoDisablePlugin : Plugin {
    override suspend fun onInit(bot: AutoPostTelegramBot) {
        val ratingPlugin: RatingPlugin = bot.pluginManager.plugins.firstOrNull {
            it is RatingPlugin
        } as? RatingPlugin ?:let {
            commonLogger.warning("Plugin $name was not load for the reason that rating plugin was not found")
            return
        }

        val schedulerPlugin: SchedulerPlugin = (bot.pluginManager.plugins.firstOrNull {
            it is SchedulerPlugin
        } as? SchedulerPlugin) ?:let {
            commonLogger.warning("Plugin $name was not load for the reason that scheduler plugin was not found")
            return
        }

        val botWR = WeakReference(bot.executor)
        val sourceChatId = bot.config.sourceChatId

        schedulerPlugin.timerSchedulesTable.postTimeRegisteredChannel.subscribeChecking(
            {
                commonLogger.throwing(
                    name,
                    "register post time scheduler registered",
                    it
                )
                true
            }
        ) {
            botWR.get() ?.let {
                bot ->

                disableLikesForPost(
                    it.first,
                    bot,
                    sourceChatId,
                    ratingPlugin.postsLikesMessagesTable
                )

                true
            } ?: false
        }

        ratingPlugin.postsLikesMessagesTable.ratingMessageRegisteredChannel.subscribe(
            {
                commonLogger.throwing(
                    name,
                    "register post rating enabled",
                    it
                )
                true
            }
        ) {
            schedulerPlugin.timerSchedulesTable.unregisterPost(it.first)
        }
    }
}