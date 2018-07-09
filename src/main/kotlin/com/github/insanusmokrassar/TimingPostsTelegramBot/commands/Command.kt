package com.github.insanusmokrassar.TimingPostsTelegramBot.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.TimingPostsTelegramBot.messagesListener
import com.pengrad.telegrambot.model.Message
import kotlinx.coroutines.experimental.launch
import java.util.logging.Logger

private val logger = Logger.getLogger(Command::class.java.simpleName)

abstract class Command : UpdateCallback<Message> {
    protected abstract val commandRegex: Regex

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

    override fun invoke(p1: Int, p2: Message) {
        p2.text() ?.let {
            if (commandRegex.matches(it)) {
                onCommand(p1, p2)
            }
        }
    }

    protected abstract fun onCommand(updateId: Int, message: Message)
}