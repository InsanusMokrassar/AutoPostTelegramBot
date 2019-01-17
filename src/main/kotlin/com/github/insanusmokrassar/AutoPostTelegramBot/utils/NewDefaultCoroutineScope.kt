package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

fun NewDefaultCoroutineScope(
    threads: Int = 4
): CoroutineScope = CoroutineScope(
    Executors.newFixedThreadPool(
        threads
    ).asCoroutineDispatcher()
)
