package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginManager
import com.pengrad.telegrambot.TelegramBot

const val HIGH_PRIORITY = Int.MAX_VALUE
const val MIDDLE_PRIORITY = 0
const val LOW_PRIORITY = Int.MIN_VALUE

fun List<Forwarder>.correctSort(): List<Forwarder> = sortedDescending()

interface Forwarder : Comparable<Forwarder> {
    val importance: Int
    fun canForward(message: PostMessage): Boolean
    fun forward(
        bot: TelegramBot,
        targetChatId: Long,
        vararg messages: PostMessage
    ): List<Int>

    override fun compareTo(other: Forwarder): Int {
        return importance.compareTo(other.importance)
    }
}