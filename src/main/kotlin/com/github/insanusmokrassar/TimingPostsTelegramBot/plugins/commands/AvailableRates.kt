package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.commands

import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.executeAsync
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage

class AvailableRates : Command() {
    override val version: PluginVersion = 0L
    override val commandRegex: Regex = Regex("^/availableRatings$")

    override fun onCommand(updateId: Int, message: Message) {
        val bot = botWR ?.get() ?: return
        var maxRatingLength = 0
        var maxCountLength = 0
        var commonCount = 0

        val ratingCountMap = mutableMapOf<Int, Int>()
        PostsTable.getAll().map {
            PostsLikesTable.getPostRating(it)
        }.also {
            commonCount = it.size
            maxRatingLength = it.maxBy {
                rating ->
                ratingCountMap[rating] ?.let {
                    num ->
                    ratingCountMap[rating] = num + 1
                } ?:let {
                    ratingCountMap[rating] = 1
                }
                rating.toString().length
            } ?.toString() ?.length ?.let {
                it + 2
            } ?: 0
            maxCountLength = ratingCountMap.maxBy {
                it.value
            } ?.value ?.toString() ?.length ?.let {
                it + 2
            } ?: 0
        }

        val formatString = "`%-${maxRatingLength}s`: `%${maxCountLength}s`"

        ratingCountMap.toList().sortedBy {
            it.first
        }.joinToString(
            "\n",
            "Ratings:\n",
            "\nCount of posts: $commonCount"
        ) {
            formatString.format(
                it.first,
                it.second
            )
        }.let {
            bot.executeAsync(
                SendMessage(
                    message.chat().id(),
                    it
                ).parseMode(
                    ParseMode.Markdown
                )
            )
        }
    }
}