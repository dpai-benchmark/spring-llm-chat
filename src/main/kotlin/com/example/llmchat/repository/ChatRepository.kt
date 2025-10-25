package com.example.llmchat.repository

import com.example.llmchat.model.Chat
import com.example.llmchat.model.Message
import org.springframework.ai.chat.memory.ChatMemoryRepository
import org.springframework.ai.chat.messages.Message as AiMessage
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRepository : JpaRepository<Chat, Long>, ChatMemoryRepository {

    override fun saveAll(
        conversationId: String,
        messages: List<AiMessage>
    ) {
        val chat = findById(conversationId.toLong()).orElse(null) ?: throw IllegalArgumentException("Chat not found")
        messages.forEach { message ->
            chat.addMessage(Message.fromAiMessage(message))
        }
        save(chat)
    }

    override fun findConversationIds(): List<String> = findAll().map { chat -> chat.id.toString() }


    override fun findByConversationId(conversationId: String): List<AiMessage> {
        return findById(conversationId.toLong()).orElse(null)?.messages?.map { it.toAiMessage() }
            ?: throw IllegalArgumentException("Chat not found")
    }

    override fun deleteByConversationId(conversationId: String) {
    }
}