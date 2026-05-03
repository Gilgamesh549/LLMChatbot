package com.example.llmchatbot

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var loginLayout: LinearLayout
    private lateinit var chatLayout: LinearLayout
    private lateinit var usernameInput: EditText
    private lateinit var goButton: Button
    private lateinit var titleText: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button

    private lateinit var database: AppDatabase
    private lateinit var adapter: ChatAdapter
    private lateinit var llmChatService: LlmChatService

    private var currentUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginLayout = findViewById(R.id.loginLayout)
        chatLayout = findViewById(R.id.chatLayout)
        usernameInput = findViewById(R.id.usernameInput)
        goButton = findViewById(R.id.goButton)
        titleText = findViewById(R.id.titleText)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)

        database = AppDatabase.getDatabase(this)
        llmChatService = LlmChatService()

        adapter = ChatAdapter(mutableListOf())
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = adapter

        goButton.setOnClickListener {
            login()
        }

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun login() {
        val username = usernameInput.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter your username", Toast.LENGTH_SHORT).show()
            return
        }

        currentUsername = username

        loginLayout.visibility = View.GONE
        chatLayout.visibility = View.VISIBLE
        titleText.text = "ChatBot - $currentUsername"

        loadChatHistory()
    }

    private fun loadChatHistory() {
        lifecycleScope.launch {
            val history = withContext(Dispatchers.IO) {
                database.messageDao().getMessagesByUser(currentUsername)
            }

            if (history.isEmpty()) {
                val welcomeMessage = Message(
                    username = currentUsername,
                    content = "Welcome $currentUsername! How can I help you today?",
                    isUser = false,
                    timestamp = getCurrentTime()
                )

                withContext(Dispatchers.IO) {
                    database.messageDao().insertMessage(welcomeMessage)
                }
                adapter.addMessage(welcomeMessage)
                scrollChatToBottom()
            } else {
                adapter.setMessages(history)
                scrollChatToBottom()
            }
        }
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()

        if (text.isEmpty()) {
            Toast.makeText(this, "Please type a message", Toast.LENGTH_SHORT).show()
            return
        }

        val userMessage = Message(
            username = currentUsername,
            content = text,
            isUser = true,
            timestamp = getCurrentTime()
        )

        messageInput.text.clear()
        setSendingState(true)

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.messageDao().insertMessage(userMessage)
                }
                adapter.addMessage(userMessage)
                scrollChatToBottom()

                val chatHistory = adapter.getMessages()
                val botReplyText = withContext(Dispatchers.IO) {
                    llmChatService.getBotReply(
                        username = currentUsername,
                        latestUserMessage = text,
                        chatHistory = chatHistory
                    )
                }

                val botMessage = Message(
                    username = currentUsername,
                    content = botReplyText,
                    isUser = false,
                    timestamp = getCurrentTime()
                )

                withContext(Dispatchers.IO) {
                    database.messageDao().insertMessage(botMessage)
                }
                adapter.addMessage(botMessage)
                scrollChatToBottom()
            } catch (exception: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    exception.message ?: "Unable to contact the LLM service.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setSendingState(false)
            }
        }
    }

    private fun setSendingState(isSending: Boolean) {
        sendButton.isEnabled = !isSending
        messageInput.isEnabled = !isSending
        usernameInput.isEnabled = !isSending
        goButton.isEnabled = !isSending
    }

    private fun scrollChatToBottom() {
        if (adapter.itemCount > 0) {
            chatRecyclerView.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date())
    }
}
