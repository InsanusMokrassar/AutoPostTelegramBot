package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.splitForMessageWithAdditionalStep
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.bot.exceptions.RequestException
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendTextMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.lang.ref.WeakReference
import java.util.logging.*

private class LoggerHandler(
    loggerToHandle: Logger,
    executor: RequestsExecutor,
    private val logsChatId: ChatIdentifier
) : Handler() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val botWR = WeakReference(executor)

    private val logsChannel = Channel<LogRecord>(Channel.UNLIMITED)
    private val recordsSendingJob = scope.launch {
        for (msg in logsChannel) {
            val bot = botWR.get() ?: break
            formatter.format(msg).splitForMessageWithAdditionalStep(6).forEach { record ->
                try {
                    bot.execute(
                        SendTextMessage(
                            logsChatId,
                            record,
                            MarkdownParseMode
                        )
                    )
                } catch (e: RequestException) {
                    bot.execute(
                        SendTextMessage(
                            logsChatId,
                            record
                        )
                    )
                }
            }
        }
    }.apply {
        start()
    }

    init {
        loggerToHandle.addHandler(this)

        formatter = SimpleFormatter()
    }

    override fun publish(record: LogRecord?) {
        record ?.let {
            scope.launch {
                logsChannel.send(record)
            }
        }
    }

    override fun flush() {}

    override fun close() {
        botWR.clear()
        logsChannel.cancel()
        recordsSendingJob.cancel()
    }
}

@Synchronized
fun initHandler(executor: RequestsExecutor, logsChatId: ChatIdentifier) {
    commonLogger.handlers.firstOrNull { it is LoggerHandler } ?: run {
        LoggerHandler(commonLogger, executor, logsChatId)
    }
}
