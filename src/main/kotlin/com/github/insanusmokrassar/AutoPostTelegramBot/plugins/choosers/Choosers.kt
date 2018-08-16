package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.initObject
import com.github.insanusmokrassar.IObjectK.interfaces.IObject

val choosers = mapOf(
    "mostRated" to MostRatedChooser::class.java.canonicalName,
    "mostRatedRandom" to MostRatedRandomChooser::class.java.canonicalName,
    "smartChooser" to SmartChooser::class.java.canonicalName,
    "none" to NoneChooser::class.java.canonicalName
)

fun initChooser(chooserName: String, paramsSection: IObject<Any>? = null): Chooser {
    return (choosers[chooserName] ?: chooserName).let {
        initObject(it, paramsSection)
    }
}
