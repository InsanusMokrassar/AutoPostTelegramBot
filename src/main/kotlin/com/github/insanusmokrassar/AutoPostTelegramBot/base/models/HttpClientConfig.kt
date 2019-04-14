package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import io.ktor.client.engine.okhttp.OkHttpConfig
import kotlinx.serialization.*
import okhttp3.Credentials
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

@Serializable
data class HttpClientConfig(
    val proxy: ProxySettings? = null,
    val connectTimeout: Long = 0,
    val writeTimeout: Long = 0,
    val readTimeout: Long = 0,
    val debug: Boolean = false
) {
    @Transient
    val builder: OkHttpConfig.() -> Unit = {
        config {
            proxy ?.let {
                proxy(
                    Proxy(
                        Proxy.Type.SOCKS,
                        InetSocketAddress(
                            proxy.host,
                            proxy.port
                        )
                    )
                )
                proxy.password ?.let { password ->
                    proxyAuthenticator { _, response ->
                        response.request().newBuilder().apply {
                            addHeader(
                                "Proxy-Authorization",
                                Credentials.basic(proxy.username ?: "", password)
                            )
                        }.build()
                    }
                }
            }
            connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
            readTimeout(readTimeout, TimeUnit.MILLISECONDS)
        }
    }
}
