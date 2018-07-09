package com.github.insanusmokrassar.TimingPostsTelegramBot.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

class AvailableRates(
    bot: TelegramBot
) : UpdateCallback<Message> {
    private val botWR = WeakReference(bot)

    override fun invoke(updateId: Int, message: Message) {
        val bot = botWR.get() ?: return
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