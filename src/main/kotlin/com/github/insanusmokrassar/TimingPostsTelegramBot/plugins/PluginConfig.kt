package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.initObject

class PluginConfig(
    val className: String? = null,
    val params: IObject<Any>? = null
) {
    fun newInstance(): Plugin? {
        return className ?.let {
            initObject(it, params)
        }
    }
}
