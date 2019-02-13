package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import kotlinx.coroutines.channels.Channel

private const val isConflatedModeFlag = "CONFLATED_MODE_ON"

val isConflatedMode: Boolean by lazy {
    System.getenv(isConflatedModeFlag) == "1"
}

fun chooseCapacity(defaultCapacity: Int): Int = if (isConflatedMode) {
    Channel.CONFLATED
} else {
    defaultCapacity
}
