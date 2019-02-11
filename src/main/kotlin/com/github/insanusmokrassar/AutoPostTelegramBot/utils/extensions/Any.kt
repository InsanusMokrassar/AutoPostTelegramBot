package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.TelegramBotAPI.bot.exceptions.RequestException
import com.github.insanusmokrassar.TelegramBotAPI.types.buttons.Matrix
import com.github.insanusmokrassar.TelegramBotAPI.utils.matrix
import com.github.insanusmokrassar.TelegramBotAPI.utils.row
import kotlin.math.ceil

inline fun <reified T> List<T>.toTable(columns: Int): Matrix<T> {
    val rows = ceil(size.toFloat() / columns).toInt()
    return matrix {
        for (i in 0 until rows) {
            row {
                try {
                    subList(i * columns, (i + 1) * columns)
                } catch (e: IndexOutOfBoundsException) {
                    subList(i * columns, size)
                }.forEach { add(it) }
            }
        }
    }
}

fun Any.sendToLogger(e: Throwable, sourceMethod: String = "Unknown method") {
    when (e) {
        is RequestException -> {
            commonLogger.throwing(
                this::class.java.simpleName,
                sourceMethod,
                e
            )
            commonLogger.warning(e.response.toString())
        }
        else -> commonLogger.throwing(
            this::class.java.simpleName,
            sourceMethod,
            e
        )
    }
}
