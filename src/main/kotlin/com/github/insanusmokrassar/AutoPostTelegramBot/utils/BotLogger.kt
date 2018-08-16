package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeAsync
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.splitForMessageWithAdditionalStep
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference
import java.util.logging.Handler
import java.util.logging.LogRecord

private class LoggerHandler(
    bot: TelegramBot,
    private val logsChatId: Long
) : Handler() {
    private val botWR = WeakReference(bot)

    override fun publish(record: LogRecord?) {
        val formatter = formatter
        botWR.get() ?.also {
            bot ->
            record ?.also {
                formatter.format(it).splitForMessageWithAdditionalStep(6).forEach {
                    bot.executeAsync(
                        SendMessage(
                            logsChatId,
                            it
                        ).parseMode(
                            ParseMode.Markdown
                        )
                    )
                }
            }
        }
    }

    override fun flush() {
    }

    override fun close() {
        botWR.clear()
    }
}

@Synchronized
fun initLogger(bot: TelegramBot, logsChatId: Long) {
    commonLogger.handlers.firstOrNull { it is LoggerHandler } ?: run {
        commonLogger.addHandler(
            LoggerHandler(bot, logsChatId)
        )
    }
}
