package com.example.llmchat.service

import com.example.llmchat.model.Chat
import com.example.llmchat.repository.ChatRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class ChatService(private val chatRepository: ChatRepository) {

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
}