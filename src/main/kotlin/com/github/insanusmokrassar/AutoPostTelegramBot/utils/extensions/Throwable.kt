package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RegisteredRefresher
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestException
import java.io.ByteArrayOutputStream
import java.io.PrintStream

fun Throwable.collectStackTrace(): String {
    return ByteArrayOutputStream().also {
        printStackTrace(PrintStream(it))
    }.toString(Charsets.UTF_8.toString())
}

fun Throwable.sendToLogger() {
    when (this) {
        is RequestException -> {
            commonLogger.throwing(
                RegisteredRefresher::class.java.simpleName,
                "remove registered post-message link",
                this
            )
            commonLogger.warning(response.toString())
        }
        else -> commonLogger.throwing(
            this::class.java.simpleName,
            "remove registered post-message link",
            this
        )
    }
}
