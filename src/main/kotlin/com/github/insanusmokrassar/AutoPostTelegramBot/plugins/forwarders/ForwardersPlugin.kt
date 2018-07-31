package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin

class ForwardersPlugin : Plugin {

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
}