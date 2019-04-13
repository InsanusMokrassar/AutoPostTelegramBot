package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import kotlinx.serialization.Serializable

@Serializable
data class ProxySettings(
    val host: String = "localhost",
    val port: Int = 1080,
    val username: String? = null,
    val password: String? = null
)
