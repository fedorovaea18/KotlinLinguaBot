# **KotlinLinguaBot**

A Telegram bot for learning vocabulary words with interactive quizzes with multiple-choice answers and statistics tracking.

## **Start**


## Configuration

### Learning options

By default, a word is considered "learned" after 3 correct answers. You can change this in `DatabaseUserDictionary.kt`:

```kotlin
class DatabaseUserDictionary(
    private val connection: Connection, 
    private val learningThreshold: Int = 3  // Change this value
)
```

### Question options

Default is 4 answer options. Change in `LearnWordsTrainer.kt`:

```kotlin
class LearnWordsTrainer(
    private val fileUserDictionary: IUserDictionary,
    private val countOfQuestionWords: Int = 4  // Change this value
)
```

## **Technology stack**

