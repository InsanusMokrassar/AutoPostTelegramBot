package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NoRowFoundException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendTextMessage
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
    private val logsChatId: ChatIdentifier,
    private val postsTable: PostsBaseInfoTable
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
                    postsTable.findPost(it.messageId)
                )
            } catch (e: NoRowFoundException) {
                botWR.get() ?.execute(
                    SendTextMessage(
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
                val fromChooser = chooser.triggerChoose(exceptions = choosen)
                if (fromChooser.isEmpty()) {
                    break
                } else {
                    choosen.addAll(fromChooser)
                }
            }
        } catch (e: NumberFormatException) {
            println("Can't extract number of posts")
            return
        }

        botWR.get() ?.let { executor ->
            PublishPostScope.launch {
                executor.execute(
                    SendTextMessage(
                        logsChatId,
                        "Was chosen to publish: ${choosen.size}. (Repeats of choosing was excluded)",
                        parseMode = MarkdownParseMode
                    )
                ).let {
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