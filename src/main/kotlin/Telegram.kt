package ru.fedorova.spring

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TelegramBot(private val botToken: String) {

    private val client: HttpClient = HttpClient.newBuilder().build()

    fun getMe(botToken: String): String {
        val urlGetMe = "https://api.telegram.org/bot$botToken/getMe"
        val requestGetMe: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetMe)).build()
        val responseGetMe: HttpResponse<String> = client.send(requestGetMe, HttpResponse.BodyHandlers.ofString())

        return responseGetMe.body()

    }

    fun getUpdates(botToken: String, updateId: Int): String {
        val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
        val requestGetUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val responseGetUpdates: HttpResponse<String> =
            client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

        return responseGetUpdates.body()

    }
}

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0

    while (true) {
        Thread.sleep(2000)
        val updates: String = TelegramBot(botToken).getUpdates(botToken, updateId)
        println(updates)

        val updateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
        val updateIdMatch: MatchResult? = updateIdRegex.find(updates)
        updateId = updateIdMatch?.groups?.get(1)?.value?.toInt()?.plus(1) ?: 0
        //println(updateId)

        val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
        val messageTextMatch: MatchResult? = messageTextRegex.find(updates)
        val text = messageTextMatch?.groups?.get(1)?.value
        println(text)
    }

}
