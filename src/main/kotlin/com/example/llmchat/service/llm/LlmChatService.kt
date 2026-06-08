package com.example.llmchat.service.llm

import org.springframework.ai.chat.messages.Message

interface LlmChatService {
    fun chat(message: Message): String?
}