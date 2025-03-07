package ru.fedorova.spring

import java.io.File

const val MAXIMUM_PERCENT = 100

class Statistics(
    val totalCount: Int,
    val learnedCount: Int,
    val percent: Int,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

class LearnWordsTrainer(private val learnedAnswerCount: Int = 3, private val countOfQuestionWords: Int = 4) {

    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val totalCount = dictionary.size
        val learnedCount = dictionary.filterLearnedWords()
        val percent = (learnedCount  * MAXIMUM_PERCENT) / totalCount
        return Statistics(totalCount, learnedCount, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < learnedAnswerCount }
        if (notLearnedList.isEmpty()) return null
        val questionWords = if (notLearnedList.size < countOfQuestionWords) {
            val learnedList = dictionary.filter { it.correctAnswersCount >= learnedAnswerCount }.shuffled()
            notLearnedList.shuffled().take(countOfQuestionWords) +
                    learnedList.take(countOfQuestionWords - notLearnedList.size)
        } else {
            notLearnedList.shuffled().take(countOfQuestionWords)
        }.shuffled()
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
