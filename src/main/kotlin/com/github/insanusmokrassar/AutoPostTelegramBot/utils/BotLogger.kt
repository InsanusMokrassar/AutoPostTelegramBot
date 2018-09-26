package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.sendBlocking
import java.lang.ref.WeakReference
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter

private fun TelegramBot.sendLogRecord(record: String, chatId: Long) {
    executeSync(
        SendMessage(
            chatId,
            record
        )
    )
}

private fun TelegramBot.sendLogRecordAsync(record: String, chatId: Long) {
    executeAsync(
        SendMessage(
            chatId,
            record
        )
    )
}

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
