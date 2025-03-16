package ru.fedorova.spring

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val telegramBotService = TelegramBotService(botToken)

    while (true) {
        Thread.sleep(2000)
        val updates: String = telegramBotService.getUpdates(updateId)
        println(updates)

        val updateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
        val updateIdMatch: MatchResult? = updateIdRegex.find(updates)
        updateId = updateIdMatch?.groups?.get(1)?.value?.toInt()?.plus(1) ?: 0

        val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
        val messageTextMatch: MatchResult? = messageTextRegex.find(updates)
        val text = messageTextMatch?.groups?.get(1)?.value

        val chatIdRegex: Regex = "\"chat\":\\{\"id\":(\\d+)".toRegex()
        val chatIdMatch: MatchResult? = chatIdRegex.find(updates)
        val chatId = chatIdMatch?.groups?.get(1)?.value?.toInt()

        if (text != null && chatId != null && text == "Hello") {
            telegramBotService.sendMessage(chatId, text)
        }
    }

}
