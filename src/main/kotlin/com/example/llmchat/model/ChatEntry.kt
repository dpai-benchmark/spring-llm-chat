package com.example.llmchat.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.springframework.ai.chat.messages.Message
import java.time.LocalDateTime

@Entity
class ChatEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    val content: String,
    @Enumerated(EnumType.STRING) val role: Role,
    @CreationTimestamp val createdAt: LocalDateTime? = null
) {

    fun toMessage(): Message {
        return role.getMessage(content)
    }

    companion object {
        fun fromMessage(message: Message): ChatEntry {
            return ChatEntry(content = message.text,
                role = Role.getRole(message.messageType.name)
            )
        }
    }
}