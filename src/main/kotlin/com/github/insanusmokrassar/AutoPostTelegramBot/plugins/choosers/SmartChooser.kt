package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginVersion
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.pluginLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostIdRatingPair
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.*

private val defaultTimeZone = DateTimeZone.getDefault()
private val timeFormat: DateTimeFormatter = DateTimeFormat.forPattern("HH:mm")

private val coverFormat: DateTimeFormatter = DateTimeFormat.forPattern("HH:mm:ss:SS")

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
    val timeOffset: String = defaultTimeZone.id,
    val time: Array<String?> = arrayOf(
        "00:00",
        null
    ),
    val sort: String = defaultSort,
    val count: Int = 1
) {
    private val zeroHour: DateTime by lazy {
        timeFormat.parseDateTime("00:00")
    }

    private val nextDayZeroHour: DateTime by lazy {
        zeroHour.plusDays(1)
    }

    private val timeZone by lazy {
        DateTimeZone.forID(timeOffset)
    }

    private val timePairs: List<Pair<DateTime, DateTime>> by lazy {
        val pairs = mutableListOf<Pair<DateTime, DateTime>>()
        var currentPair: Pair<DateTime?, DateTime?>? = null

        val timeFormatWithConfigTimeZone = timeFormat.withZone(timeZone)

        time.forEach {
            s ->
            val dateTime = s ?.let {
                timeFormatWithConfigTimeZone.parseDateTime(
                    it
                ).withZone(
                    DateTimeZone.getDefault()
                )
            } ?.let {
                it.withDate(
                    zeroHour.toLocalDate()
                )
            }
            currentPair ?.let {
                currentPairNN ->
                val first = currentPairNN.first ?: zeroHour
                val second = dateTime ?: nextDayZeroHour

                if (first > second) {
                    pairs.add(first to nextDayZeroHour)
                    pairs.add(zeroHour to second)
                } else {
                    pairs.add(first to second)
                }

                currentPair = null
            } ?:let {
                currentPair = dateTime to null
            }
        }
        pairs
    }

    fun actual(
        now: Long = timeFormat.parseMillis(
            timeFormat.print(DateTime.now())
        )
    ): Boolean {
        timePairs.forEach {
            if ((it.first.isBefore(now) || it.first.isEqual(now)) && it.second.isAfter(now)) {
                return true
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
            stringBuilder.append("  ${timeFormat.print(it.first)} - ${timeFormat.print(it.second)}\n")
        }
        return stringBuilder.toString()
    }
}

private fun checkCover(times: List<SmartChooserConfigItem>): List<Pair<Long, Long>> {
    var currentFirst: Long? = null
    val result = mutableListOf<Pair<Long, Long>>()
    (timeFormat.parseMillis("00:00") .. timeFormat.parseDateTime("00:00").plusDays(1).millis).forEach {
        now ->
        currentFirst ?.let {
            first ->
            times.firstOrNull { it.actual(now) } ?.let {
                if (first != now) {
                    result.add(first to now)
                }
                currentFirst = null
            }
        } ?: {
            times.firstOrNull { it.actual(now) } ?: {
                currentFirst = now
            }()
        }()
    }
    return result
}

private class SmartChooserConfig(
    val times: List<SmartChooserConfigItem> = emptyList()
)

class SmartChooser(
    config: IObject<Any>
) : RateChooser() {
    override val version: PluginVersion = 0L
    private val config = config.toObject(SmartChooserConfig::class.java)

    init {
        println("Smart chooser inited: ${this.config.times.joinToString(separator = "\n") { it.toString() }}")

        launch {
            val deferred = async {
                checkCover(this@SmartChooser.config.times)
            }

            deferred.await().also {
                if (it.isNotEmpty()) {
                    it.joinToString("\n", "$name: Uncovered time:\n") {
                        "${coverFormat.print(it.first)} - ${coverFormat.print(it.second)}"
                    }.also {
                        pluginLogger.warning(it)
                    }
                } else {
                    pluginLogger.info("$name: All day covered")
                }
            }
        }
    }

    override fun triggerChoose(): Collection<Int> {
        val actualItem = config.times.firstOrNull { it.actual() }
        return actualItem ?.let {
            postsLikesTable ?.getRateRange(
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