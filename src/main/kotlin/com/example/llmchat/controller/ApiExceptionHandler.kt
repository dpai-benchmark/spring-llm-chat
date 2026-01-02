package com.example.llmchat.controller

import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(LlmEmptyResponseException::class)
    fun handleLlmEmptyResponse(ex: LlmEmptyResponseException): ResponseEntity<ApiError> {
        val error = ApiError(
            code = "LLM_EMPTY_RESPONSE",
            message = ex.message ?: "LLM returned empty response"
        )
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error)
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(ex: EntityNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiError(code = "NOT_FOUND", message = ex.message ?: "Not found"))
}

data class ApiError(
    val code: String,
    val message: String
)