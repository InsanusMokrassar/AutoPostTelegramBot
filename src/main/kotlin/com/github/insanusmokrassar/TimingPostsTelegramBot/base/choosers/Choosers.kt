package com.github.insanusmokrassar.TimingPostsTelegramBot.base.choosers

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.initObject

val choosers = mapOf(
    "mostRated" to MostRatedChooser::class.java.canonicalName,
    "mostRatedRandom" to MostRatedRandomChooser::class.java.canonicalName,
    "smartChooser" to SmartChooser::class.java.canonicalName
)

fun initChooser(chooserName: String, paramsSection: IObject<Any>? = null): Chooser {
    return (choosers[chooserName] ?: chooserName).let {
        initObject(it, paramsSection)
    }
}
