package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins

import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.publishers.Publisher
import com.github.insanusmokrassar.TimingPostsTelegramBot.triggers.Trigger
import com.pengrad.telegrambot.TelegramBot

interface Plugin {
    fun init(
        chooser: Chooser,
        publisher: Publisher,
        trigger: Trigger,
        bot: TelegramBot
    )
}