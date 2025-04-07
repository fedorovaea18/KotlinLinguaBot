package ru.fedorova.spring

const val MAXIMUM_PERCENT = 100

data class Word(
    val original: String,
    val translation: String,
    var correctAnswersCount: Int = 0,
)

class Statistics(
    val totalCount: Int,
    val learnedCount: Int,
    val percent: Int,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

class LearnWordsTrainer(
    private val userDictionary: IUserDictionary,
    private val countOfQuestionWords: Int = 4
) {

    internal var question: Question? = null

    fun getStatistics(): Statistics {
        val totalCount = userDictionary.getSize()
        val learnedCount = userDictionary.getNumOfLearnedWords()
        val percent = (learnedCount * MAXIMUM_PERCENT) / totalCount
        return Statistics(totalCount, learnedCount, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = userDictionary.getUnlearnedWords()
        if (notLearnedList.isEmpty()) return null
        val questionWords = if (notLearnedList.size < countOfQuestionWords) {
            val learnedList = userDictionary.getLearnedWords().shuffled()
            notLearnedList.shuffled().take(countOfQuestionWords) +
                    learnedList.take(countOfQuestionWords - notLearnedList.size)
        } else {
            notLearnedList.shuffled().take(countOfQuestionWords)
        }.shuffled()
        val correctAnswer = questionWords.random()
        question = Question(
            variants = questionWords,
            correctAnswer = correctAnswer,
        )
        return question
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerIndex) {
                userDictionary.setCorrectAnswersCount(it.correctAnswer.original, it.correctAnswer.correctAnswersCount++)
                true
            } else {
                false
            }
        } ?: false
    }

    fun resetProgress() {
        userDictionary.resetUserProgress()
    }

}
