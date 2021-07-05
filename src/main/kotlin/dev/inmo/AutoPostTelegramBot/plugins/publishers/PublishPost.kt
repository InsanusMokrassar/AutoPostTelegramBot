package dev.inmo.AutoPostTelegramBot.plugins.publishers

import dev.inmo.AutoPostTelegramBot.base.database.exceptions.NoRowFoundException
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsTable
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.Chooser
import dev.inmo.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import dev.inmo.AutoPostTelegramBot.utils.commands.Command
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.requests.DeleteMessage
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.ParseMode.MarkdownParseMode
import dev.inmo.tgbotapi.types.UpdateIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

private val PublishPostScope = NewDefaultCoroutineScope()

class PublishPost(
    chooser: Chooser?,
    publisher: Publisher,
    private val botWR: WeakReference<RequestsExecutor>,
    private val logsChatId: ChatIdentifier,
    private val postsTable: PostsBaseInfoTable = PostsTable
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