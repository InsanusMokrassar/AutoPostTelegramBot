package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.pengrad.telegrambot.TelegramBot

interface Chooser : Plugin {

    /**
     * Must return postIds for posting
     */
    fun triggerChoose(): Collection<Int>

    /**
     * By default - do nothing on init
     */
    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) { }
}