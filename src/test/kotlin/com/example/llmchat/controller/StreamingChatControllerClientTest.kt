package com.example.llmchat.controller

import com.example.llmchat.service.ChatService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StreamingChatControllerClientTest {

    @Autowired
    private lateinit var httpClient: HttpClient

    @LocalServerPort
    private var port: Int = 0

    @MockitoBean
    private lateinit var chatService: ChatService

    @Test
    @Timeout(15) // Prevent hanging tests
    fun `should stream chat responses via SSE with proper event parsing`() {
        val chatId = 123L
        val prompt = "Hello, how are you?"
        val expectedMessages = listOf("Hello! ", "I'm doing well, ", "thank you for asking!")

        // Create mock emitter with proper event structure
        val mockEmitter = SseEmitter(30000L)
        given(chatService.processInteractionWithStreaming(eq(chatId.toString()), eq(prompt)))
            .willReturn(mockEmitter)

        // Send events with proper SSE formatting
        val eventLatch = CountDownLatch(1)
        CompletableFuture.runAsync {
            try {
                Thread.sleep(100) // Allow connection to establish
                expectedMessages.forEach { message ->
                    mockEmitter.send(
                        SseEmitter.event()
                            .name("message")
                            .id(System.currentTimeMillis().toString())
                            .data(message)
                    )
                    Thread.sleep(50)
                }
                mockEmitter.send(SseEmitter.event().name("done").data(""))
                mockEmitter.complete()
                eventLatch.countDown()
            } catch (e: Exception) {
                mockEmitter.completeWithError(e)
                eventLatch.countDown()
            }
        }

        // Make request with modern HTTP client
        val url = buildUrl("/chat-stream/$chatId", mapOf("prompt" to prompt))
        val events = collectSSEEvents(url, Duration.ofSeconds(10))

        // Wait for events to be sent
        assertThat(eventLatch.await(5, TimeUnit.SECONDS)).isTrue()

        // Verify events
        val messageEvents = events.filter { it.type == "message" }
        val doneEvents = events.filter { it.type == "done" }

        assertThat(messageEvents).hasSize(3)
        assertThat(doneEvents).hasSize(1)

        messageEvents.forEachIndexed { index, event ->
            assertThat(event.data).isEqualTo(expectedMessages[index])
            assertThat(event.id).isNotNull()
        }
    }

    @Test
    @Timeout(10)
    fun `should handle service errors gracefully`() {
        val chatId = 456L
        val prompt = "Test error"

        val mockEmitter = SseEmitter(30000L)
        given(chatService.processInteractionWithStreaming(eq(chatId.toString()), eq(prompt)))
            .willReturn(mockEmitter)

        // Simulate error after partial data
        CompletableFuture.runAsync {
            try {
                Thread.sleep(100)
                mockEmitter.send(SseEmitter.event().name("message").data("Partial response"))
                Thread.sleep(50)
                mockEmitter.completeWithError(RuntimeException("Service error"))
            } catch (_: Exception) {
                // Expected if emitter already completed
            }
        }

        val url = buildUrl("/chat-stream/$chatId", mapOf("prompt" to prompt))

        // Should either get partial data or error response
        try {
            val events = collectSSEEvents(url, Duration.ofSeconds(5))
            // If we get events, verify we got at least the partial response
            assertThat(events).isNotEmpty()
            assertThat(events.first().data).isEqualTo("Partial response")
        } catch (e: Exception) {
            // Or we might get an exception, which is also acceptable for error cases
            assertThat(e.message).containsIgnoringCase("chunked transfer encoding, state: READING_LENGTH")
        }
    }

    @Test
    @Timeout(5)
    fun `should return 400 for missing prompt parameter`() {
        val chatId = 789L
        val url = buildUrl("/chat-stream/$chatId")

        val request = HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Accept", "text/event-stream")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        assertThat(response.statusCode()).isEqualTo(400)
    }

    @Test
    @Timeout(10)
    fun `should handle empty responses correctly`() {
        val chatId = 100L
        val prompt = "empty"

        val mockEmitter = SseEmitter(30000L)
        given(chatService.processInteractionWithStreaming(eq(chatId.toString()), eq(prompt)))
            .willReturn(mockEmitter)

        // Complete immediately without sending data
        CompletableFuture.runAsync {
            Thread.sleep(100)
            mockEmitter.complete()
        }

        val url = buildUrl("/chat-stream/$chatId", mapOf("prompt" to prompt))
        val events = collectSSEEvents(url, Duration.ofSeconds(5))

        assertThat(events).isEmpty()
    }

    @Test
    @Timeout(5)
    fun `should handle special characters in prompt`() {
        val chatId = 200L
        val prompt = "Hello with émojis 🚀 and special chars: <>&\""

        val mockEmitter = SseEmitter(30000L)
        given(chatService.processInteractionWithStreaming(eq(chatId.toString()), eq(prompt)))
            .willReturn(mockEmitter)

        CompletableFuture.runAsync {
            Thread.sleep(100)
            mockEmitter.send(SseEmitter.event().name("message").data("Response received!"))
            mockEmitter.complete()
        }

        val url = buildUrl("/chat-stream/$chatId", mapOf("prompt" to prompt))
        val events = collectSSEEvents(url, Duration.ofSeconds(5))

        assertThat(events).hasSize(1)
        assertThat(events.first().data).isEqualTo("Response received!")
    }

    @Test
    @Timeout(10)
    fun `should verify mock interactions`() {
        val chatId = 300L
        val prompt = "verification test"

        val mockEmitter = SseEmitter()
        given(chatService.processInteractionWithStreaming(any(), any()))
            .willReturn(mockEmitter)

        CompletableFuture.runAsync {
            Thread.sleep(100)
            mockEmitter.complete()
        }

        val url = buildUrl("/chat-stream/$chatId", mapOf("prompt" to prompt))
        collectSSEEvents(url, Duration.ofSeconds(5))

        // Verify the service was called with correct parameters
        org.mockito.kotlin.verify(chatService).processInteractionWithStreaming(
            eq(chatId.toString()),
            eq(prompt)
        )
    }

    // Helper methods

    private fun buildUrl(path: String, params: Map<String, String> = emptyMap()): String {
        val baseUrl = "http://localhost:$port$path"
        if (params.isEmpty()) return baseUrl

        val queryString = params.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }
        return "$baseUrl?$queryString"
    }

    private fun collectSSEEvents(url: String, timeout: Duration): List<SSEEvent> {
        val request = HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .timeout(timeout)
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw RuntimeException("HTTP ${response.statusCode()}: ${response.body()}")
        }

        val contentType = response.headers().firstValue("Content-Type").orElse("")
        assertThat(contentType).contains("text/event-stream")

        return parseSSEEvents(response.body())
    }

    private fun parseSSEEvents(sseData: String): List<SSEEvent> {
        if (sseData.isBlank()) return emptyList()

        return sseData.split("\n\n")
            .filter { it.isNotBlank() }
            .mapNotNull { parseSSEEvent(it) }
    }

    private fun parseSSEEvent(eventBlock: String): SSEEvent? {
        val lines = eventBlock.split("\n")
        var eventType: String? = null
        var data: String? = null
        var id: String? = null

        for (line in lines) {
            when {
                line.startsWith("event:") -> eventType = line.substringAfter("event:").trim()
                line.startsWith("data:") -> data = line.substringAfter("data:")
                line.startsWith("id:") -> id = line.substringAfter("id:").trim()
            }
        }

        return if (data != null) {
            SSEEvent(eventType ?: "message", data, id)
        } else null
    }

    data class SSEEvent(
        val type: String,
        val data: String,
        val id: String? = null
    )

    @TestConfiguration
    class TestConfig {
        @Bean
        fun httpClient(): HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
    }
}

