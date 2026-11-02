package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

internal fun TestPilotRepository.addNotification(
    userId: String,
    title: String,
    body: String,
    type: NotificationType,
    deepLink: String
) {
    val notif = AppNotification(
        notificationId = "notif_${System.currentTimeMillis()}_${userId.hashCode()}",
        userId = userId,
        title = title,
        body = body,
        type = type,
        deepLinkRoute = deepLink,
        isRead = false
    )
    _state.update { it.copy(notifications = it.notifications + notif) }
}

fun TestPilotRepository.markNotificationRead(notificationId: String) {
    _state.update { st -> st.copy(notifications = st.notifications.map { if (it.notificationId == notificationId) it.copy(isRead = true) else it }) }
}

fun TestPilotRepository.markAllNotificationsRead() {
    val uid = _state.value.currentUser?.userId ?: return
    _state.update { st -> st.copy(notifications = st.notifications.map { if (it.userId == uid) it.copy(isRead = true) else it }) }
}
