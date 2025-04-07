package ru.fedorova.spring

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BOT_API_URL = "https://api.telegram.org/bot"
const val RESET_CLICKED = "reset_clicked"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackquery: CallbackQuery? = null,
)

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>?,
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long?,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("callback_data")
    val callbackData: String,
    @SerialName("text")
    val text: String,
)

class TelegramBotService(private val botToken: String) {

    private val client: HttpClient = HttpClient.newBuilder().build()

    private fun sendRequest(request: HttpRequest): String {
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }


    fun getUpdates(updateId: Long): String {
        val url = "$TELEGRAM_BOT_API_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(url)).build()
        return sendRequest(request)
    }

    fun sendMessage(json: Json, chatId: Long?, message: String): String {
        val url = "$TELEGRAM_BOT_API_URL$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = message,
        )
        val requestBodyString = json.encodeToString(requestBody)

        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        return sendRequest(request)
    }

    fun sendMenu(json: Json, chatId: Long?): String {
        val url = "$TELEGRAM_BOT_API_URL$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard(
                            text = "Изучать слова",
                            callbackData = LEARN_WORDS_CLICKED
                        ),
                        InlineKeyboard(
                            text = "Статистика",
                            callbackData = STATISTICS_CLICKED
                        )
                    ),
                    listOf(
                        InlineKeyboard(
                            text = "Сбросить прогресс",
                            callbackData = RESET_CLICKED
                        )
                    )
                )
            )
        )
        val requestBodyString = json.encodeToString(requestBody)

        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        return sendRequest(request)
    }

    fun sendQuestion(json: Json, chatId: Long?, question: Question): String {
        val url = "$TELEGRAM_BOT_API_URL$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = question.correctAnswer.original,
            replyMarkup = ReplyMarkup(
                listOf(question.variants.mapIndexed { index, word ->
                    InlineKeyboard(
                        text = word.translation, callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
                    )
                })
            )
        )
        val requestBodyString = json.encodeToString(requestBody)

        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        return sendRequest(request)
    }

}
