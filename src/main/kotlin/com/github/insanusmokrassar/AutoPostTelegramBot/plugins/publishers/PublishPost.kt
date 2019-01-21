package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NoRowFoundException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.TextContent
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

private val PublishPostScope = NewDefaultCoroutineScope()

class PublishPost(
    chooser: Chooser?,
    publisher: Publisher,
    private val botWR: WeakReference<RequestsExecutor>,
    private val logsChatId: ChatIdentifier
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

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val publisher = publisherWR.get() ?: return

        val choosen = mutableListOf<Int>()

        message.replyTo ?.also {
            try {
                choosen.add(
                    PostsTable.findPost(it.messageId)
                )
            } catch (e: NoRowFoundException) {
                botWR.get() ?.execute(
                    SendMessage(
                        message.chat.id,
                        "Message is not related to any post",
                        replyToMessageId = it.messageId
                    )
                )
            }
        } ?: try {
            val chooser = chooserWR.get() ?: return
            val count = (message.content as? TextContent) ?.text ?.split(" ") ?.let {
                if (it.size > 1) {
                    it[1].toIntOrNull()
                } else {
                    null
                }
            } ?: 1

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

        botWR.get() ?.let { executor ->
            PublishPostScope.launch {
                executor.execute(
                    SendMessage(
                        logsChatId,
                        "Was chosen to publish: ${choosen.size}. (Repeats of choosing was excluded)",
                        parseMode = MarkdownParseMode
                    )
                ).asMessage.let {
                    choosen.forEach {
                        publisher.publishPost(
                            it
                        )
                    }
                    executor.execute(
                        DeleteMessage(
                            message.chat.id,
                            message.messageId
                        )
                    )
                }
            }
        }
    }
}