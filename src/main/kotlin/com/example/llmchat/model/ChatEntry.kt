package com.example.llmchat.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
class ChatEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    val content: String,
    @Enumerated(EnumType.STRING) val role: Role,
    @CreationTimestamp private val createdAt: LocalDateTime? = null
)