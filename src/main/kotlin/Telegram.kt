package ru.fedorova.spring

import kotlinx.serialization.json.Json

fun main(args: Array<String>) {

    val botToken = args[0]
    var lastUpdateId = 0L
    val telegramBotService = TelegramBotService(botToken)

    val json = Json {
        ignoreUnknownKeys = true
    }

    val trainer = LearnWordsTrainer()

    while (true) {
        Thread.sleep(2000)
        val responseString: String = telegramBotService.getUpdates(lastUpdateId)
        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        val updates = response.result
        val firstUpdate = updates.firstOrNull() ?: continue
        val updateId = firstUpdate.updateId
        lastUpdateId = updateId + 1

        val message = firstUpdate.message?.text
        val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackquery?.message?.chat?.id
        val data = firstUpdate.callbackquery?.data

        if (message?.lowercase() == "hello" && chatId != null) {
            telegramBotService.sendMessage(json, chatId, message)
        }

        if (message?.lowercase() == "/start" && chatId != null) {
            telegramBotService.sendMenu(json, chatId)
        }

        if (data == STATISTICS_CLICKED && chatId != null) {
            val statistics = trainer.getStatistics()
            val statisticsText =
                "Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}%"
            telegramBotService.sendMessage(json, chatId, statisticsText)
        }

        fun checkNextQuestionAndSend(
            json: Json,
            trainer: LearnWordsTrainer,
            telegramBotService: TelegramBotService,
            chatId: Long?,
        ) {
            val question = trainer.getNextQuestion()
            if (question == null) {
                telegramBotService.sendMessage(json, chatId, "Все слова в словаре выучены")
            } else {
                telegramBotService.sendQuestion(json, chatId, question)
            }
        }

        if (data == LEARN_WORDS_CLICKED) {
            checkNextQuestionAndSend(json, trainer, telegramBotService, chatId)
        }

        if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
            val userAnswerIndex = data.substring(CALLBACK_DATA_ANSWER_PREFIX.length).toInt()
            val isCorrect = trainer.checkAnswer(userAnswerIndex)
            if (isCorrect) {
                telegramBotService.sendMessage(json, chatId, "Правильно!")
            } else {
                val rightAnswer = trainer.question?.correctAnswer
                if (rightAnswer !== null) {
                    val wrongAnswerText = "Неправильно! ${rightAnswer.original} - это ${rightAnswer.translation}"
                    telegramBotService.sendMessage(json, chatId, wrongAnswerText)
                }
            }
            checkNextQuestionAndSend(json, trainer, telegramBotService, chatId)
        }

    }

}
