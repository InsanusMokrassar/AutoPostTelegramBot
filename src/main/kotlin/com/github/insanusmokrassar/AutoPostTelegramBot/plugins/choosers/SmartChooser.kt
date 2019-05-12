package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.RatingPair
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.getRatingRange
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.parseDateTimes
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

private const val ascendSort = "ascend"
private const val descendSort = "descend"
private const val randomSort = "random"
private const val defaultSort = descendSort

private val commonRandom = Random()

private typealias InnerChooser = (List<Pair<PostId, RatingPair>>, Int) -> Collection<Pair<PostId, RatingPair>>

private val randomSorter: InnerChooser = { pairs, count ->
    val mutablePairs = pairs.toMutableList()
    val resultList = mutableListOf<Pair<PostId, RatingPair>>()
    while (mutablePairs.isNotEmpty() && resultList.size < count) {
        val chosen = mutablePairs.random()
        resultList.add(chosen)
        mutablePairs.remove(chosen)
    }
    resultList
}

private val commonInnerChoosers = mapOf<String, InnerChooser>(
    ascendSort to { pairs, count ->
        pairs.sortedBy { (_, rating) -> rating.second }.let {
            it.subList(
                0,
                if (it.size < count) {
                    it.size
                } else {
                    count
                }
            )
        }
    },
    descendSort to { pairs, count ->
        pairs.sortedByDescending { (_, rating) -> rating.second }.let {
            it.subList(
                0,
                if (it.size < count) {
                    it.size
                } else {
                    count
                }
            )
        }
    },
    randomSort to randomSorter
)

interface SmartChooserBaseConfigItem {
    val minRate: Int?
    val maxRate: Int?
    val sort: String
    val count: Int
    val minAge: Long?
    val maxAge: Long?
    val otherwise: SmartChooserAdditionalConfigItem?
}

abstract class AbstractSmartChooserBaseConfigItem : SmartChooserBaseConfigItem {
    @Transient
    private val minAgeAsDateTime: DateTime? by lazy {
        minAge ?.let {
            DateTime.now().withZone(DateTimeZone.UTC).withMillis(it).withZone(DateTimeZone.getDefault())
        }
    }

    @Transient
    private val maxAgeAsDateTime: DateTime? by lazy {
        maxAge ?.let {
            DateTime.now().withZone(DateTimeZone.UTC).withMillis(it).withZone(DateTimeZone.getDefault())
        }
    }

    @Transient
    val chooser: InnerChooser
        get() = commonInnerChoosers[sort] ?: randomSorter

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
}

@Serializable
data class SmartChooserAdditionalConfigItem(
    override val minRate: Int? = null,
    override val maxRate: Int? = null,
    override val sort: String = defaultSort,
    override val count: Int = 1,
    override val minAge: Long? = null,
    override val maxAge: Long? = null,
    override val otherwise: SmartChooserAdditionalConfigItem? = null
) : AbstractSmartChooserBaseConfigItem()

@Serializable
data class SmartChooserConfigItem (
    override val minRate: Int? = null,
    override val maxRate: Int? = null,
    val time: String? = null,
    val times: List<String>? = null,
    override val sort: String = defaultSort,
    override val count: Int = 1,
    override val minAge: Long? = null,
    override val maxAge: Long? = null,
    override val otherwise: SmartChooserAdditionalConfigItem? = null
) : AbstractSmartChooserBaseConfigItem() {
    @Transient
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

@Serializable
class SmartChooser(
    val times: List<SmartChooserConfigItem> = emptyList()
) : RateChooser() {
    init {
        commonLogger.info("Smart chooser inited: ${times.joinToString(separator = "\n") { it.toString() }}")
    }

    override suspend fun triggerChoose(time: DateTime, exceptions: List<PostId>): Collection<Int> {
        var actualItem: AbstractSmartChooserBaseConfigItem? = times.firstOrNull { it.isActual(time) }
        while (true) {
            actualItem ?.also { item ->
                ratingPlugin.getRatingRange(
                    item.minRate,
                    item.maxRate
                ).mapNotNull {
                    ratingPlugin.resolvePostId(it.first) ?.let { postId ->
                        if (postId !in exceptions && item.checkPostAge(postId)) {
                            postId to it
                        } else {
                            null
                        }
                    }
                }.distinctBy { (postId, _) ->
                    postId
                }.let { chosenList ->
                    val resultChosen = item.chooser.invoke(chosenList, item.count).map { it.first }
                    if (resultChosen.isNotEmpty()) {
                        return resultChosen
                    }
                }
                actualItem = item.otherwise
            } ?: return emptyList()
        }
    }
}