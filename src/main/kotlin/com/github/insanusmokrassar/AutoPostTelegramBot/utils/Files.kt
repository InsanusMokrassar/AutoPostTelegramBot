package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.io.InputStream

fun <T> load(
    filename: String,
    serializer: KSerializer<T>,
    deserializationMethod: (InputStream) -> T = {
        val data = it.reader().readText()
        Json.parse(
            serializer,
            data
        )
    }
): T {
    return (ClassLoader.getSystemResourceAsStream(filename) ?: FileInputStream(filename)).let(deserializationMethod)
}
