package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.messagesListener
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginManager
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference
import java.util.logging.Logger

private val logger = Logger.getLogger(Command::class.java.simpleName)

abstract class Command : UpdateCallback<Message>, Plugin {
    protected abstract val commandRegex: Regex
    protected var botWR: WeakReference<TelegramBot>? = null

    init {
        messagesListener.openSubscription().also {
            launch {
                while (isActive) {
                    val received = it.receive()
                    try {
                        invoke(received.first, received.second)
                    } catch (e: Exception) {
                        logger.throwing(
                            Command::class.java.canonicalName,
                            "Perform message",
                            e
                        )
                    }
                }
            }
        }
    }

    override fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        botWR = WeakReference(bot)
    }

    override fun invoke(p1: Int, p2: Message) {
        p2.text() ?.let {
            if (commandRegex.matches(it)) {
                onCommand(p1, p2)
            }
        }
    }

    abstract fun onCommand(updateId: Int, message: Message)
}