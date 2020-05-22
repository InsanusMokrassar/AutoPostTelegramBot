package com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.checkedMessagesFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.updates.*
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageEntity.textsources.BotCommandTextSource
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.TextContent
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.BaseMessageUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.BaseSentMessageUpdate
import com.github.insanusmokrassar.TelegramBotAPI.updateshandlers.UpdateReceiver
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
                it.entities.firstOrNull { textPart ->
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
