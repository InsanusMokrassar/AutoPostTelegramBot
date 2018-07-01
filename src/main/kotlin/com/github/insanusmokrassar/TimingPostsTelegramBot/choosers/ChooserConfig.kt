package com.github.insanusmokrassar.TimingPostsTelegramBot.choosers

import com.github.insanusmokrassar.IObjectK.interfaces.IObject

data class ChooserConfig(
    val chooserName: String,
    val params: IObject<Any>? = null
)
