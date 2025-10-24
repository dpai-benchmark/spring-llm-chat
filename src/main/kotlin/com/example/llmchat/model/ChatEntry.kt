package com.example.llmchat.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.springframework.ai.chat.messages.Message
import java.time.LocalDateTime

@Entity
@Table(name = "chat_entries")
class ChatEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    val content: String,
    @Enumerated(EnumType.STRING) val role: Role,
    @CreationTimestamp val createdAt: LocalDateTime? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    val chat: Chat? = null
) {

    fun toMessage(): Message {
        return role.getMessage(content)
    }

    companion object {
        fun fromMessage(message: Message, chat: Chat? = null): ChatEntry {
            return ChatEntry(content = message.text,
                role = Role.getRole(message.messageType.name),
                chat = chat
            )
        }
    }
}