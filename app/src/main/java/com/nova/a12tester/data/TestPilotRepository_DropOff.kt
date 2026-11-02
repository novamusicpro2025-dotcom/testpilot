package com.nova.a12tester.data

import com.nova.a12tester.domain.AssignmentStatus
import com.nova.a12tester.domain.NotificationType
import kotlinx.coroutines.flow.update

/**
 * Ensures testers are actively participating.
 * Rule: If a tester has missed a day, send a reminder.
 * If a tester has missed 3 days, abandon the assignment, decrement assigned count, 
 * deduct reputation, and allow a new tester to join.
 */
fun TestPilotRepository.enforceTesterDropOff() {
    val now = System.currentTimeMillis()
    val dayMillis = 24L * 3600 * 1000L
    
    val activeAssignments = _state.value.assignments.filter { 
        it.status == AssignmentStatus.IN_PROGRESS || it.status == AssignmentStatus.ASSIGNED 
    }
    
    for (assignment in activeAssignments) {
        val lastCheckIn = assignment.lastCheckInEpochMs
        
        // Skip brand new assignments that are less than 1 day old
        if (lastCheckIn == 0L && (now - assignment.assignedAtEpochMs) < dayMillis) continue
        
        val timeSinceLastAction = if (lastCheckIn == 0L) (now - assignment.assignedAtEpochMs) else (now - lastCheckIn)
        val daysMissed = timeSinceLastAction / dayMillis
        
        if (daysMissed == 1L) {
            // Missed 1 day -> Send Reminder
            addNotification(
                userId = assignment.testerId,
                title = "⚠️ Missed Daily Test: ${assignment.appName}",
                body = "You missed testing ${assignment.appName} yesterday. Open it for 2 minutes to keep your daily streak and earn coins.",
                type = NotificationType.SYSTEM,
                deepLink = "testing_progress/${assignment.assignmentId}"
            )
        } else if (daysMissed >= 3L) {
            // Missed 3 days -> Abandon assignment and replace
            _state.update { st ->
                st.copy(
                    assignments = st.assignments.map {
                        if (it.assignmentId == assignment.assignmentId) {
                            it.copy(status = AssignmentStatus.ABANDONED)
                        } else it
                    },
                    apps = st.apps.map {
                        if (it.appId == assignment.appId) {
                            it.copy(assignedTesterCount = (it.assignedTesterCount - 1).coerceAtLeast(0))
                        } else it
                    },
                    users = st.users.map {
                        if (it.userId == assignment.testerId) {
                            it.copy(reputationScore = (it.reputationScore - 15).coerceAtLeast(0))
                        } else it
                    }
                )
            }
            
            addNotification(
                userId = assignment.testerId,
                title = "❌ Assignment Abandoned: ${assignment.appName}",
                body = "You missed 3 days of testing for ${assignment.appName}. Your assignment was cancelled and reputation decreased by 15.",
                type = NotificationType.SYSTEM,
                deepLink = "active_assignments"
            )
            
            addNotification(
                userId = assignment.developerId,
                title = "🔄 Tester Replaced",
                body = "A tester for ${assignment.appName} was inactive for 3 days and has been removed. A new tester slot is now open.",
                type = NotificationType.SYSTEM,
                deepLink = "my_apps"
            )
        }
    }
}