package com.example.llmchat.controller

import com.example.llmchat.service.ChatService
import jakarta.validation.ConstraintViolationException
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@Validated
class ChatController(private val chatService: ChatService) {

    @GetMapping("/")
    fun mainPage(model: ModelMap): String {
        model.addAttribute("chats", chatService.getAllChats())
        return "chat"
    }

    @GetMapping("/chat/{chatId}")
    fun showChat(@PathVariable chatId: String, model: ModelMap): String {
        model.addAttribute("chats", chatService.getAllChats())
        model.addAttribute("chat", chatService.getChat(chatId))
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

    @PostMapping("/chat/{chatId}/entry")
    fun talkToModel(
        @PathVariable chatId: String,
        @RequestParam @NotBlank(message = "Prompt must not be empty") prompt: String
    ): String {
        chatService.processInteraction(chatId, prompt)
        return "redirect:/chat/$chatId"
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<Void> {
        return if (ex.message == "Chat not found") {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ex.constraintViolations.joinToString { it.message })
}