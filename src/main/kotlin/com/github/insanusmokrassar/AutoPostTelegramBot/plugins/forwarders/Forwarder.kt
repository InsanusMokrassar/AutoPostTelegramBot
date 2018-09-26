package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import java.io.IOException

const val HIGH_PRIORITY = Int.MAX_VALUE
const val MIDDLE_PRIORITY = 0
const val LOW_PRIORITY = Int.MIN_VALUE

interface Forwarder : Comparable<Forwarder> {
    val importance: Int
    fun canForward(message: PostMessage): Boolean

    @Throws(IOException::class)
    suspend fun forward(
        bot: TelegramBot,
        targetChatId: Long,
        vararg messages: PostMessage
    ): Map<PostMessage, Message>

    override fun compareTo(other: Forwarder): Int {
        return importance.compareTo(other.importance)
    }
}