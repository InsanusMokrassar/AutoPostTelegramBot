package com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.initObject

class PluginConfig(
    val classname: String? = null,
    val params: IObject<Any>?
) {
    fun newInstance(): Plugin? {
        return classname ?.let {
            initObject<Plugin>(it, params)
        }
    }
}
