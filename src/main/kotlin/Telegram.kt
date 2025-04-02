package ru.fedorova.spring

import kotlinx.serialization.json.Json

fun main(args: Array<String>) {

    val botToken = args[0]
    var lastUpdateId = 0L
    val telegramBotService = TelegramBotService(botToken)

    val json = Json {
        ignoreUnknownKeys = true
    }

    val trainers = HashMap<Long, LearnWordsTrainer>()

    while (true) {
        Thread.sleep(2000)
        val responseString: String = telegramBotService.getUpdates(lastUpdateId)
        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdate(it, json, telegramBotService, trainers) }
        lastUpdateId = sortedUpdates.last().updateId + 1
    }
}

fun handleUpdate(
    update: Update,
    json: Json,
    telegramBotService: TelegramBotService,
    trainers: HashMap<Long, LearnWordsTrainer>
) {

    val message = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackquery?.message?.chat?.id ?: return
    val data = update.callbackquery?.data

    val trainer = trainers.getOrPut(chatId) { LearnWordsTrainer("$chatId.txt") }

    if (message?.lowercase() == "hello") {
        telegramBotService.sendMessage(json, chatId, message)
    }

    if (message?.lowercase() == "/start") {
        telegramBotService.sendMenu(json, chatId)
    }

    if (data == STATISTICS_CLICKED) {
        val statistics = trainer.getStatistics()
        val statisticsText =
            "Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}%"
        telegramBotService.sendMessage(json, chatId, statisticsText)
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

    if (data == RESET_CLICKED) {
        trainer.resetProgress()
        telegramBotService.sendMessage(json, chatId, "Прогресс сброшен")
    }

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
