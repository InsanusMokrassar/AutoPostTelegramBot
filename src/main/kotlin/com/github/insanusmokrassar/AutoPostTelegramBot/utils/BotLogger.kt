package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeBlocking
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.splitForMessageWithAdditionalStep
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference
import java.util.logging.*

private class LoggerHandler(
    loggerToHandle: Logger,
    bot: TelegramBot,
    private val logsChatId: Long
) : Handler() {
    private val botWR = WeakReference(bot)

    private val defaultFormatter = SimpleFormatter()

    private val recordsActor = actor<LogRecord> {
        for (msg in channel) {
            val bot = botWR.get() ?: break
            formatter.format(msg).splitForMessageWithAdditionalStep(6).forEach {
                record ->
                bot.executeBlocking(
                    SendMessage(
                        logsChatId,
                        record
                    )
                )
            }
        }
    }

    init {
        loggerToHandle.addHandler(this)
    }

    override fun publish(record: LogRecord?) {
        launch {
            recordsActor.send(record ?: return@launch)
        }
    }

    override fun flush() {}

    override fun close() {
        botWR.clear()
        recordsActor.close()
    }

    override fun getFormatter(): Formatter {
        return super.getFormatter() ?: defaultFormatter
    }
}

@Synchronized
fun initHandler(bot: TelegramBot, logsChatId: Long) {
    commonLogger.handlers.firstOrNull { it is LoggerHandler } ?: run {
        LoggerHandler(commonLogger, bot, logsChatId)
    }
}
