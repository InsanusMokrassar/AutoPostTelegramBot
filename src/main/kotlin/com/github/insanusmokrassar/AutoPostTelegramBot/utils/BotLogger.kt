package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeAsync
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.splitForMessageWithAdditionalStep
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.experimental.*
import java.lang.ref.WeakReference
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter

private fun TelegramBot.sendLogRecord(record: String, chatId: Long) {
    executeAsync(
        SendMessage(
            chatId,
            record
        ).parseMode(
            ParseMode.Markdown
        )
    )
}

private class LoggerHandler(
    loggerToHandle: Logger,
    bot: TelegramBot,
    private val logsChatId: Long,
    private val logMessagesDelay: Long = 1000L
) : Handler() {
    private val botWR = WeakReference(bot)

    private val defaultFormatter = SimpleFormatter()

    private val logsQueue: Queue<String> = ArrayDeque<String>()

    private var logsSendingJob: Job? = null

    init {
        loggerToHandle.addHandler(this)
    }

    override fun publish(record: LogRecord?) {
        record ?.also {
            formatter.format(it).splitForMessageWithAdditionalStep(6).forEach {
                addLogRecord("```$it```")
            }
        }
    }

    override fun flush() {
        runBlocking {
            logsSendingJob ?.cancelAndJoin()
            while (logsQueue.isNotEmpty()) {
                botWR.get() ?.sendLogRecord() ?: logsQueue.poll()
            }
        }
    }

    override fun close() {
        botWR.clear()
    }

    private fun addLogRecord(record: String) {
        logsQueue.offer(record)
        refreshSendJob()
    }

    private fun refreshSendJob() {
        logsSendingJob ?:also {
            logsSendingJob = launch {
                try {
                    while (isActive && logsQueue.isNotEmpty()) {
                        botWR.get() ?.sendLogRecord() ?: break
                        delay(logMessagesDelay)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    logsSendingJob = null
                }
            }
        }
    }

    private fun TelegramBot.sendLogRecord() = sendLogRecord(logsQueue.poll(), logsChatId)

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
