package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.triggers

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectK.realisations.SimpleIObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.publishers.Publisher
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.*

private data class TimerStrategyConfig(
    val delay: Long = 60 * 60 *1000
)

class TimerTriggerStrategy (
    config: IObject<Any> = SimpleIObject()
) : Plugin {
    private val config = config.toObject(TimerStrategyConfig::class.java)

    override fun init(
        baseConfig: FinalConfig,
        chooser: Chooser,
        publisher: Publisher,
        bot: TelegramBot
    ) {
        launch {
            while (isActive) {
                synchronized(PostsTable) {
                    synchronized(PostsMessagesTable) {
                        synchronized(PostsLikesTable) {
                            try {
                                chooser.triggerChoose().forEach {
                                    publisher.publishPost(it)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                delay(config.delay)
            }
        }
    }
}
