package com.example.llmchat.repository

import com.example.llmchat.model.Chat
import com.example.llmchat.model.ChatEntry
import org.springframework.ai.chat.memory.ChatMemoryRepository
import org.springframework.ai.chat.messages.Message
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRepository : JpaRepository<Chat, Long>, ChatMemoryRepository {

    override fun saveAll(
        conversationId: String,
        messages: List<Message>
    ) {
        val chat = findById(conversationId.toLong()).orElse(null) ?: throw IllegalArgumentException("Chat not found")
        messages.forEach { message ->
            chat.addEntry(ChatEntry.fromMessage(message))
        }
        save(chat)
    }

    override fun findConversationIds(): List<String> = findAll().map { chat -> chat.id.toString() }


    override fun findByConversationId(conversationId: String): List<Message> {
        return findById(conversationId.toLong()).orElse(null)?.history?.map { it.toMessage() }
            ?: throw IllegalArgumentException("Chat not found")
    }

    override fun deleteByConversationId(conversationId: String) {
    }
}