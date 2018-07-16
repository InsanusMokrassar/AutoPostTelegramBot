package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.choosers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginManager
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