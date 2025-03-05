package ru.fedorova.spring

import java.io.File

const val MAXIMUM_PERCENT = 100
const val MAX_COUNT_RIGHT_ANSWERS = 3
const val NOT_LEARNED_WORDS_COUNT = 4

data class Word(
    val original: String,
    val translation: String,
    var correctAnswersCount: Int = 0,
)

fun loadDictionary(): MutableList<Word> {

    val wordsFile: File = File("words.txt")

    val dictionary: MutableList<Word> = mutableListOf()

    wordsFile.forEachLine { line ->
        val part = line.split("|")
        val correctAnswersCount = part[2].toIntOrNull() ?: 0
        val word = Word(
            original = part[0],
            translation = part[1],
            correctAnswersCount = correctAnswersCount
        )
        dictionary.add(word)
    }

    return dictionary

}

fun List<Word>.filterLearnedWords(): Int {
    return this.filter { it.correctAnswersCount >= MAX_COUNT_RIGHT_ANSWERS }.size
}

fun saveDictionary(dictionary: MutableList<Word>) {
    val wordsFile: File = File("words.txt")
    wordsFile.printWriter().use { out ->
        dictionary.forEach { word ->
            out.println("${word.original}|${word.translation}|${word.correctAnswersCount}")
        }
    }

}

fun main() {

    val dictionary = loadDictionary()

    while (true) {
        println("Меню:\n1 - Учить слова\n2 - Статистика\n0 - Выход")

        val input = readln().toString()

        when (input) {
            "1" -> {
                val notLearnedList = dictionary.filter { it.correctAnswersCount < MAX_COUNT_RIGHT_ANSWERS }

                if (notLearnedList.isEmpty()) {
                    println("Все слова в словаре выучены")
                    continue
                }

                val questionWords = notLearnedList.shuffled().take(NOT_LEARNED_WORDS_COUNT)
                val correctAnswer = questionWords.random()
                val answerChoices = questionWords.shuffled()

                println("${correctAnswer.original}:")

                answerChoices.forEachIndexed { index, word ->
                    println("\t${index + 1} - ${word.translation}")
                }

                val splitter = "-".repeat(10)
                println("\t" + splitter)
                println("\t0 - Меню")

                println("Введите номер правильного ответа:")
                val userAnswerInput = readln().toInt()

                if (userAnswerInput == 0) {
                    continue
                }

                val correctAnswerId = answerChoices.indexOf(correctAnswer) + 1

                if (userAnswerInput == correctAnswerId) {
                    correctAnswer.correctAnswersCount++
                    println("Правильно!")
                    saveDictionary(dictionary)
                } else {
                    println("Неправильно! ${correctAnswer.original} - это ${correctAnswer.translation}")
                }

            }
            "2" -> {
                val totalCount = dictionary.size
                val learnedCount = dictionary.filterLearnedWords()
                val percent = (learnedCount / totalCount) * MAXIMUM_PERCENT
                println("Выучено $learnedCount слов из $totalCount | $percent%")
                println()
            }
            "0" -> break
            else -> println("Введите число 1, 2 или 0")
        }
    }

}
