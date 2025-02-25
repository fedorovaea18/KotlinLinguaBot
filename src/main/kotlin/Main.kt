package ru.fedorova.spring

import java.io.File

fun main() {

    val wordsFile: File = File("words.txt")
    wordsFile.createNewFile()
    wordsFile.forEachLine { println(it) }

}
