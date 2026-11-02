package com.nova.a12tester.domain

data class ChatMessage(
    val messageId: String,
    val appId: String,
    val assignmentId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)