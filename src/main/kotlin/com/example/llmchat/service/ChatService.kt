package com.example.llmchat.service

import com.example.llmchat.model.Chat
import com.example.llmchat.model.ChatEntry
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
        val id = chatId.toLongOrNull() ?: throw IllegalArgumentException("Invalid chat ID format: $chatId")
        return chatRepository.findById(id).orElse(null)
    }

    fun createChat(title: String): Chat {
        return chatRepository.save(Chat(title = title))
    }

    fun deleteChat(chatId: String) {
        val id = chatId.toLongOrNull() ?: throw IllegalArgumentException("Invalid chat ID format: $chatId")
        chatRepository.deleteById(id)
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
        chat.addEntry(ChatEntry(content = prompt, role = user, chat = chat))
        chatRepository.save(chat)
    }

    @Transactional
    fun processInteractionWithStreaming(chatId: String, prompt: String): SseEmitter {
        val emitter = SseEmitter(0L)

        chatClient.prompt()
            .advisors {
                advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId)
            }
            .user(prompt)
            .stream().chatResponse()
            .doOnNext { response ->
                val token = response.result.output
                emitter.send(token)
            }
            .doOnComplete {
                emitter.complete()
            }
            .doOnError { error ->
                emitter.completeWithError(error)
            }
            .subscribe()

        return emitter
    }
}