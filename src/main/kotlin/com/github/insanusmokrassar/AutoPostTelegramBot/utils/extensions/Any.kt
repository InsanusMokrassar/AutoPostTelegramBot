package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

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
