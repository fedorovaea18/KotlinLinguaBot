package ru.fedorova.spring

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val TELEGRAM_BOT_API_URL = "https://api.telegram.org/bot"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"

class TelegramBotService(private val botToken: String) {

    private val client: HttpClient = HttpClient.newBuilder().build()

    private fun sendRequest(request: HttpRequest): String {
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun getUpdates(updateId: Int): String {
        val url = "${TELEGRAM_BOT_API_URL}$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(url)).build()
        return sendRequest(request)
    }

    fun sendMessage(chatId: Long, text: String): String {
        val encoded = URLEncoder.encode(
            text,
            StandardCharsets.UTF_8
        )
        val url = "${TELEGRAM_BOT_API_URL}$botToken/sendMessage?chat_id=$chatId&text=$encoded"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(url)).build()
        return sendRequest(request)
    }

    fun sendMenu(chatId: Long): String {
        val url = "${TELEGRAM_BOT_API_URL}$botToken/sendMessage"
            val sendMenuBody = """
                {
                    "chat_id": $chatId,
                    "text": "Основное меню",
                    "reply_markup": {
                        "inline_keyboard": [
                            [
                                {
                                    "text": "Изучить слова",
                                    "callback_data": "$LEARN_WORDS_CLICKED"
                                },
                                {
                                    "text": "Статистика",
                                    "callback_data": "$STATISTICS_CLICKED"
                                }
                            ]    
                        ]
                    }
                }    
        """.trimIndent()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody))
            .build()
        return sendRequest(request)
    }

}
