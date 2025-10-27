package com.example.llmchat

import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class LlmChatExampleApplication {
    @Bean
    fun chatClient(builder: ChatClient.Builder): ChatClient {
        return builder.build()
    }
}

fun main(args: Array<String>) {
    runApplication<LlmChatExampleApplication>(*args)
}
