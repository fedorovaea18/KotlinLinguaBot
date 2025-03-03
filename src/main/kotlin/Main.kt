package ru.fedorova.spring

import java.io.File

const val MAXIMUM_PERCENT = 100
const val MAX_COUNT_RIGHT_ANSWERS = 3

data class Word(
    val original: String,
    val translation: String,
    val correctAnswersCount: Int = 0,
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

fun main() {

    val dictionary = loadDictionary()

    while (true) {
        println("Меню:\n1 - Учить слова\n2 - Статистика\n0 - Выход")

        val input = readln().toString()

        when (input) {
            "1" -> println("Учить слова")
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
