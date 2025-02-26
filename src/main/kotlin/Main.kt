package ru.fedorova.spring

import java.io.File

data class Word(
    val original: String,
    val translation: String,
    val correctAnswersCount: Int = 0,
)

fun main() {

    val wordsFile: File = File("words.txt")

    val dictionary: MutableList<Word> = mutableListOf()

    //wordsFile.createNewFile()

    wordsFile.forEachLine { line ->
        val part = line.split("|")
        val correctAnswersCount = part[2].toIntOrNull() ?: 0
        val word = Word(original = part[0], translation = part[1], correctAnswersCount = correctAnswersCount)
        dictionary.add(word)
    }

    println(dictionary)

}
