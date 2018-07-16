package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating

import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.CallbackQueryReceivers.CallbackQueryReceiver
import com.github.insanusmokrassar.TimingPostsTelegramBot.realMessagesListener
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.queryAnswer
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

const val disable = "❌"

internal fun makeDisableText() = disable

private const val disableIdentifier = "disableRatings"

fun makeDisableInline(postId: Int): String = "$disableIdentifier: $postId"
fun extractDisableInline(from: String): Int? = try {
    from.toIObject().get<String>(disableIdentifier).toInt()
} catch (e: ReadException) {
    null
}

class DisableReceiver(
    bot: TelegramBot,
    sourceChatId: Long
) : CallbackQueryReceiver(bot) {
    private val awaitApprove = HashMap<Long, Int>()

    init {
        realMessagesListener.broadcastChannel.openSubscription().also {
            launch {
                val botWR = WeakReference(bot)
                while (isActive) {
                    val message = it.receive()

                    val userId = message.second.chat().id()

                    awaitApprove[userId] ?.let {
                        if (extractDisableInline(message.second.text()) == it) {
                            awaitApprove.remove(userId)
                            val bot = botWR.get() ?: return@let
                            clearRatingDataForPostId(
                                it,
                                bot,
                                sourceChatId
                            )

                            bot.executeAsync(
                                SendMessage(
                                    userId,
                                    "Rating was disabled"
                                ).parseMode(
                                    ParseMode.Markdown
                                )
                            )
                        }
                    }
                }
                it.cancel()
            }
        }
    }

    override fun invoke(
        query: CallbackQuery,
        bot: TelegramBot?
    ) {
        bot ?: return
        extractDisableInline(query.data()) ?.let {
            awaitApprove[query.from().id().toLong()] = it
            bot.queryAnswer(
                query.id(),
                "Please, write to me `${makeDisableInline(it)}` if you want to disable ratings for this post",
                true
            )
        }
    }
}