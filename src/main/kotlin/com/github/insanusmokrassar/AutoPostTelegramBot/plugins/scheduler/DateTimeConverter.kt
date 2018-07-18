package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import org.joda.time.DateTime

interface DateTimeConverter {
    val formatPattern: String
    val timeZoneId: String
    fun tryConvert(from: String): DateTime?
}
