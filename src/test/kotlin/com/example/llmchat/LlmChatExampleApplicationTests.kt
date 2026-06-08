package com.example.llmchat

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
class LlmChatExampleApplicationTests {

	@Test
	fun contextLoads() {
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
