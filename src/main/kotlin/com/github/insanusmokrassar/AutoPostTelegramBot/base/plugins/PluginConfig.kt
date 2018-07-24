package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.initObject

class PluginConfig(
    val classname: String? = null,
    val params: IObject<Any>?
) {
    fun newInstance(vararg additionalClassLoaders: ClassLoader): Plugin? {
        return classname ?.let {
            initObject<Plugin>(
                it,
                params,
                this::class.java.classLoader,
                *additionalClassLoaders
            )
        }
    }
}
