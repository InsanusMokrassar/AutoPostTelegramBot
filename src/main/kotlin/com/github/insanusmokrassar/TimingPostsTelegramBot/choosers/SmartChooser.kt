package com.github.insanusmokrassar.TimingPostsTelegramBot.choosers

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostIdRatingPair
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsLikesTable
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.util.*

private val timeFormat = DateTimeFormat.forPattern("HH:mm")

private fun String.toTime(offset: String = "+00:00"): DateTime {
    return timeFormat.withZone(
        DateTimeZone.forID(offset)
    ).parseDateTime(
        this
    )
}

private fun Long.fromTime(offset: String = "+00:00"): String {
    return timeFormat.withZone(
        DateTimeZone.forID(offset)
    ).print(
        this
    )
}

private fun getZeroHour(offset: String = "+00:00"): Long {
    return timeFormat.withZone(DateTimeZone.forID(offset)).parseDateTime("00:00").millis
}

private fun get24Hour(offset: String = "+00:00"): Long {
    return getZeroHour(offset) + (24 * 60 * 60 * 1000)
}

private const val ascendSort = "ascend"
private const val descendSort = "descend"
private const val randomSort = "random"
private const val defaultSort = descendSort

private val commonRandom = Random()

private typealias InnerChooser = (List<PostIdRatingPair>, Int) -> Collection<Int>

private val commonInnerChoosers = mapOf<String, InnerChooser>(
    ascendSort to {
        pairs, count ->
        pairs.sortedBy { it.second }.let {
            it.subList(
                0,
                if (it.size < count) {
                    it.size
                } else {
                    count
                }
            )
        }.map {
            it.first
        }
    },
    descendSort to {
        pairs, count ->
        pairs.sortedByDescending { it.second }.let {
            it.subList(
                0,
                if (it.size < count) {
                    it.size
                } else {
                    count
                }
            )
        }.map {
            it.first
        }
    },
    randomSort to {
        pairs, count ->
        mutableSetOf<Int>().apply {
            val from = pairs.toMutableSet()
            while (size < count && size < from.size) {
                val chosen = from.elementAt(
                    commonRandom.nextInt(
                        from.size
                    )
                )
                from.remove(chosen)
                add(
                    chosen.first
                )
            }
        }
    }
)

private class SmartChooserConfigItem (
    val minRate: Int? = null,
    val maxRate: Int? = null,
    val timeOffset: String = "+00:00",
    val time: Array<String?> = arrayOf(
        "00:00",
        null
    ),
    val sort: String = defaultSort,
    val count: Int = 1
) {
    private var realTimePairs: List<Pair<Long, Long>>? = null

    private val zeroHour: Long by lazy {
        getZeroHour(timeOffset)
    }

    private val nextDayZeroHour: Long by lazy {
        get24Hour(timeOffset)
    }

    private val timePairs: List<Pair<Long, Long>> by lazy {
        val pairs = mutableListOf<Pair<Long, Long>>()
        var currentPair: Pair<Long?, Long?>? = null
        time.forEach {
            s ->
            currentPair ?.let {
                currentPairNN ->
                val first = currentPairNN.first ?: zeroHour
                val second = s ?. toTime(timeOffset) ?. millis ?: nextDayZeroHour

                if (first > second) {
                    pairs.add(first to nextDayZeroHour)
                    pairs.add(zeroHour to second)
                } else {
                    pairs.add(first to second)
                }

                currentPair = null
            } ?:let {
                currentPair = s ?. toTime(timeOffset) ?. millis to null
            }
        }
        realTimePairs = pairs
        pairs
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
                    if (it.first <= now && now < it.second) {
                        return true
                    }
                }
            }
            return false
        }

    val chooser: InnerChooser?
        get() = commonInnerChoosers[sort]

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Rating: ${minRate ?: "any low"} - ${maxRate ?: "any big"}\n")
        stringBuilder.append("Time:\n")
        timePairs.forEach {
            stringBuilder.append("  ${it.first.fromTime(timeOffset)} - ${it.second.fromTime(timeOffset)}\n")
        }
        return stringBuilder.toString()
    }
}

private class SmartChooserConfig(
    val times: List<SmartChooserConfigItem> = emptyList()
)

class SmartChooser(
    config: IObject<Any>
) : Chooser {
    private val config = config.toObject(SmartChooserConfig::class.java)

    init {
        println("Smart chooser inited: ${this.config.times.joinToString(separator = "\n") { it.toString() }}")
        println("Actual: ${this.config.times.firstOrNull { it.actual } ?.toString() ?: "Nothing"}")
    }

    override fun triggerChoose(): Collection<Int> {
        val actualItem = config.times.firstOrNull { it.actual }
        return actualItem ?.let {
            PostsLikesTable.getRateRange(
                it.minRate,
                it.maxRate
            )
        } ?.let {
            chosenList ->
            actualItem.chooser ?.invoke(
                chosenList,
                actualItem.count
            )
        } ?: emptyList()
    }
}