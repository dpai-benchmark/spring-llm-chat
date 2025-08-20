package com.example.llmchat.model

import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage

enum class Role(val role: String) {
    USER("user") {
        override fun getMessage(prompt: String): Message {
            return UserMessage(prompt)
        }
    },
    ASSISTANT("assistant") {
        override fun getMessage(prompt: String): Message {
            return AssistantMessage(prompt)
        }
    },
    SYSTEM("system") {
        override fun getMessage(prompt: String): Message {
            return SystemMessage(prompt)
        }
    };

    abstract fun getMessage(prompt: String): Message

    companion object {
        fun getRole(roleName: String): Role? {
            return entries.firstOrNull { role: Role -> role.name == roleName }
        }
    }
}