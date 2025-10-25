package com.example.llmchat.controller

import com.example.llmchat.model.Chat
import com.example.llmchat.model.Role
import com.example.llmchat.repository.ChatRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
])
class ChatControllerIntegrationTest {

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

    @Test
    @Transactional
    fun `POST talkToModel creates two entries (user and assistant)`() {
        // Arrange: create and persist a chat
        val chat = chatRepository.save(Chat(title = "Integration Chat"))
        val chatId = chat.id
        assertNotNull(chatId)

        // Act: call endpoint with prompt
        val prompt = "Hello model"
        mockMvc.perform(post("/chat/{chatId}/message", chatId.toString()).param("content", prompt))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/chat/${chatId}"))

        // Assert: reload chat and verify two entries added
        val reloaded = chatRepository.findById(chatId!!).orElseThrow()
        assertEquals(2, reloaded.messages.size, "Exactly two messages should be created")
        // Order should be: USER then ASSISTANT
        assertEquals(Role.USER, reloaded.messages[0].role)
        assertEquals(prompt, reloaded.messages[0].content)
        assertEquals(Role.ASSISTANT, reloaded.messages[1].role)
        assertEquals("stubbed answer", reloaded.messages[1].content)
    }

    @Test
    @Transactional
    fun `POST addMessage with nonexistent chatId returns 404`() {
        mockMvc.perform(post("/chat/999/message").param("content", "test"))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun `POST talkToModel with empty prompt handles gracefully`() {
        val chat = chatRepository.save(Chat(title = "Test Chat"))
        mockMvc.perform(post("/chat/${chat.id}/entry").param("prompt", ""))
            .andExpect(status().is4xxClientError)
    }
}
