package com.github.insanusmokrassar.TimingPostsTelegramBot.choosers

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsLikesTable
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

private val timeFormat = DateTimeFormat.forPattern("HH:mm")

private class SmartChooserConfigItem (
    val minRate: Int? = null,
    val maxRate: Int? = null,
    val timeOffset: String = "+00:00",
    val time: Array<String?> = arrayOf(
        null,
        null
    )
) {
    private var realTimePairs: List<Pair<Long?, Long?>>? = null

    private val timePairs: List<Pair<Long?, Long?>>
        get() {
            return realTimePairs ?:let {
                val pairs = mutableListOf<Pair<Long?, Long?>>()
                var lastFirstTime: String? = null
                time.forEachIndexed {
                    index, s ->
                    lastFirstTime = if (index % 2 == 0) {
                        s
                    } else {
                        pairs.add(
                            Pair(
                                lastFirstTime ?.let {
                                    timeFormat.parseDateTime(
                                        it
                                    ).withZone(
                                        DateTimeZone.forID(timeOffset)
                                    ).millis
                                },
                                s ?.let {
                                    timeFormat.parseDateTime(
                                        it
                                    ).withZone(
                                        DateTimeZone.forID(timeOffset)
                                    ).millis
                                }
                            )
                        )
                        null
                    }
                }
                realTimePairs = pairs
                pairs
            }
        }

    val actual: Boolean
        get() {
            DateTime.now().let {
                timeFormat.print(it.millis)
            }.let {
                timeFormat.parseDateTime(it).millis
            }.let {
                now ->
                timePairs.forEach {
                    if (it.first ?.let { it <= now } != false && it.second ?.let { it > now } != false) {
                        return true
                    }
                }
            }
            return false
        }
}

private class SmartChooserConfig(
    val times: List<SmartChooserConfigItem> = emptyList(),
    val mostRatedIfNoActual: Boolean = false
)

class SmartChooser(
    config: IObject<Any>
) : Chooser {

    private val config = config.toObject(SmartChooserConfig::class.java)

    override fun triggerChoose(): Collection<Int> {
        return config.times.firstOrNull { it.actual } ?.let {
            PostsLikesTable.getRateRange(
                it.minRate,
                it.maxRate
            )
        } ?: if (config.mostRatedIfNoActual) {
            PostsLikesTable.getMostRated().firstOrNull() ?.let {
                listOf(it)
            }
        } else {
            null
        } ?: emptyList()
    }
}