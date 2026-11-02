package com.nova.a12tester.data

import com.nova.a12tester.domain.ChatMessage
import com.nova.a12tester.domain.NotificationType
import kotlinx.coroutines.flow.update

fun TestPilotRepository.sendMessage(
    appId: String,
    assignmentId: String,
    receiverId: String,
    text: String
): Boolean {
    val current = _state.value.currentUser ?: return false
    if (text.isBlank()) return false
    
    val msgId = "msg_${System.currentTimeMillis()}_${current.userId}"
    val message = ChatMessage(
        messageId = msgId,
        appId = appId,
        assignmentId = assignmentId,
        senderId = current.userId,
        receiverId = receiverId,
        text = text.trim()
    )
    
    _state.update { st ->
        st.copy(chatMessages = st.chatMessages + message)
    }
    
    val deepLinkRoute = "chat/$appId/$assignmentId/${current.userId}"

    addNotification(
        userId = receiverId,
        title = "New Message from ${current.displayName}",
        body = text.take(50) + if (text.length > 50) "..." else "",
        type = NotificationType.SYSTEM,
        deepLink = deepLinkRoute
    )
    
    return true
}
