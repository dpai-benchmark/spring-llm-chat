package com.example.llmchat.controller

import com.example.llmchat.service.ChatService
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus

@Controller
class ChatController(private val chatService: ChatService) {

    @GetMapping("/")
    fun mainPage(model: ModelMap): String {
        model.addAttribute("chats", chatService.getAllChats())
        return "chat"
    }

    @GetMapping("/chat/{chatId}")
    fun showChat(@PathVariable chatId: String, model: ModelMap): String {
        model.addAttribute("chats", chatService.getAllChats())
        val chat = chatService.getChat(chatId) ?: throw IllegalArgumentException("Chat not found")
        model.addAttribute("chat", chat)
        return "chat"
    }

    @PostMapping("/chat")
    fun newChat(@RequestParam @NotBlank(message = "Chat title must not be empty") title: String): String {
        val chat = chatService.createChat(title)
        return "redirect:/chat/${chat.id}"
    }

    @DeleteMapping("/chat/{chatId}")
    fun deleteChat(@PathVariable chatId: String): String {
        chatService.deleteChat(chatId)
        return "redirect:/"
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<String> {
        return ResponseEntity.badRequest().body(ex.message)
    }
}