package dev.inmo.AutoPostTelegramBot.utils

import dev.inmo.tgbotapi.types.MessageIdentifier

fun makeLinkToMessage(
    username: String,
    messageId: MessageIdentifier
): String = "https://telegram.me/$username/$messageId"

fun makePhotoLink(
    botToken: String,
    filePath: String
) = "https://api.telegram.org/file/bot$botToken/$filePath"
