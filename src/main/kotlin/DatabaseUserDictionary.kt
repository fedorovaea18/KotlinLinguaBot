package ru.fedorova.spring

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class DatabaseUserDictionary(
    private val connection: Connection, private val learningThreshold: Int = 3
) : IUserDictionary {

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
            translate VARCHAR NOT NULL
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
        val query = """
            SELECT COUNT(*)
            FROM user_answers
            WHERE correct_answer_count >= ?
        """.trimIndent()
        connection.prepareStatement(query).use { statement ->
            statement.setInt(1, learningThreshold)
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

    private fun getWords(query: String, threshold: Int): List<Word> {
        return connection.prepareStatement(query).use { statement ->
            statement.setInt(1, threshold)
            statement.executeQuery().use { resultSet ->
                val wordsList = mutableListOf<Word>()
                while (resultSet.next()) {
                    val original = resultSet.getString("text")
                    val translation = resultSet.getString("translate")
                    val correctAnswersCount = resultSet.getInt("correct_answer_count")
                    wordsList.add(Word(original, translation, correctAnswersCount))
                }
                wordsList
            }
        }
    }

    override fun getLearnedWords(): List<Word> {
        val query = """
            SELECT words.text, words.translate, user_answers.correct_answer_count
            FROM words
            JOIN user_answers ON words.id = user_answers.word_id
            WHERE user_answers.correct_answer_count >= ?
        """.trimIndent()
        return getWords(query, learningThreshold)
    }

    override fun getUnlearnedWords(): List<Word> {
        val query = """
            SELECT words.text, words.translate, user_answers.correct_answer_count
            FROM words
            JOIN user_answers ON words.id = user_answers.word_id
            WHERE user_answers.correct_answer_count < ?
        """.trimIndent()
        return getWords(query, learningThreshold)
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        val query = """
            UPDATE user_answers
            SET correct_answer_count = ?
            WHERE word_id = (SELECT id FROM words WHERE text = ?)
        """.trimIndent()
        connection.prepareStatement(query).use { statement ->
            statement.setInt(1, correctAnswersCount)
            statement.setString(2, word)
            statement.executeUpdate()
        }
    }

    override fun resetUserProgress() {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                DELETE FROM user_answers
                """.trimIndent()
            )
        }
    }
}

fun updateDictionary(wordsFile: File, connection: Connection) {
    val query = """
        INSERT OR IGNORE INTO words (text, translate)
        VALUES (?, ?)
    """.trimIndent()

    DriverManager.getConnection("jdbc:sqlite:data.db").use { connection ->
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
                connection.commit()
            }
        } catch (e: Exception) {
            println("Ошибка при обновлении словаря: ${e.message}")
            connection.rollback()
        } finally {
            connection.autoCommit = true
        }
    }
}
