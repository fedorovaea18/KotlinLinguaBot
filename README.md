# **KotlinLinguaBot**

Telegram is a bot for learning vocabulary through interactive multiple choice quizzes and tracking statistics using the official Telegram API.

## **Start**
1. Clone the repository
```
git clone git@github.com:fedorovaea18/KotlinLinguaBot.git
cd KotlinLinguaBot
```
2. Build the project and create shadow JAR
This will create `build/libs/KotlinLinguaBot-1.0-SNAPSHOT-all.jar`
3. Prepare dictionary file (optional)
The bot can work without `words.txt` - you can upload words via Telegram later. However, if you want to start with a dictionary, create a `words.txt` file in the project root with the following format: `original word|translation`

## Usage

### Telegram Bot

1. Get your bot token from [@BotFather](https://t.me/BotFather)
2. Run the bot:
```
java -jar build/libs/KotlinLinguaBot-1.0-SNAPSHOT-all.jar YOUR_BOT_TOKEN
```
3. Start chatting with your bot on Telegram
4. Send `/start` to see the menu

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
- _API: Telegram Bot API_;
- _Serialization: Kotlinx Serialization JSON_;
- _Database: SQLite_;
- _Testing: JUnit5_.



