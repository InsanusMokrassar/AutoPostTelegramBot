package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.publishers.Publisher
import com.pengrad.telegrambot.TelegramBot

interface Plugin {
    fun init(
        baseConfig: FinalConfig,
        chooser: Chooser,
        publisher: Publisher,
        bot: TelegramBot
    )
}