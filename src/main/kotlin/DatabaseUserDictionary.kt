package ru.fedorova.spring

import java.io.File
import java.sql.Connection

class DatabaseUserDictionary(
    private val connection: Connection, private val learningThreshold: Int = 3
) : IUserDictionary {

    private var currentChatId: Long? = null

    fun setCurrentChatId(chatId: Long) {
        this.currentChatId = chatId
    }

    init {
        initializeDatabase(connection)
    }

    private fun initializeDatabase(connection: Connection) {
        try {
            connection.autoCommit = false

            val createWordsTableQuery = """
            CREATE TABLE IF NOT EXISTS words (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text VARCHAR NOT NULL UNIQUE,
                translate VARCHAR NOT NULL,
                correctAnswerCount INTEGER DEFAULT 0
            );
        """.trimIndent()

            val createUsersTableQuery = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username VARCHAR NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                chat_id INTEGER UNIQUE NOT NULL
            );
        """.trimIndent()

            val createAnswersTableQuery = """
            CREATE TABLE IF NOT EXISTS user_answers (
                user_id INTEGER,
                word_id INTEGER,
                correct_answer_count INTEGER DEFAULT 0,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, word_id),
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (word_id) REFERENCES words(id)
            );
        """.trimIndent()

            connection.createStatement().use { statement ->
                statement.execute(createUsersTableQuery)
                statement.execute(createWordsTableQuery)
                statement.execute(createAnswersTableQuery)
            }

            connection.commit()
            println("Транзакция успешно завершена.")
        } catch (e: Exception) {
            connection.rollback()
            println("Ошибка при выполнении инициализации базы данных: ${e.message}")
        } finally {
            connection.autoCommit = true
        }
    }


    override fun getNumOfLearnedWords(): Int {
        val chatId = currentChatId ?: throw IllegalStateException("Chat ID не получен")
        val query = """
        SELECT COUNT(*) 
        FROM user_answers 
        WHERE user_id = (SELECT id FROM users WHERE chat_id = ?)
        AND correct_answer_count >= $learningThreshold;
        """.trimIndent()
        connection.prepareStatement(query).use { statement ->
            statement.setLong(1, chatId)
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) {
                    resultSet.getInt(1)
                } else {
                    0
                }
            }
        }
    }

    override fun getSize(): Int {
        val query = """
            SELECT COUNT(*) FROM words
        """.trimIndent()
        connection.prepareStatement(query).use { statement ->
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) {
                    resultSet.getInt(1)
                } else {
                    0
                }
            }
        }
    }

    private fun getWords(query: String): List<Word> {
        val wordsList = mutableListOf<Word>()
        connection.prepareStatement(query).use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val original = resultSet.getString("text")
                    val translation = resultSet.getString("translate")
                    val correctAnswersCount = resultSet.getInt("correct_answer_count")
                    wordsList.add(Word(original, translation, correctAnswersCount))
                }
            }
        }
        return wordsList
    }

    override fun getLearnedWords(): List<Word> {
        return getWords(
            """
             SELECT words.text, words.translate, COALESCE(user_answers.correct_answer_count, 0) AS correct_answer_count
             FROM words
             LEFT JOIN user_answers ON words.id = user_answers.word_id
             WHERE user_answers.correct_answer_count >= $learningThreshold
            """.trimIndent()
        )
    }

    override fun getUnlearnedWords(): List<Word> {
        return getWords(
            """
             SELECT words.text, words.translate, COALESCE(user_answers.correct_answer_count, 0) AS correct_answer_count
             FROM words
             LEFT JOIN user_answers ON words.id = user_answers.word_id
             WHERE user_answers.correct_answer_count IS NULL OR user_answers.correct_answer_count < $learningThreshold
            """.trimIndent()
        )
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        val chatId = currentChatId ?: throw IllegalStateException("Chat ID не получен")

        val query = """
        INSERT OR REPLACE INTO user_answers (user_id, word_id, correct_answer_count, updated_at)
        VALUES (
            (SELECT id FROM users WHERE chat_id = ?),
            (SELECT id FROM words WHERE text = ?),
            ?,
            CURRENT_TIMESTAMP
        );
        """.trimIndent()
        connection.prepareStatement(query).use { statement ->
            statement.setLong(1, chatId)
            statement.setString(2, word)
            statement.setInt(3, correctAnswersCount)
            statement.executeUpdate()
        }

        val updateWordSql = """
            UPDATE words SET correctAnswerCount = ? WHERE text = ?
        """.trimIndent()
        connection.prepareStatement(updateWordSql).use { stmt ->
            stmt.setInt(1, correctAnswersCount)
            stmt.setString(2, word)
            stmt.executeUpdate()
        }
    }

    override fun getCorrectAnswersCount(word: String): Int {
        val chatId = currentChatId ?: throw IllegalStateException("Chat ID не получен")
        val query = """
            SELECT correct_answer_count 
            FROM user_answers 
            WHERE user_id = (SELECT id FROM users WHERE chat_id = ?)
            AND word_id = (SELECT id FROM words WHERE text = ?)
        """.trimIndent()
        connection.prepareStatement(query).use { statement ->
            statement.setLong(1, chatId)
            statement.setString(2, word)
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) {
                    resultSet.getInt("correct_answer_count")
                } else {
                    0
                }
            }
        }
    }

    override fun resetUserProgress() {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                DELETE FROM user_answers
                """.trimIndent()
            )
            statement.executeUpdate(
                """
                UPDATE words SET correctAnswerCount = 0
                """.trimIndent()
            )
        }
    }
}

fun updateDictionary(wordsFile: File, connection: Connection) {
    synchronized(connection) {
        val query = """
        INSERT OR IGNORE INTO words (text, translate)
        VALUES (?, ?)
    """.trimIndent()
        connection.autoCommit = false
        try {
            connection.prepareStatement(query).use { statement ->
                wordsFile.forEachLine { line ->
                    val parts = line.split("|")
                    if (parts.size >= 2) {
                        val original = parts[0].trim()
                        val translation = parts[1].trim()
                        statement.setString(1, original)
                        statement.setString(2, translation)
                        statement.addBatch()
                    } else {
                        println("Некорректный формат строки: $line")
                    }
                }
                statement.executeBatch()
            }
            connection.commit()
            println("Словарь успешно обновлён")
        } catch (e: Exception) {
            connection.rollback()
            println("Ошибка при обновлении словаря: ${e.message}")
            throw e
        } finally {
            connection.autoCommit = true
        }
    }
}
