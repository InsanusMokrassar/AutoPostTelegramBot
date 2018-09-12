package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NoRowFoundException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.DeleteMessage
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

class PublishPost(
    chooser: Chooser?,
    publisher: Publisher,
    private val botWR: WeakReference<TelegramBot>,
    private val logsChatId: Long = 0
) : Command() {
    override val commandRegex: Regex = Regex("^/publishPost( \\d+)?$")

    private var publisherWR = WeakReference<Publisher>(null)
    private var chooserWR = WeakReference<Chooser>(null)


    init {
        publisherWR = WeakReference(publisher)
        chooser ?.let {
            chooserWR = WeakReference(it)
        }
    }

    override fun onCommand(updateId: Int, message: Message) {
        val publisher = publisherWR.get() ?: return

        val choosen = mutableListOf<Int>()

        message.replyToMessage() ?.also {
            try {
                choosen.add(
                    PostsTable.findPost(it.messageId())
                )
            } catch (e: NoRowFoundException) {
                botWR.get() ?.executeAsync(
                    SendMessage(
                        message.chat().id(),
                        "Message is not related to any post"
                    ).replyToMessageId(
                        it.messageId()
                    )
                )
            }
        } ?:also {
            try {
                val chooser = chooserWR.get() ?: return
                val splitted = message.text().split(" ")
                val count = if (splitted.size > 1) {
                    splitted[1].toInt()
                } else {
                    1
                }

                while (choosen.size < count) {
                    val toAdd = chooser.triggerChoose().filter {
                        !choosen.contains(it)
                    }.let {
                        val futureSize = choosen.size + it.size
                        val toAdd = if (futureSize > count) {
                            futureSize - count
                        } else {
                            it.size
                        }
                        it.toList().subList(0, toAdd)
                    }
                    if (toAdd.isEmpty()) {
                        break
                    }
                    choosen.addAll(
                        toAdd
                    )
                }
            } catch (e: NumberFormatException) {
                println("Can't extract number of posts")
                return
            }
        }

        botWR.get() ?.let {
            bot ->

            bot.executeAsync(
                SendMessage(
                    logsChatId,
                    "Was chosen to publish: ${choosen.size}. (Repeats of choosing was excluded)"
                ).parseMode(
                    ParseMode.Markdown
                ),
                onResponse = {
                    _,  _ ->
                    choosen.forEach {
                        publisher.publishPost(
                            it
                        )
                    }
                    bot.executeAsync(
                        DeleteMessage(
                            message.chat().id(),
                            message.messageId()
                        )
                    )
                }
            )
        }
    }
}