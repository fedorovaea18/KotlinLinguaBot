package ru.fedorova.spring

import java.io.File

const val MAXIMUM_PERCENT = 100
const val MAX_COUNT_RIGHT_ANSWERS = 3
const val NOT_LEARNED_WORDS_COUNT = 4

class Statistics(
    val totalCount: Int,
    val learnedCount: Int,
    val percent: Int,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

class LearnWordsTrainer {

    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val totalCount = dictionary.size
        val learnedCount = dictionary.filterLearnedWords()
        val percent = (learnedCount / totalCount) * MAXIMUM_PERCENT
        return Statistics(totalCount, learnedCount, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < MAX_COUNT_RIGHT_ANSWERS }
        if (notLearnedList.isEmpty()) return null
        val questionWords = notLearnedList.shuffled().take(NOT_LEARNED_WORDS_COUNT)
        val correctAnswer = questionWords.random()
        question = Question(
            variants = questionWords,
            correctAnswer = correctAnswer,)
        return question
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerIndex) {
                it.correctAnswer.correctAnswersCount++
                saveDictionary(dictionary)
                true
            } else {
                false
            }
        } ?: false
    }

    private fun loadDictionary(): MutableList<Word> {
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

    private fun saveDictionary(dictionary: MutableList<Word>) {
        val wordsFile: File = File("words.txt")
        wordsFile.printWriter().use { out ->
            dictionary.forEach { word ->
                out.println("${word.original}|${word.translation}|${word.correctAnswersCount}")
            }
        }
    }

}
