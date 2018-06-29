package com.github.insanusmokrassar.TimingPostsTelegramBot.utils

fun makeLinkToMessage(
    username: String,
    messageId: Int
): String = "https://telegram.me/$username/$messageId"
