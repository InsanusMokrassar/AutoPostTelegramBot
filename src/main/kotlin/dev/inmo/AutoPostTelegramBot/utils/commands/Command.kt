package dev.inmo.AutoPostTelegramBot.utils.commands

import dev.inmo.AutoPostTelegramBot.checkedMessagesFlow
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.extensions.utils.updates.filterCommandsInsideTextMessages
import dev.inmo.tgbotapi.extensions.utils.updates.onlySentMessageUpdates
import dev.inmo.tgbotapi.types.MessageEntity.textsources.BotCommandTextSource
import dev.inmo.tgbotapi.types.UpdateIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.update.abstracts.BaseMessageUpdate
import dev.inmo.tgbotapi.updateshandlers.UpdateReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

abstract class Command {
    val callback: UpdateReceiver<BaseMessageUpdate>
        get() = this::invoke
    protected abstract val commandRegex: Regex

    init {
        CoroutineScope(Dispatchers.Default).launch {
            checkedMessagesFlow.collectWithErrors {
                invoke(it)
            }
        }
    }

    suspend fun invoke(p1: BaseMessageUpdate) {
        (p1.data as? CommonMessage<*>) ?.let { message ->
            (message.content as? TextContent) ?.also {
                it.textEntities.firstOrNull { textPart ->
                    val source = textPart.source
                    source is BotCommandTextSource && (commandRegex.matches(source.command))
                } ?.also {
                    onCommand(p1.updateId, message)
                } ?: if (commandRegex.matches(it.text)) {
                    onCommand(p1.updateId, message)
                }
            }
        }
    }

    abstract suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>)
}

fun buildCommandFlow(
    commandRegex: Regex
): Flow<CommonMessage<TextContent>> = checkedMessagesFlow.onlySentMessageUpdates().filterCommandsInsideTextMessages(
    commandRegex
).mapNotNull { it as? CommonMessage<TextContent> }
