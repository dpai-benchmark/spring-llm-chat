package com.example.llmchat.controller

import com.example.llmchat.model.Chat
import com.example.llmchat.repository.ChatRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
])
class ChatControllerApiTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var chatRepository: ChatRepository

    @TestConfiguration
    class ChatClientTestConfig {
        @Bean
        @Primary
        fun chatClientMock(): ChatClient {
            val client = Mockito.mock(ChatClient::class.java, Answers.RETURNS_DEEP_STUBS)
            `when`(client.prompt().user(Mockito.anyString()).call().content()).thenReturn("stubbed answer")
            return client
        }
    }

    @BeforeEach
    fun setup() {
        chatRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `GET root should display empty chat list initially`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(view().name("chat"))
            .andExpect(model().attributeExists("chats"))
    }

    @Test
    @Transactional
    fun `POST chat should create new chat and redirect`() {
        val title = "Test Chat"
        
        val result = mockMvc.perform(post("/chat").param("title", title))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrlPattern("/chat/*"))
            .andReturn()

        // Verify chat was created in database
        val chats = chatRepository.findAll()
        assertThat(chats).hasSize(1)
        assertThat(chats[0].title).isEqualTo(title)
    }

    @Test
    @Transactional
    fun `GET chat by id should display specific chat`() {
        // Create a chat directly in database
        val chat = chatRepository.save(Chat(title = "Test Chat"))
        val chatId = chat.id!!

        // Test GET /chat/{chatId}
        mockMvc.perform(get("/chat/{chatId}", chatId))
            .andExpect(status().isOk)
            .andExpect(view().name("chat"))
            .andExpect(model().attributeExists("chats"))
            .andExpect(model().attributeExists("chat"))
    }

    @Test
    @Transactional
    fun `DELETE chat should remove chat and redirect to root`() {
        // Create a chat directly in database
        val chat = chatRepository.save(Chat(title = "Test Chat"))
        val chatId = chat.id!!

        // Verify chat exists
        assertThat(chatRepository.findAll()).hasSize(1)

        // Delete the chat
        mockMvc.perform(delete("/chat/{chatId}", chatId))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))

        // Verify chat was deleted
        assertThat(chatRepository.findAll()).hasSize(0)
    }

    @Test
    @Transactional
    fun `POST chat with empty title should return 400 Bad Request`() {
        mockMvc.perform(post("/chat").param("title", ""))
            .andExpect(status().isBadRequest)

        // Verify no chat was created
        assertThat(chatRepository.findAll()).hasSize(0)
    }

    @Test
    @Transactional
    fun `GET chat with invalid ID should return 400 Bad Request`() {
        mockMvc.perform(get("/chat/{chatId}", "invalid-id"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun `DELETE chat with invalid ID should return 400 Bad Request`() {
        mockMvc.perform(delete("/chat/{chatId}", "invalid-id"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun `GET nonexistent chat should return 400 Bad Request`() {
        mockMvc.perform(get("/chat/{chatId}", "999999"))
            .andExpect(status().isBadRequest)
    }
}