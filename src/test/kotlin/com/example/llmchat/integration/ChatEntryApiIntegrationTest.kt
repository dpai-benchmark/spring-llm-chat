package com.example.llmchat.integration

import com.example.llmchat.LlmChatExampleApplication
import com.example.llmchat.model.Role
import com.example.llmchat.repository.ChatRepository
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.jdbc.Sql
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@Testcontainers
@SpringBootTest(
    classes = [LlmChatExampleApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ChatEntryApiIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var chatRepository: ChatRepository

    @LocalServerPort
    var port: Int = 0

    @BeforeEach
    fun setup() {
        wiremock.resetAll()
        val body = """
          {
            "model": "gemma3:4b-it-q4_K_M",
            "created_at": "${Instant.now()}",
            "message": { "role": "assistant", "content": "$ANSWER" },
            "done": true
          }
        """.trimIndent()

        wiremock.stubFor(
            post(urlEqualTo("/api/chat"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun postJson(url: String): ResponseEntity<String> {
        val headers = HttpHeaders().apply { accept = listOf(MediaType.APPLICATION_JSON) }
        return restTemplate.postForEntity(url, HttpEntity<Void>(headers), String::class.java)
    }

    @Sql("/seed-chat.sql")
    @Test
    fun `when correct data passed then service returns 200`() {
        val prompt = "Hi"
        val url = "http://localhost:$port/chat/1/entry?prompt=$prompt"
        val response = postJson(url)
        assertEquals(200, response.statusCode.value())
        val json = response.body
        assertNotNull(json)

        val chatId = JsonPath.read<Number>(json, "$.chatId").toLong()
        assertEquals(1, chatId)

        val answer = JsonPath.read<String>(json, "$.answer")
        assertTrue(answer.contains(ANSWER))

        wiremock.verify(postRequestedFor(urlEqualTo("/api/chat")))

        // Find the messages we just added (last 2 messages)
        val chat = chatRepository.findById(chatId).orElseThrow()
        assertTrue(chat.history.size >= 2, "Should have at least USER and ASSISTANT messages")
        val userMessage = chat.history.findLast { it.role == Role.USER && it.content == prompt }
        val assistantMessage = chat.history.findLast { it.role == Role.ASSISTANT && it.content.contains(ANSWER) }

        assertNotNull(userMessage, "USER message should be saved to database")
        assertNotNull(assistantMessage, "ASSISTANT message should be saved to database")
        assertEquals(prompt, userMessage.content, "USER message content should match prompt")
        assertTrue(assistantMessage.content.contains(ANSWER), "ASSISTANT message should contain LLM response")
    }

    @Sql("/seed-chat.sql")
    @Test
    fun `when prompt is missing then 400 error is returned`() {
        val url = "http://localhost:$port/chat/1/entry"

        val response = postJson(url)

        assertEquals(400, response.statusCode.value())
        val json = response.body
        assertNotNull(json)

        val status = JsonPath.read<Number>(json, "$.status").toInt()
        assertEquals(400, status)

        val error = JsonPath.read<String>(json, "$.error")
        assertTrue(error.contains("Bad Request"))

        val message = JsonPath.read<String>(json, "$.message")
        assertTrue(message.contains("prompt"))
    }

    @Test
    fun `when unknown chat id is passed then 404 error is returned`() {
        val url = "http://localhost:$port/chat/999/entry?prompt=hi"

        val response = postJson(url)

        assertEquals(404, response.statusCode.value())
        val json = response.body
        assertNotNull(json)
    }

    @Sql("/seed-chat.sql")
    @Test
    fun `when LLM returns empty response then 502 error is returned`() {
        wiremock.stubFor(
            post(urlEqualTo("/api/chat"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")
                )
        )

        val url = "http://localhost:$port/chat/1/entry?prompt=hi"
        val response = postJson(url)

        assertEquals(502, response.statusCode.value())

        val json = response.body
        assertNotNull(json)

        wiremock.verify(postRequestedFor(urlEqualTo("/api/chat")))
    }

    @Sql("/seed-chat.sql")
    @Test
    fun `when external LLM fails then 502 error is returned`() {
        wiremock.stubFor(
            post(urlEqualTo("/api/chat"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                )
        )

        val url = "http://localhost:$port/chat/1/entry?prompt=hi"
        val response = postJson(url)

        assertEquals(502, response.statusCode.value())

        val json = response.body
        assertNotNull(json)

        wiremock.verify(postRequestedFor(urlEqualTo("/api/chat")))
    }

    companion object {
        const val ANSWER = "Hello world!"

        private val wiremock = WireMockServer(wireMockConfig().dynamicPort())

        @Container
        @JvmField
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            if (!wiremock.isRunning) wiremock.start()

            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)

            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }

            registry.add("spring.ai.ollama.base-url") { wiremock.baseUrl() }
            registry.add("spring.ai.model.chat") { "ollama" }
            registry.add("spring.ai.openai.api-key") { "dummy" }

            registry.add("spring.ai.retry.max-attempts") { "1" }
            registry.add("spring.ai.retry.backoff.initial-interval") { "1" }
            registry.add("spring.ai.retry.backoff.max-interval") { "3" }
            registry.add("server.error.include-message") { "always" }
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            wiremock.stop()
        }
    }
}
