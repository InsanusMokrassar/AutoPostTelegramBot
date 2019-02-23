package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

fun String.splitByStep(step: Int): List<String> {
    return (0 until length step step).map {
        val currentLength = (it + step).let {
            if (it > length) {
                length
            } else {
                it
            }
        }
        substring(it until currentLength)
    }
}

fun String.toMarkdown(): String {
    return replace(
        "*",
        "\\*"
    ).replace(
        "_",
        "\\_"
    )
}

private const val maxMessageSymbols = 4095

fun String.splitForMessage(): List<String> = splitByStep(maxMessageSymbols)
fun String.splitForMessageWithAdditionalStep(addStep: Int): List<String> = splitByStep(maxMessageSymbols - addStep)
