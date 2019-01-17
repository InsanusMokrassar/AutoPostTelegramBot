package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class ProxySettings(
    @Optional
    val host: String = "localhost",
    @Optional
    val port: Int = 1080,
    @Optional
    val username: String? = null,
    @Optional
    val password: String? = null
)
