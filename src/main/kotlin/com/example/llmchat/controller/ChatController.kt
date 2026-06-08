package com.example.llmchat.controller

import com.example.llmchat.service.ChatService
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

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
        model.addAttribute("chat", chatService.getChat(chatId))
        return "chat"
    }

    @PostMapping("/chat")
    fun newChat(@RequestParam title: String): String {
        val chat = chatService.createChat(title)
        return "redirect:/chat/${chat.id}"
    }

    @DeleteMapping("/chat/{chatId}")
    fun deleteChat(@PathVariable chatId: String): String {
        chatService.deleteChat(chatId)
        return "redirect:/"
    }
}