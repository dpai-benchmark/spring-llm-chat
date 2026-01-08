package com.example.llmchat.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage

class RoleTest {

    @Test
    fun whenGetMessageIsCalledForUserRoleThenUserMessageIsReturned() {
        val result = Role.USER.getMessage(TEST_PROMPT)
        assertInstanceOf<UserMessage>(result)
    }

    @Test
    fun whenGetMessageIsCalledForAssistantRoleThenAssistantMessageIsReturned() {
        val result = Role.ASSISTANT.getMessage(TEST_PROMPT)
        assertInstanceOf<AssistantMessage>(result)
    }

    @Test
    fun whenGetMessageIsCalledForSystemRoleThenSystemMessageIsReturned() {
        val result = Role.SYSTEM.getMessage(TEST_PROMPT)
        assertInstanceOf<SystemMessage>(result)
    }

    private companion object {
        const val TEST_PROMPT = "test_prompt"
    }
}