package ru.fedorova.spring

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseUserDictionaryTest {
    private lateinit var connection: Connection
    private lateinit var dictionary: DatabaseUserDictionary

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        dictionary = DatabaseUserDictionary(connection)
        dictionary.setCurrentChatId(1L)

        connection.prepareStatement("INSERT INTO users (username, chat_id) VALUES (?, ?)").use {
            it.setString(1, "testuser")
            it.setLong(2, 1L)
            it.executeUpdate()
        }

        connection.prepareStatement("INSERT INTO words (text, translate) VALUES (?, ?)").use {
            it.setString(1, "cat")
            it.setString(2, "кошка")
            it.executeUpdate()
        }
        connection.prepareStatement("INSERT INTO words (text, translate) VALUES (?, ?)").use {
            it.setString(1, "dog")
            it.setString(2, "собака")
            it.executeUpdate()
        }
        connection.prepareStatement("INSERT INTO words (text, translate) VALUES (?, ?)").use {
            it.setString(1, "bird")
            it.setString(2, "птица")
            it.executeUpdate()
        }
    }

    @AfterEach
    fun tearDown() {
        connection.close()
    }

    @Test
    fun testInitialStatistics() {
        assertEquals(3, dictionary.getSize())
        assertEquals(0, dictionary.getNumOfLearnedWords())
    }

    @Test
    fun testSetCorrectAnswersCount() {
        dictionary.setCorrectAnswersCount("cat", 1)

        connection.prepareStatement("SELECT correctAnswerCount FROM words WHERE text = ?").use {
            it.setString(1, "cat")
            val resultSet = it.executeQuery()
            assertTrue(resultSet.next())
            assertEquals(1, resultSet.getInt("correctAnswerCount"))
        }

        connection.prepareStatement("""
            SELECT correct_answer_count FROM user_answers 
            WHERE user_id = (SELECT id FROM users WHERE chat_id = ?) 
            AND word_id = (SELECT id FROM words WHERE text = ?)
        """.trimIndent()).use {
            it.setLong(1, 1L)
            it.setString(2, "cat")
            val resultSet = it.executeQuery()
            assertTrue(resultSet.next())
            assertEquals(1, resultSet.getInt("correct_answer_count"))
        }
    }

    @Test
    fun testLearnedWordsThreshold() {

        dictionary.setCorrectAnswersCount("cat", 2)
        dictionary.setCorrectAnswersCount("dog", 1)

        assertEquals(0, dictionary.getNumOfLearnedWords())
        assertEquals(3, dictionary.getUnlearnedWords().size)

        dictionary.setCorrectAnswersCount("cat", 3)

        assertEquals(1, dictionary.getNumOfLearnedWords())
        assertEquals(2, dictionary.getUnlearnedWords().size)

        val learnedWords = dictionary.getLearnedWords()
        assertEquals(1, learnedWords.size)
        assertEquals("cat", learnedWords[0].original)
    }

    @Test
    fun testMultipleCorrectAnswers() {

        dictionary.setCorrectAnswersCount("cat", 1)
        dictionary.setCorrectAnswersCount("cat", 2)
        dictionary.setCorrectAnswersCount("cat", 3)

        assertEquals(1, dictionary.getNumOfLearnedWords())

        connection.prepareStatement("SELECT correct_answer_count FROM user_answers WHERE word_id = (SELECT id FROM words WHERE text = ?)").use {
            it.setString(1, "cat")
            val resultSet = it.executeQuery()
            assertTrue(resultSet.next())
            assertEquals(3, resultSet.getInt("correct_answer_count"))
        }
    }

    @Test
    fun testResetUserProgress() {

        dictionary.setCorrectAnswersCount("cat", 3)
        dictionary.setCorrectAnswersCount("dog", 2)

        assertEquals(1, dictionary.getNumOfLearnedWords())

        dictionary.resetUserProgress()

        assertEquals(0, dictionary.getNumOfLearnedWords())
        assertEquals(3, dictionary.getUnlearnedWords().size)

        connection.prepareStatement("SELECT COUNT(*) FROM user_answers").use {
            val resultSet = it.executeQuery()
            assertTrue(resultSet.next())
            assertEquals(0, resultSet.getInt(1))
        }
    }

    @Test
    fun testGetLearnedAndUnlearnedWords() {

        dictionary.setCorrectAnswersCount("cat", 3)
        dictionary.setCorrectAnswersCount("dog", 2)
        dictionary.setCorrectAnswersCount("bird", 0)

        val learnedWords = dictionary.getLearnedWords()
        val unlearnedWords = dictionary.getUnlearnedWords()

        assertEquals(1, learnedWords.size)
        assertEquals("cat", learnedWords[0].original)

        assertEquals(2, unlearnedWords.size)
        val unlearnedTexts = unlearnedWords.map { it.original }.toSet()
        assertTrue(unlearnedTexts.contains("dog"))
        assertTrue(unlearnedTexts.contains("bird"))
    }

    @Test
    fun testSetCorrectAnswersCountWithoutChatId() {
        val dictionaryWithoutChatId = DatabaseUserDictionary(connection)
        assertThrows<IllegalStateException> {
            dictionaryWithoutChatId.setCorrectAnswersCount("cat", 1)
        }
    }

    @Test
    fun testGetNumOfLearnedWordsWithoutChatId() {
        val dictionaryWithoutChatId = DatabaseUserDictionary(connection)
        assertThrows<IllegalStateException> {
            dictionaryWithoutChatId.getNumOfLearnedWords()
        }
    }
} 