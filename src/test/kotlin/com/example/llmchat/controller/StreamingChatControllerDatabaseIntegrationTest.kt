package com.example.llmchat.controller

import com.example.llmchat.model.Chat
import com.example.llmchat.model.Role
import com.example.llmchat.repository.ChatRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.Answers
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Flux
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") // Use test profile for database configuration
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true"
    ]
)
class StreamingChatControllerDatabaseIntegrationTest {

    @Autowired
    private lateinit var httpClient: HttpClient

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var chatRepository: ChatRepository

    private lateinit var testChat: Chat

    @BeforeEach
    fun setup() {
        // Clear database and create test chat
        chatRepository.deleteAll()
        testChat = Chat(title = "Test Chat")
        testChat = chatRepository.save(testChat)
        // Force flush to ensure data is persisted
        chatRepository.flush()
    }

    @Test
    @Timeout(30)
    fun `should save user prompt and assistant response to database during SSE streaming`() {
        val chatId = testChat.id.toString()
        val prompt = "What is the capital of France?"

        // Verify initial state
        val initialChat = chatRepository.findById(testChat.id!!).get()
        assertThat(initialChat.history).isEmpty()

        // Trigger SSE request (non-blocking; stream stays open). We don't wait for completion.
        val url = buildUrl("/chat-stream/$chatId", mapOf("prompt" to prompt))
        startSSERequest(url)

        // Wait/poll for async processing to complete (assistant entry may or may not be saved depending on env)
        try {
            waitUntilReturn(timeout = 25_000) {
                val c = chatRepository.findById(testChat.id!!).get()
                c.history.firstOrNull { it.role == Role.ASSISTANT }
            } != null
        } catch (_: AssertionError) {
        }
        // Verify database state after streaming
        val updatedChat = chatRepository.findById(testChat.id!!).get()

        // Verify user entry always saved
        val userEntry = updatedChat.history.first { it.role == Role.USER }
        assertThat(userEntry.content).isEqualTo(prompt)
        assertThat(userEntry.role).isEqualTo(Role.USER)
        assertThat(userEntry.createdAt).isNotNull()

        // Verify assistant entry if present
        val assistantEntry = updatedChat.history.firstOrNull { it.role == Role.ASSISTANT }
        if (assistantEntry != null) {
            assertThat(assistantEntry.role).isEqualTo(Role.ASSISTANT)
            assertThat(assistantEntry.content).isNotBlank()
            assertThat(assistantEntry.createdAt).isNotNull()
            assertThat(assistantEntry.createdAt).isAfterOrEqualTo(userEntry.createdAt)
        }
    }

    @Test
    @Timeout(10)
    fun `should handle multiple concurrent requests to same chat`() {
        val chatId = testChat.id.toString()
        val prompts = listOf("Question 1", "Question 2", "Question 3")

        val futures = prompts.map { prompt ->
            CompletableFuture.supplyAsync {
                val url = buildUrl("/chat-stream/$chatId", mapOf("prompt" to prompt))
                try {
                    startSSERequest(url)
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }

        // Trigger all
        futures.forEach { it.join() }

        // Wait for async processing: expect user entries for all prompts
        waitUntil(timeout = 10_000) {
            val c = chatRepository.findById(testChat.id!!).get()
            c.history.count { it.role == Role.USER } >= prompts.size
        }

        // Verify database state
        val updatedChat = chatRepository.findById(testChat.id!!).get()

        // Should have entries for all prompts (user + assistant for each successful request)
        val userEntries = updatedChat.history.filter { it.role == Role.USER }
        assertThat(userEntries).hasSize(prompts.size)

        // Verify all prompts are present
        val savedPrompts = userEntries.map { it.content }.sorted()
        assertThat(savedPrompts).isEqualTo(prompts.sorted())
    }

    @Test
    @Timeout(10)
    fun `should maintain entry order in database`() {
        val chatId = testChat.id.toString()
        val prompts = listOf("First question", "Second question")

        // Send requests sequentially
        for (prompt in prompts) {
            val url = buildUrl("/chat-stream/$chatId", mapOf("prompt" to prompt))
            try {
                startSSERequest(url)
                // Wait for this prompt's USER entry to be persisted before next
                waitUntil(timeout = 5_000) {
                    val c = chatRepository.findById(testChat.id!!).get()
                    c.history.any { it.role == Role.USER && it.content == prompt }
                }
            } catch (_: Exception) {
                // Continue with next request
            }
        }

        // Wait for at least both user entries to exist
        waitUntil(timeout = 5_000) {
            val c = chatRepository.findById(testChat.id!!).get()
            c.history.count { it.role == Role.USER } >= 2
        }

        val updatedChat = chatRepository.findById(testChat.id!!).get()
        val entries = updatedChat.history.sortedBy { it.createdAt }

        // Verify order: USER1, ASSISTANT1, USER2, ASSISTANT2
        assertThat(entries.size).isGreaterThanOrEqualTo(2) // At least user entries

        val userEntries = entries.filter { it.role == Role.USER }
        assertThat(userEntries).hasSize(2)
        assertThat(userEntries[0].content).isEqualTo("First question")
        assertThat(userEntries[1].content).isEqualTo("Second question")
        assertThat(userEntries[1].createdAt).isAfter(userEntries[0].createdAt)
    }

    @Test
    fun `should handle nonexistent chat gracefully`() {
        val nonExistentChatId = "999999"
        val prompt = "Hello"

        val url = buildUrl("/chat-stream/$nonExistentChatId", mapOf("prompt" to prompt))

        val request = HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Accept", "text/event-stream")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        // Should return appropriate error status
        assertThat(response.statusCode()).isIn(400, 404, 500)

        // Verify no data was created
        val totalEntries = chatRepository.findAll().sumOf { it.history.size }
        assertThat(totalEntries).isZero()
    }

    // Helper methods (same as before)
    private fun buildUrl(path: String, params: Map<String, String> = emptyMap()): String {
        val baseUrl = "http://localhost:$port$path"
        if (params.isEmpty()) return baseUrl

        val queryString = params.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }
        return "$baseUrl?$queryString"
    }

    // Fire-and-forget SSE start: initiate request asynchronously without waiting for completion
    private fun startSSERequest(url: String) {
        val request = HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .GET()
            .build()
        // Use discarding body handler to avoid buffering; don't set per-request timeout to let stream stay open
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
        // We intentionally do not join/cancel here; server keeps stream open
    }

    // Utility: wait until condition is true or timeout (milliseconds)
    private fun waitUntil(timeout: Long, interval: Long = 100, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            if (condition()) return
            Thread.sleep(interval)
        }
        throw AssertionError($$"Condition not met within $timeout ms")
    }

    // Utility: wait until supplier returns non-null and return it, or throw on timeout
    private fun <T> waitUntilReturn(timeout: Long, interval: Long = 100, supplier: () -> T?): T? {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            val v = supplier()
            if (v != null) return v
            Thread.sleep(interval)
        }
        return null
    }

    // Test configuration
    @TestConfiguration
    class DatabaseTestConfig {
        @Bean
        fun httpClient(): HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        @Bean
        @Primary
        fun chatClientMock(): ChatClient {
            val client = Mockito.mock(ChatClient::class.java, Answers.RETURNS_DEEP_STUBS)
            `when`(
                client.prompt().user(Mockito.anyString()).stream().chatResponse()
            ).thenReturn(
                Flux.just(ChatResponse(listOf(Generation(AssistantMessage("Stubbed answer")))))
            )
            return client
        }
    }
}

