package com.example.llmchat.service

import com.example.llmchat.controller.ChatEntryResponse
import com.example.llmchat.controller.LlmEmptyResponseException
import com.example.llmchat.model.ChatEntity
import com.example.llmchat.model.Role
import com.example.llmchat.repository.ChatRepository
import com.example.llmchat.service.llm.LlmChatService
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val llmChatService: LlmChatService,
) {

    @Transactional
    fun addEntry(chatId: Long, prompt: String): ChatEntryResponse {
        val chat = chatRepository.findById(chatId)
            .orElseThrow { EntityNotFoundException("Chat with $chatId not found") }

        // Save User message
        val userChatEntity = ChatEntity(
            role = Role.USER,
            content = prompt,
            chat = chat,
        )
        chat.history.add(userChatEntity)
        chatRepository.saveAndFlush(chat)

        // Send request to the LLM Chat Service
        val message = userChatEntity.role.getMessage(prompt)
        try {
            val answer: String = llmChatService.chat(message)
                ?.takeIf { it.isNotBlank() }
                ?: throw LlmEmptyResponseException("LLM returned empty response for chatId = $chatId")

            // Save Assistant message
            val assistantChatEntity = ChatEntity(
                role = Role.ASSISTANT,
                content = answer,
                chat = chat
            )
            chat.history.add(assistantChatEntity)
            chatRepository.save(chat)

            return ChatEntryResponse(
                chatId = chatId,
                answer = answer,
            )
        } catch (e: Throwable) {
            throw LlmEmptyResponseException("LLM returned error: ${e.message}")
        }
    }
}