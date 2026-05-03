# LLM ChatBot App (SIT305 Task 8.1C)

## Description
This is an Android application developed for SIT305 Task 8.1C. The app allows users to log in with a username and interact with an AI-powered chatbot. User messages are sent to an OpenAI-compatible LLM backend, and chatbot replies are displayed in a chat interface.

The application also saves chat history locally using Room database, so previous conversations can still be viewed after the app is restarted.

## Features
* **User Login:** Allows users to enter a username before accessing the chatbot screen.
* **Chat Interface:** Provides a simple messaging interface where users can send messages and receive chatbot responses.
* **LLM Backend Integration:** Connects to an OpenAI-compatible `/chat/completions` API to generate real AI chatbot replies.
* **Room Database Storage:** Saves chat messages locally using Room database.
* **Chat History:** Restores previous conversations when the same username logs in again.
* **User-Based History:** Stores messages separately for each username.
* **Timestamps:** Displays a timestamp on each message bubble.
* **Error Handling:** Shows clear messages when the LLM backend is not configured, the API request fails, or the account quota is exceeded.
* **Secure API Configuration:** Stores the API key in `local.properties`, which is ignored by Git and should not be uploaded to GitHub.

## Tools Used
* Android Studio
* Kotlin
* Room Database
* RecyclerView
* OpenAI-compatible LLM API
* Gradle

## Setup Instructions

To connect the app to a real LLM backend, create or edit the `local.properties` file in the project root and add the following values:

```properties
OPENAI_API_KEY=your_api_key_here
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-4o-mini
