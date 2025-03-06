package ru.fedorova.spring

data class Word(
    val original: String,
    val translation: String,
    var correctAnswersCount: Int = 0,
)

fun Question.asConsoleString(): String {
    val variants = this.variants
        .mapIndexed { index, word: Word ->  "\t${index + 1} - ${word.translation}"}
        .joinToString(separator = "\n")
    return this.correctAnswer.original + "\n" + variants + "\n\t" + "-".repeat(10) + "\n\t0 - Меню"
}

fun List<Word>.filterLearnedWords(): Int {
    return this.filter { it.correctAnswersCount >= MAX_COUNT_RIGHT_ANSWERS }.size
}

fun main() {

    val trainer = LearnWordsTrainer()

    while (true) {

        println("Меню:\n1 - Учить слова\n2 - Статистика\n0 - Выход")
        val input = readln().toIntOrNull()
        when (input) {
            1 -> {
                val question = trainer.getNextQuestion()

                if (question == null) {
                    println("Все слова в словаре выучены")
                    continue
                } else {
                    println(question.asConsoleString())
                }

                println("Введите номер правильного ответа:")
                val userAnswerInput = readln().toIntOrNull()

                if (userAnswerInput == 0) {
                    continue
                }

                if (trainer.checkAnswer(userAnswerInput?.minus(1))) {
                    println("Правильно!")
                } else {
                    println("Неправильно! ${question.correctAnswer.original} - это ${question.correctAnswer.translation}")
                }
            }

            2 -> {
                val statistics = trainer.getStatistics()
                println("Выучено ${statistics.learnedCount} слов из ${statistics.totalCount} | ${statistics.percent}%")
                println()
            }

            0 -> break
            else -> println("Введите число 1, 2 или 0")
        }
    }

}
