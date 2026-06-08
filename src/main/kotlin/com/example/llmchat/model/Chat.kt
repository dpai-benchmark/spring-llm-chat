package com.example.llmchat.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "chat")
class Chat (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "title", nullable = false)
    var title: String = "",

    @Column(name = "createdAt", updatable = false, nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "history")
    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "chat", fetch = FetchType.EAGER)
    var history: MutableList<ChatEntity> = mutableListOf(),
)