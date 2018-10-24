package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

fun DateTime.withoutTimeZoneOffset(): DateTime {
    return withZone(
        DateTimeZone.UTC
    ).plus(
        zone.toTimeZone().rawOffset.toLong()
    )
}
