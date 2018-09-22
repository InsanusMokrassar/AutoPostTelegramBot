package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.initSemaphore
import com.pengrad.telegrambot.TelegramBot

data class RequestsRegenConfig(
    val delay: Long = 1000,
    val regen: Int = 1,
    val max: Int = 30
) {
    fun applyFor(bot: TelegramBot) {
        initSemaphore(bot, max, delay, regen)
    }
}
