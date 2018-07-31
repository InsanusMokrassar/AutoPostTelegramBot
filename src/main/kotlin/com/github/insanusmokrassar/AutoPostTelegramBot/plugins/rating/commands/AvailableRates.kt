package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

class AvailableRates(
    private val botWR: WeakReference<TelegramBot>,
    private val postsLikesMessagesTable: PostsLikesMessagesTable
) : Command() {
    override val commandRegex: Regex = Regex("^/availableRatings$")

    override fun onCommand(updateId: Int, message: Message) {
        val bot = botWR.get() ?: return
        var maxRatingLength = 0
        var maxCountLength = 0
        var commonCount = 0

        val ratingCountMap = mutableMapOf<Int, Int>()
        postsLikesMessagesTable.getEnabledPostsIdAndRatings().map { it.second }.also {
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