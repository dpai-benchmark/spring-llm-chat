package com.example.llmchat.repository

import com.example.llmchat.model.Chat
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRepository: JpaRepository<Chat, Long>