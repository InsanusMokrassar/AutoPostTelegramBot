package dev.inmo.AutoPostTelegramBot.utils.extensions

import java.io.ByteArrayOutputStream
import java.io.PrintStream

fun Throwable.collectStackTrace(): String {
    return ByteArrayOutputStream().also {
        printStackTrace(PrintStream(it))
    }.toString(Charsets.UTF_8.toString())
}
