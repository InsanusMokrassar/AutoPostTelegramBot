package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

data class HttpClientConfig(
    val proxy: ProxySettings? = null,
    val connectTimeout: Long = 0,
    val writeTimeout: Long = 0,
    val readTimeout: Long = 0,
    val debug: Boolean = false
) {
    private val builder: OkHttpClient.Builder by lazy {
        OkHttpClient.Builder().also {
            builder ->
            proxy ?.let {
                _ ->
                builder.proxy(
                    Proxy(
                        Proxy.Type.SOCKS,
                        InetSocketAddress(
                            proxy.host,
                            proxy.port
                        )
                    )
                )
                proxy.password ?.let {
                    password ->
                    builder.proxyAuthenticator {
                        _, response ->
                        response.request().newBuilder().apply {
                            addHeader(
                                "Proxy-Authorization",
                                Credentials.basic(proxy.username ?: "", password)
                            )
                        }.build()
                    }
                }
            }
            builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            builder.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
            builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS)
            if (debug) {
                builder.addInterceptor(
                    HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
                )
            }
        }
    }

    fun createClient(): OkHttpClient {
        return builder.build()
    }
}
