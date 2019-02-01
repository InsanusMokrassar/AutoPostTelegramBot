package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import java.lang.ref.WeakReference

class AvailableRates(
    private val botWR: WeakReference<RequestsExecutor>,
    private val postsLikesMessagesTable: PostsLikesMessagesTable
) : Command() {
    override val commandRegex: Regex = Regex("^/availableRatings$")

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val bot = botWR.get() ?: return
        var maxRatingLength = 0
        var maxCountLength = 0
        val commonCount: Int

        val ratingCountMap = mutableMapOf<Int, Int>()
        postsLikesMessagesTable.getEnabledPostsIdAndRatings().map { (_, rating) -> rating }.also {
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
            bot.execute(
                SendMessage(
                    message.chat.id,
                    it,
                    parseMode = MarkdownParseMode
                )
            )
        }
    }
}