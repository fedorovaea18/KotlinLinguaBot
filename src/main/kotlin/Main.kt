package ru.fedorova.spring

import java.sql.DriverManager
import java.io.File

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

    val connection = DriverManager.getConnection("jdbc:sqlite:data.db")
    connection.use {
        updateDictionary(File("words.txt"), connection)
        
        val dictionary = try {
            DatabaseUserDictionary(connection)
        } catch (e: Exception) {
            println("Невозможно загрузить словарь")
            return
        }
        dictionary.setCurrentChatId(1L) // Set a fixed chat ID for console use

        // Ensure user with chat_id = 1 exists in the users table
        val username = "console_user"
        val chatId = 1L
        val insertUserSql = """
            INSERT OR IGNORE INTO users (username, chat_id) VALUES (?, ?)
        """.trimIndent()
        connection.prepareStatement(insertUserSql).use { stmt ->
            stmt.setString(1, username)
            stmt.setLong(2, chatId)
            stmt.executeUpdate()
        }

        val trainer = LearnWordsTrainer(dictionary)

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

}