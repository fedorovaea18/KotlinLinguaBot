package ru.fedorova.spring

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val telegramBotService = TelegramBotService(botToken)

    val updateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdRegex: Regex = "\"chat\":\\{\"id\":(\\d+)".toRegex()
    val dataRegex: Regex = "\"data\":\"(.+?)\"".toRegex()

    val trainer = LearnWordsTrainer()

    while (true) {
        Thread.sleep(2000)
        val updates: String = telegramBotService.getUpdates(updateId)
        println(updates)

        val updateIdMatch: MatchResult? = updateIdRegex.find(updates)
        updateId = updateIdMatch?.groups?.get(1)?.value?.toIntOrNull()?.plus(1) ?: continue

        val messageTextMatch: MatchResult? = messageTextRegex.find(updates)
        val messageText = messageTextMatch?.groups?.get(1)?.value

        val chatIdMatch: MatchResult? = chatIdRegex.find(updates)
        val chatId = chatIdMatch?.groups?.get(1)?.value?.toLongOrNull() ?: continue

        val dataMatch: MatchResult? = dataRegex.find(updates)
        val data = dataMatch?.groups?.get(1)?.value

        if (messageText?.lowercase() == "hello") {
            telegramBotService.sendMessage(chatId, messageText)
        }

        if (messageText?.lowercase() == "/start") {
            telegramBotService.sendMenu(chatId)
        }

        if (data == STATISTICS_CLICKED) {
            val statistics = trainer.getStatistics()
            val statisticsText =
                "Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}%"
            telegramBotService.sendMessage(chatId, statisticsText)
        }

        fun checkNextQuestionAndSend(
            trainer: LearnWordsTrainer,
            telegramBotService: TelegramBotService,
            chatId: Long,
        ) {
            val question = trainer.getNextQuestion()
            if (question == null) {
                telegramBotService.sendMessage(chatId, "Все слова в словаре выучены")
            } else {
                telegramBotService.sendQuestion(chatId, question)
            }

        }

        if (data == LEARN_WORDS_CLICKED) {
            checkNextQuestionAndSend(trainer, telegramBotService, chatId)
        }

        if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
            val userAnswerIndex = data.substring(CALLBACK_DATA_ANSWER_PREFIX.length).toInt()
            val isCorrect = trainer.checkAnswer(userAnswerIndex)
            if (isCorrect) {
                telegramBotService.sendMessage(chatId, "Правильно!")
            } else {
                val rightAnswer = trainer.question?.correctAnswer
                if (rightAnswer !== null) {
                    val wrongAnswerText =
                        "Неправильно! ${rightAnswer.original} - это ${rightAnswer.translation}"
                    telegramBotService.sendMessage(chatId, wrongAnswerText)

                }
            }

            checkNextQuestionAndSend(trainer, telegramBotService, chatId)
        }

    }

}
