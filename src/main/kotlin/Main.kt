package ru.fedorova.spring

import java.io.File

fun main() {

    val wordsFile: File = File("words.txt")
    wordsFile.createNewFile()
    val lines = wordsFile.readLines()

    for (line in lines) {
        println(line)
    }

}
