package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

fun TestPilotRepository.reportApp(appId: String, reason: ReportReason, details: String): Boolean {
    val current = _state.value.currentUser ?: return false
    val app = _state.value.apps.firstOrNull { it.appId == appId } ?: return false
    if (details.trim().length < 10) {
        _state.update { it.copy(errorMessage = "Please provide at least 10 characters of detail so moderators can act.") }
        return false
    }
    val report = AbuseReport(
        reportId = "rep_${System.currentTimeMillis()}",
        reporterId = current.userId,
        reporterName = current.displayName,
        targetAppId = appId,
        targetAppName = app.appName,
        reason = reason,
        details = details.trim(),
        status = ReportStatus.OPEN
    )
    _state.update { it.copy(reports = it.reports + report, successMessage = "Report submitted to the moderation queue.", isLoading = false) }
    return true
}
