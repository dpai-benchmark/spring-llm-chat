package com.example.llmchat.service

import com.example.llmchat.model.Chat
import com.example.llmchat.model.ChatEntry
import com.example.llmchat.model.Role
import com.example.llmchat.repository.ChatRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(private val chatRepository: ChatRepository, private val chatClient: ChatClient) {

    fun getAllChats(): List<Chat> {
        return chatRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
    }

    fun getChat(chatId: String): Chat? {
        return chatRepository.findById(chatId.toLong()).orElse(null)
    }

    fun createChat(title: String): Chat {
        return chatRepository.save(Chat(title = title))
    }

    fun deleteChat(chatId: String) {
        chatRepository.deleteById(chatId.toLong())
    }

    @Transactional
    fun processInteraction(chatId: String, prompt: String) {
        addChatEntry(chatId, prompt, Role.USER)
        val answer = chatClient.prompt().user(prompt).call().content() ?: "..."
        addChatEntry(chatId, answer, Role.ASSISTANT)
    }

    @Transactional
    fun addChatEntry(chatId: String, prompt: String, user: Role) {
        val chat = getChat(chatId) ?: throw IllegalArgumentException("Chat not found")
        chat.addEntry(ChatEntry(content = prompt, role = user))
        chatRepository.save(chat)
    }
}