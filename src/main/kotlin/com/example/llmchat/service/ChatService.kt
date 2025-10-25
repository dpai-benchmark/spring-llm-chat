package com.example.llmchat.service

import com.example.llmchat.model.Chat
import com.example.llmchat.model.Message
import com.example.llmchat.model.Role
import com.example.llmchat.repository.ChatRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val chatClient: ChatClient,
) {

    fun getAllChats(): List<Chat> {
        return chatRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
    }

    fun getChat(chatId: String): Chat? {
        return try {
            chatRepository.findById(chatId.toLong()).orElse(null)
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun createChat(title: String): Chat {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            throw IllegalArgumentException("Chat title cannot be empty")
        }
        return chatRepository.save(Chat(title = trimmedTitle))
    }

    fun deleteChat(chatId: String) {
        try {
            chatRepository.deleteById(chatId.toLong())
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid chat ID format")
        }
    }

    @Transactional
    fun processInteraction(chatId: String, prompt: String) {
        addMessage(chatId, prompt, Role.USER)
        val answer = chatClient.prompt().user(prompt).call().content() ?: "..."
        addMessage(chatId, answer, Role.ASSISTANT)
    }

    @Transactional
    fun addMessage(chatId: String, content: String, role: Role) {
        val chat = getChat(chatId) ?: throw IllegalArgumentException("Chat not found")
        chat.addMessage(Message(content = content, role = role))
        chatRepository.save(chat)
    }

    @Transactional
    fun processInteractionWithStreaming(chatId: String, prompt: String): SseEmitter {
        val emitter = SseEmitter(0L)
        val answer = StringBuilder()

        // Save user prompt to database
        addMessage(chatId, prompt, Role.USER)

        chatClient.prompt()
            .advisors {
                advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId)
            }
            .user(prompt)
            .stream().chatResponse().subscribe(
                { response ->
                    val token = response.result.output
                    emitter.send(token)
                    answer.append(token.text)
                },
                { error -> 
                    emitter.completeWithError(error)
                },
                { 
                    // On completion, save the complete assistant response to database
                    if (answer.isNotEmpty()) {
                        addMessage(chatId, answer.toString(), Role.ASSISTANT)
                    }
                    emitter.complete()
                }
            )

        return emitter
    }
}