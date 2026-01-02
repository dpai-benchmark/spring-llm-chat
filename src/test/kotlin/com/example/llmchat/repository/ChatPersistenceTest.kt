package com.example.llmchat.repository

import com.example.llmchat.model.Chat
import com.example.llmchat.model.ChatEntity
import com.example.llmchat.model.Role
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import kotlin.test.assertEquals

@Testcontainers
@SpringBootTest
class ChatPersistenceTest @Autowired constructor(
    val repository: ChatRepository
) {

    @Test
    fun whenCorrectDataPassedThenItIsPersistedCorrectly() {
        val chat = Chat(title = "first_chat")
        chat.history.add(ChatEntity(role = Role.USER, createdAt = Instant.now(), chat = chat))
        chat.history.add(ChatEntity(role = Role.ASSISTANT, createdAt = Instant.now(), chat = chat))

        val savedChat = repository.saveAndFlush(chat)
        assertNotNull(savedChat.id)
        val chatFromDB = repository.findById(savedChat.id!!).get()

        assertEquals(2, chatFromDB.history.size)
        assertEquals(Role.USER, chatFromDB.history[0].role)
        assertEquals(Role.ASSISTANT, chatFromDB.history[1].role)
    }

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            // because  as per gradle config both openAI and ollama models are automatically available model should
            // be specified to avoid ambiguity
            registry.add("spring.ai.model.chat") { "ollama" }
            registry.add("spring.ai.openai.api-key") { "dummy" }
        }
    }
}