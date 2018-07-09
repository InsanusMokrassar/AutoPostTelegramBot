package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.TimingPostsTelegramBot.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.messagesListener
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.publishers.Publisher
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
        messagesListener.broadcastChannel.openSubscription().also {
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

    override fun init(
        baseConfig: FinalConfig,
        chooser: Chooser,
        publisher: Publisher,
        bot: TelegramBot
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

    protected abstract fun onCommand(updateId: Int, message: Message)
}