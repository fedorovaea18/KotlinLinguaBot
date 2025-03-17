package ru.fedorova.spring

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val telegramBotService = TelegramBotService(botToken)

    val updateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdRegex: Regex = "\"chat\":\\{\"id\":(\\d+)".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates: String = telegramBotService.getUpdates(updateId)
        println(updates)

        val updateIdMatch: MatchResult? = updateIdRegex.find(updates)
        updateId = updateIdMatch?.groups?.get(1)?.value?.toIntOrNull()?.plus(1) ?: continue

        val messageTextMatch: MatchResult? = messageTextRegex.find(updates)
        val text = messageTextMatch?.groups?.get(1)?.value

        val chatIdMatch: MatchResult? = chatIdRegex.find(updates)
        val chatId = chatIdMatch?.groups?.get(1)?.value?.toLongOrNull() ?: continue

        if (text == "Hello") {
            telegramBotService.sendMessage(chatId, text)
        }
    }

}
