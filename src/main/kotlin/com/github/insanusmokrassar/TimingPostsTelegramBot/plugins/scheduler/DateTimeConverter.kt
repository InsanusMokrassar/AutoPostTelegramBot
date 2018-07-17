package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.scheduler

import org.joda.time.DateTime

interface DateTimeConverter {
    val formatPattern: String
    val timeZoneId: String
    fun tryConvert(from: String): DateTime?
}
