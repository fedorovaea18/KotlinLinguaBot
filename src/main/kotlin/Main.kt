package ru.fedorova.spring

const val SEPARATOR_LENGTH = 10

fun Question.asConsoleString(): String {
    val variants = this.variants
        .mapIndexed { index, word: Word -> "\t${index + 1} - ${word.translation}" }
        .joinToString(separator = "\n")
    return this.correctAnswer.original + "\n" + variants + "\n\t" + "-".repeat(SEPARATOR_LENGTH) + "\n\t0 - Меню"
}

fun List<Word>.filterLearnedWords(learnedAnswerCount: Int = 3): Int {
    return this.filter { it.correctAnswersCount >= learnedAnswerCount }.size
}

fun main() {

    val trainer = try {
        LearnWordsTrainer(learnedAnswerCount = 3, countOfQuestionWords = 4)
    } catch (e: Exception) {
        println("Невозможно загрузить словарь")
        return
    }

    while (true) {

        println("Меню:\n1 - Учить слова\n2 - Статистика\n0 - Выход")
        val input = readln().toIntOrNull()
        when (input) {
            1 -> {
                while (true) {
                    val question = trainer.getNextQuestion()

                    if (question == null) {
                        println("Все слова в словаре выучены")
                        break
                    } else {
                        println(question.asConsoleString())

                        println("Введите номер правильного ответа:")
                        val userAnswerInput = readln().toIntOrNull()

                        if (userAnswerInput == 0) {
                            break
                        }

                        if (trainer.checkAnswer(userAnswerInput?.minus(1))) {
                            println("Правильно!")
                        } else {
                            println("Неправильно! ${question.correctAnswer.original} - это ${question.correctAnswer.translation}")
                        }

                    }

                }
            }

            2 -> {
                val statistics = trainer.getStatistics()
                println("Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}%")
                println()
            }

            0 -> break
            else -> println("Введите число 1, 2 или 0")
        }
    }

}
