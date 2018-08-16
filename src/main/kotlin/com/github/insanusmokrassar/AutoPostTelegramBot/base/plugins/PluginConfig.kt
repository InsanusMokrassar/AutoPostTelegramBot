package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.initObject
import com.github.insanusmokrassar.IObjectK.interfaces.IObject

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
