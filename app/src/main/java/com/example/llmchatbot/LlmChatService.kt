package com.example.llmchatbot

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class LlmChatService {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun getBotReply(
        username: String,
        latestUserMessage: String,
        chatHistory: List<Message>
    ): String {
        if (BuildConfig.OPENAI_API_KEY.isBlank()) {
            return "LLM backend is not configured yet. Add OPENAI_API_KEY to local.properties to enable real chatbot replies."
        }

        val requestBody = JSONObject().apply {
            put("model", BuildConfig.OPENAI_MODEL)
            put("messages", buildMessages(username, latestUserMessage, chatHistory))
            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(BuildConfig.OPENAI_BASE_URL.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMessage = parseErrorMessage(responseBody)
                throw IllegalStateException("LLM request failed: $errorMessage")
            }

            return parseAssistantReply(responseBody)
                ?: throw IllegalStateException("The LLM service returned an empty response.")
        }
    }

    private fun buildMessages(
        username: String,
        latestUserMessage: String,
        chatHistory: List<Message>
    ): JSONArray {
        val messages = JSONArray()
        messages.put(
            JSONObject().apply {
                put("role", "system")
                put(
                    "content",
                    "You are a friendly chatbot inside an Android app. Address the user as $username when appropriate, keep replies concise, and answer clearly."
                )
            }
        )

        chatHistory.takeLast(10).forEach { message ->
            messages.put(
                JSONObject().apply {
                    put("role", if (message.isUser) "user" else "assistant")
                    put("content", message.content)
                }
            )
        }

        val historyAlreadyContainsLatestMessage =
            chatHistory.lastOrNull()?.isUser == true && chatHistory.lastOrNull()?.content == latestUserMessage

        if (!historyAlreadyContainsLatestMessage) {
            messages.put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", latestUserMessage)
                }
            )
        }

        return messages
    }

    private fun parseAssistantReply(responseBody: String): String? {
        val jsonObject = JSONObject(responseBody)
        val choices = jsonObject.optJSONArray("choices") ?: return null
        if (choices.length() == 0) {
            return null
        }

        return choices
            .optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun parseErrorMessage(responseBody: String): String {
        return try {
            JSONObject(responseBody)
                .optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: "HTTP request was not successful."
        } catch (_: Exception) {
            "HTTP request was not successful."
        }
    }
}
