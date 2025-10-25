package com.example.llmchat.repository

import com.example.llmchat.model.Message
import org.springframework.data.jpa.repository.JpaRepository

interface MessageRepository : JpaRepository<Message, Long>
