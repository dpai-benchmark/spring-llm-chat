package com.example.llmchat.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.springframework.ai.chat.messages.Message as AiMessage
import java.time.LocalDateTime

@Entity
class Message(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    val content: String,
    @Enumerated(EnumType.STRING) val role: Role,
    @CreationTimestamp val sentAt: LocalDateTime? = null
) {

    fun toAiMessage(): AiMessage {
        return role.getMessage(content)
    }

    companion object {
        fun fromAiMessage(message: AiMessage): Message {
            return Message(content = message.text,
                role = Role.getRole(message.messageType.name)
            )
        }
    }
}