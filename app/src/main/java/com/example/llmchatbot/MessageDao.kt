package com.example.llmchatbot

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {

    @Insert
    suspend fun insertMessage(message: Message)

    @Query("SELECT * FROM messages WHERE username = :username ORDER BY id ASC")
    suspend fun getMessagesByUser(username: String): List<Message>

    @Query("DELETE FROM messages WHERE username = :username")
    suspend fun clearMessages(username: String)
}