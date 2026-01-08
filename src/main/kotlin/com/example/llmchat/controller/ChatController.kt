package com.example.llmchat.controller

import com.example.llmchat.service.ChatService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(exposedHeaders = ["errors, content-type"])
@RequestMapping("/chat")
class ChatController(private val chatService: ChatService) {

    @PostMapping("/{id}/entry")
    fun addEntry(
        @PathVariable id: Long,
        @RequestParam prompt: String
    ): ResponseEntity<ChatEntryResponse> {
        val response = chatService.addEntry(id, prompt)
        return ResponseEntity.ok(response)
    }
}