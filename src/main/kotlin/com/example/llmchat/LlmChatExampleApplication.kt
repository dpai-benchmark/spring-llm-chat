package com.example.llmchat

import com.example.llmchat.repository.ChatRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class LlmChatExampleApplication {

    @Autowired
    private lateinit var chatRepository: ChatRepository

    @Bean
    fun chatClient(builder: ChatClient.Builder): ChatClient {
        return builder.defaultAdvisors(createDefaultAdvisor()).build()
    }

    private fun createDefaultAdvisor(): Advisor {
        return MessageChatMemoryAdvisor.builder(createChatMemory()).build()
    }

    private fun createChatMemory(): ChatMemory {
        return MessageWindowChatMemory
            .builder()
            .chatMemoryRepository(chatRepository)
            .maxMessages(20).build()
    }
}

fun main(args: Array<String>) {
    runApplication<LlmChatExampleApplication>(*args)
}
