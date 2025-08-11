package ru.fedorova.spring

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse

class LearnWordsTrainerTest {
    private lateinit var testDictionary: TestUserDictionary
    private lateinit var trainer: LearnWordsTrainer

    @BeforeEach
    fun setUp() {
        testDictionary = TestUserDictionary()
        trainer = LearnWordsTrainer(testDictionary)
    }

    @Test
    fun testInitialStatistics() {
        val statistics = trainer.getStatistics()
        assertEquals(5, statistics.totalCount)
        assertEquals(0, statistics.learnedCount)
        assertEquals(0, statistics.percent)
    }

    @Test
    fun testGetNextQuestionWithUnlearnedWords() {
        val question = trainer.getNextQuestion()

        assertNotNull(question)
        assertEquals(4, question.variants.size)
        assertTrue((question.variants.contains(question.correctAnswer)))

        val unlearnedWords = testDictionary.getUnlearnedWords().map { it.original }.toSet()
        question.variants.forEach { word ->
            assertTrue(unlearnedWords.contains(word.original))
        }
    }

    @Test
    fun testGetNextQuestionWithMixedWords() {

        testDictionary.setCorrectAnswersCount("cat", 3)
        testDictionary.setCorrectAnswersCount("dog", 3)

        val question = trainer.getNextQuestion()

        assertNotNull(question)
        assertEquals(4, question.variants.size)

        val allWords =
            (testDictionary.getLearnedWords() + testDictionary.getUnlearnedWords()).map { it.original }.toSet()
        question.variants.forEach { word ->
            assertTrue(allWords.contains(word.original))
        }
    }

    @Test
    fun testGetNextQuestionWhenAllWordsLearned() {

        testDictionary.getUnlearnedWords().forEach { word ->
            testDictionary.setCorrectAnswersCount(word.original, 3)
        }

        val question = trainer.getNextQuestion()
        assertNull(question)
    }

    @Test
    fun testCheckAnswerCorrect() {
        val question = trainer.getNextQuestion()
        assertNotNull(question)

        val correctAnswerIndex = question.variants.indexOf(question.correctAnswer)
        val isCorrect = trainer.checkAnswer(correctAnswerIndex)

        assertTrue(isCorrect)
        assertEquals(1, testDictionary.getCorrectAnswersCount(question?.correctAnswer?.original ?: ""))
    }

    @Test
    fun testCheckAnswerIncorrect() {
        val question = trainer.getNextQuestion()
        assertNotNull(question)

        val correctAnswerIndex = question?.variants?.indexOf(question.correctAnswer)
        val wrongAnswerIndex = if (correctAnswerIndex == 0) 1 else 0

        val isCorrect = trainer.checkAnswer(wrongAnswerIndex)

        assertFalse(isCorrect)
        assertEquals(0, testDictionary.getCorrectAnswersCount(question?.correctAnswer?.original ?: ""))
    }

    @Test
    fun testCheckAnswerNull() {
        val isCorrect = trainer.checkAnswer(null)
        assertFalse(isCorrect)
    }

    @Test
    fun testCheckAnswerNoQuestion() {
        val isCorrect = trainer.checkAnswer(0)
        assertFalse(isCorrect)
    }

    @Test
    fun testStatisticsAfterLearning() {

        val question1 = trainer.getNextQuestion()
        assertNotNull(question1)
        val correctIndex1 = question1.variants.indexOf(question1.correctAnswer)
        repeat(3) { trainer.checkAnswer(correctIndex1) }

        val question2 = trainer.getNextQuestion()
        assertNotNull(question2)
        val correctIndex2 = question2.variants.indexOf(question2.correctAnswer)
        repeat(3) { trainer.checkAnswer(correctIndex2) }

        val statistics = trainer.getStatistics()
        assertEquals(5, statistics.totalCount)
        assertEquals(2, statistics.learnedCount)
        assertEquals(40, statistics.percent)
    }

    @Test
    fun testResetProgress() {

        val question = trainer.getNextQuestion()
        assertNotNull(question)
        val correctIndex = question.variants.indexOf(question.correctAnswer)
        repeat(3) { trainer.checkAnswer(correctIndex) }

        assertEquals(1, trainer.getStatistics().learnedCount)

        trainer.resetProgress()

        assertEquals(0, trainer.getStatistics().learnedCount)
    }

    private class TestUserDictionary : IUserDictionary {
        private val words = mutableMapOf<String, Int>()
        private val allWords = listOf(
            Word("cat", "кошка", originalWord = text),
            Word("dog", "собака", originalWord = text),
            Word("bird", "птица", originalWord = text),
            Word("fish", "рыба", originalWord = text),
            Word("tree", "дерево", originalWord = text)
        )

        override fun getNumOfLearnedWords(): Int {
            return words.values.count { it >= 3 }
        }

        override fun getSize(): Int = allWords.size

        override fun getLearnedWords(): List<Word> {
            return allWords.filter { (words[it.original] ?: 0) >= 3 }
        }

        override fun getUnlearnedWords(): List<Word> {
            return allWords.filter { (words[it.original] ?: 0) < 3 }
        }

        override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
            words[word] = correctAnswersCount
        }

        override fun getCorrectAnswersCount(word: String): Int {
            return words[word] ?: 0
        }

        override fun resetUserProgress() {
            words.clear()
        }
    }
} 