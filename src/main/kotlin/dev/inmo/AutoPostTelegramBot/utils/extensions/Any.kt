package dev.inmo.AutoPostTelegramBot.utils.extensions

import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.tgbotapi.bot.exceptions.RequestException
import dev.inmo.tgbotapi.types.buttons.Matrix
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
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
