package com.github.insanusmokrassar.TimingPostsTelegramBot.base.models

data class ProxySettings(
    val host: String = "localhost",
    val port: Int = 1080,
    val username: String? = null,
    val password: String? = null
)
