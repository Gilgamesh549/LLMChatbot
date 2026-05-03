package com.example.llmchatbot

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val username: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: String
)