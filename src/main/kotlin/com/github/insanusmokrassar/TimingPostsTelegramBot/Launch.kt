package com.github.insanusmokrassar.TimingPostsTelegramBot

import com.github.insanusmokrassar.IObjectKRealisations.*

fun main(args: Array<String>) {
    val config = load(args[0]).toObject(Config::class.java).finalConfig

    println(config.toJson())
}
