package ru.fedorova.spring

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BOT_API_URL = "https://api.telegram.org/bot"

class TelegramBotService(private val botToken: String) {

    private fun sendRequest(url: String): String {
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(url)).build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun getUpdates(updateId: Int): String {
        val url = "${TELEGRAM_BOT_API_URL}$botToken/getUpdates?offset=$updateId"
        return sendRequest(url)
    }

    fun sendMessage(chatId: Int, text: String): String {
        val url = "${TELEGRAM_BOT_API_URL}$botToken/sendMessage?chat_id=$chatId&text=$text"
        return sendRequest(url)
    }

}
