package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostIdRatingPair
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.parseDateTimes
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

private const val ascendSort = "ascend"
private const val descendSort = "descend"
private const val randomSort = "random"
private const val defaultSort = descendSort

private val commonRandom = Random()

private typealias InnerChooser = (List<PostIdRatingPair>, Int) -> Collection<Int>

private val commonInnerChoosers = mapOf<String, InnerChooser>(
    ascendSort to {
        pairs, count ->
        pairs.sortedBy { (_, rating) -> rating }.let {
            it.subList(
                0,
                if (it.size < count) {
                    it.size
                } else {
                    count
                }
            )
        }.map {
            (postId, _) ->
            postId
        }
    },
    descendSort to {
        pairs, count ->
        pairs.sortedByDescending { (_, rating) -> rating }.let {
            it.subList(
                0,
                if (it.size < count) {
                    it.size
                } else {
                    count
                }
            )
        }.map {
            (postId, _) ->
            postId
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
    val time: String? = null,
    val times: List<String>? = null,
    val sort: String = defaultSort,
    val count: Int = 1,
    val minAge: Long? = null,
    val maxAge: Long? = null
) {
    private val minAgeAsDateTime: DateTime? by lazy {
        minAge ?.let {
            DateTime.now().withZone(DateTimeZone.UTC).withMillis(it).withZone(DateTimeZone.getDefault())
        }
    }

    private val maxAgeAsDateTime: DateTime? by lazy {
        maxAge ?.let {
            DateTime.now().withZone(DateTimeZone.UTC).withMillis(it).withZone(DateTimeZone.getDefault())
        }
    }

    private val timePairs: List<CalculatedPeriod> by lazy {
        (times ?.flatMap {
            it.parseDateTimes()
        } ?: time ?.parseDateTimes() ?: emptyList()).asPairs()
    }

    fun isActual(
        now: DateTime = DateTime.now()
    ): Boolean {
        return timePairs.firstOrNull {
            pair ->
            pair.isBetween(now)
        } != null
    }

    val chooser: InnerChooser?
        get() = commonInnerChoosers[sort]

    fun checkPostAge(postId: Int): Boolean {
        val postDateTime: DateTime = PostsTable.getPostCreationDateTime(postId) ?: return false
        val minIsOk = minAgeAsDateTime ?.let {
            minDateTime ->
            postDateTime.plus(minDateTime.millis).isBeforeNow
        } ?: true
        val maxIsOk = maxAgeAsDateTime ?.let {
            minDateTime ->
            postDateTime.plus(minDateTime.millis).isAfterNow
        } ?: true
        return minIsOk && maxIsOk
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Rating: ${minRate ?: "any low"} - ${maxRate ?: "any big"}\n")
        stringBuilder.append("Time:\n")
        timePairs.forEach {
            (from, to) ->
            stringBuilder.append("  ${from.dateTime} - ${to.dateTime}\n")
        }
        return stringBuilder.toString()
    }
}

private class SmartChooserConfig(
    val times: List<SmartChooserConfigItem> = emptyList()
)

class SmartChooser(
    config: IObject<Any>
) : RateChooser() {
    private val config = config.toObject(SmartChooserConfig::class.java)

    init {
        commonLogger.info("Smart chooser inited: ${this.config.times.joinToString(separator = "\n") { it.toString() }}")
    }

    override fun triggerChoose(): Collection<Int> {
        val actualItem = config.times.firstOrNull { it.isActual() }
        return actualItem ?.let {
            postsLikesTable ?.getRateRange(
                it.minRate,
                it.maxRate
            ) ?.filter {
                (postId, _) ->
                actualItem.checkPostAge(postId)
            }
        } ?.let {
            chosenList ->
            actualItem.chooser ?.invoke(
                chosenList,
                actualItem.count
            )
        } ?: emptyList()
    }
}