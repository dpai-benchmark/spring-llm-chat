package com.example.llmchat.controller

import com.example.llmchat.service.ChatService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class StreamingChatController(
    private val chatService: ChatService
) {

    @GetMapping("/chat-stream/{chatId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun talkToModel(@PathVariable chatId: Long, @RequestParam prompt: String): SseEmitter {
        return chatService.processInteractionWithStreaming(chatId.toString(), prompt)
    }
}