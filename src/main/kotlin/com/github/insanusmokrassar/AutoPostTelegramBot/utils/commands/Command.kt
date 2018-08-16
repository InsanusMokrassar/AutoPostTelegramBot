package com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.messagesListener
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.pengrad.telegrambot.model.Message
import java.util.logging.Logger

private val logger = Logger.getLogger(Command::class.java.simpleName)

abstract class Command : UpdateCallback<Message> {
    protected abstract val commandRegex: Regex

    init {
        messagesListener.subscribe {
            invoke(it.first, it.second)
        }
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