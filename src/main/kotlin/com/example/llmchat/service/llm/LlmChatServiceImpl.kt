package com.example.llmchat.service.llm

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service

@Service
class LlmChatServiceImpl(
    private val chatClient: ChatClient,
) : LlmChatService {

    override fun chat(message: Message): String? {
        return chatClient
            .prompt(Prompt(message))
            .call()
            .content()
    }
}