package com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions

import kotlin.math.ceil

inline fun <reified T> List<T>.toTable(columns: Int): Array<Array<T>> {
    val rows = ceil(size.toFloat() / columns).toInt()
    return (0 until rows).map {
        try {
            subList(it * columns, (it + 1) * columns).toTypedArray()
        } catch (e: IndexOutOfBoundsException) {
            subList(it * columns, size).toTypedArray()
        }
    }.toTypedArray()
}
