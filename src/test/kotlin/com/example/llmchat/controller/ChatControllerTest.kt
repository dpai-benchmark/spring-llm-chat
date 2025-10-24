package com.example.llmchat.controller

import com.example.llmchat.model.Chat
import com.example.llmchat.service.ChatService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.mock
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ChatControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var chatService: ChatService

    @BeforeEach
    fun setUp() {
        chatService = mock(ChatService::class.java)
        mockMvc = MockMvcBuilders.standaloneSetup(ChatController(chatService)).build()
    }

    @Test
    fun `GET root returns chat view with chats list`() {
        val chats = listOf(
            Chat(id = 2L, title = "Second"),
            Chat(id = 1L, title = "First")
        )
        `when`(chatService.getAllChats()).thenReturn(chats)

        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(view().name("chat"))
            .andExpect(model().attributeExists("chats"))
            .andExpect(model().attribute("chats", chats))
    }

    @Test
    fun `GET chat by id returns chat view with chats and chat in model`() {
        val chatId = "42"
        val allChats = listOf(Chat(id = 42L, title = "Test Chat"))
        val currentChat = Chat(id = 42L, title = "Test Chat")

        `when`(chatService.getAllChats()).thenReturn(allChats)
        `when`(chatService.getChat(chatId)).thenReturn(currentChat)

        mockMvc.perform(get("/chat/{chatId}", chatId))
            .andExpect(status().isOk)
            .andExpect(view().name("chat"))
            .andExpect(model().attributeExists("chats"))
            .andExpect(model().attributeExists("chat"))
            .andExpect(model().attribute("chats", allChats))
            .andExpect(model().attribute("chat", currentChat))
    }

    @Test
    fun `POST new chat redirects to created chat`() {
        val title = "New Chat"
        val created = Chat(id = 100L, title = title)
        `when`(chatService.createChat(title)).thenReturn(created)

        mockMvc.perform(post("/chat").param("title", title))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/chat/100"))
    }

    @Test
    fun `DELETE chat redirects to root and invokes service`() {
        val chatId = "7"

        mockMvc.perform(delete("/chat/{chatId}", chatId))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))

        verify(chatService).deleteChat(chatId)
    }

    @Test
    fun `POST talkToModel redirects back to chat and invokes processInteraction`() {
        val chatId = "123"
        val prompt = "Hello model"
        val chat = Chat(id = 123L, title = "Test")
        `when`(chatService.getChat(chatId)).thenReturn(chat)

        mockMvc.perform(post("/chat/{chatId}/entry", chatId).param("prompt", prompt))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/chat/$chatId"))

        verify(chatService).processInteraction(chatId, prompt)
    }

    @Test
    fun `POST talkToModel without prompt returns 400 Bad Request`() {
        val chatId = "123"

        mockMvc.perform(post("/chat/{chatId}/entry", chatId))
            .andExpect(status().isBadRequest)
    }
}
