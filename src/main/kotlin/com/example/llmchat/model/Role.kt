package com.example.llmchat.model

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage

enum class Role(private val displayName: String) {

    USER("user") {
        override fun getMessage(prompt: String): Message = UserMessage(prompt)
    },
    ASSISTANT("assistant") {
        override fun getMessage(prompt: String): Message = AssistantMessage(prompt)
    },
    SYSTEM("system") {
        override fun getMessage(prompt: String): Message = SystemMessage(prompt)
    };

    @JsonIgnore
    abstract fun getMessage(prompt: String): Message
}