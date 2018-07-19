package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.pengrad.telegrambot.TelegramBot

class ForwardersPlugin : Plugin {
    override val version: PluginVersion = 1L

    val forwarders = listOf(
        AudioForwarder(),
        ContactForwarder(),
        DocumentForwarder(),
        LocationForwarder(),
        MediaGroupForwarder(),
        PhotoForwarder(),
        SimpleForwarder(),
        TextForwarder(),
        VideoForwarder(),
        VoiceForwarder()
    ).sortedDescending()

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) { }
}