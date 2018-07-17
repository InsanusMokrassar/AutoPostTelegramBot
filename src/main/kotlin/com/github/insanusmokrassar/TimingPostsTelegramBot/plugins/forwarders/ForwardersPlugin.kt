package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.*
import com.pengrad.telegrambot.TelegramBot

class ForwardersPlugin : Plugin {
    override val version: PluginVersion = 0L

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