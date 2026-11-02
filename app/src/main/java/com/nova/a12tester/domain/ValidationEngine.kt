package com.nova.a12tester.domain

import java.net.URI

/**
 * Client + shared validation engine mirroring backend Cloud Functions validation.
 * Enforces strict URL patterns, package names, anti-spam checks, and Google Play policy rules.
 */
object ValidationEngine {

    private val PACKAGE_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")
    private val PROHIBITED_KEYWORDS = listOf(
        "5 star review",
        "five star review",
        "buy reviews",
        "fake review",
        "guaranteed approval",
        "click ads",
        "click every ad",
        "password"
    )

    data class SubmissionValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val extractedPackageName: String? = null
    )

    fun validateAppSubmission(
        appName: String,
        developerName: String,
        shortDescription: String,
        packageName: String,
        iconUrl: String,
        optInUrl: String,
        googleGroupUrl: String,
        privacyPolicyUrl: String,
        minAndroidVersion: Int,
        testingInstructions: String,
        testingDurationDays: Int
    ): SubmissionValidationResult {
        val errors = mutableListOf<String>()

        val cleanName = appName.trim()
        if (cleanName.length !in 3..60) {
            errors.add("App name must be between 3 and 60 characters.")
        }

        val cleanDev = developerName.trim()
        if (cleanDev.length !in 2..60) {
            errors.add("Developer name must be between 2 and 60 characters.")
        }

        val cleanDesc = shortDescription.trim()
        if (cleanDesc.length !in 15..300) {
            errors.add("Short description must be between 15 and 300 characters.")
        }

        val cleanPkg = packageName.trim()
        if (!PACKAGE_REGEX.matches(cleanPkg)) {
            errors.add("Package name must be a valid Android application ID (e.g., com.company.app).")
        }

        if (!isValidGooglePlayOptInUrl(optInUrl.trim(), cleanPkg)) {
            errors.add(
                "Google Play closed-testing URL must be an HTTPS Play Store testing link " +
                    "(https://play.google.com/apps/testing/$cleanPkg or https://play.google.com/store/apps/details?id=$cleanPkg)."
            )
        }

        val cleanGroupUrl = cleanGoogleGroupUrl(googleGroupUrl)
        if (!isValidGoogleGroupUrl(cleanGroupUrl)) {
            errors.add("Google Groups URL must be a valid HTTPS link on groups.google.com (e.g., https://groups.google.com/g/your-testers).")
        }

        if (iconUrl.isNotBlank() && !isValidHttpsUrl(iconUrl.trim())) {
            errors.add("App icon URL must use HTTPS.")
        }

        if (privacyPolicyUrl.isNotBlank() && !isValidHttpsUrl(privacyPolicyUrl.trim())) {
            errors.add("Privacy policy URL must be a valid HTTPS URL.")
        }

        if (minAndroidVersion !in 21..37) {
            errors.add("Required Android SDK version must be between API 21 (Android 5.0) and API 37.")
        }

        val cleanInstructions = testingInstructions.trim()
        if (cleanInstructions.length < 20) {
            errors.add("Testing instructions must be at least 20 characters so testers know what features to test.")
        }

        if (testingDurationDays !in 14..30) {
            errors.add("Closed testing duration must be between 14 and 30 days.")
        }

        val combinedText = "$cleanName $cleanDesc $cleanInstructions".lowercase()
        val matchedViolation = PROHIBITED_KEYWORDS.firstOrNull { combinedText.contains(it) }
        if (matchedViolation != null) {
            errors.add(
                "Submission contains policy-restricted phrase (\"$matchedViolation\"). " +
                    "Incentivizing Play Store ratings, fake reviews, or ad clicks is strictly prohibited."
            )
        }

        return SubmissionValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            extractedPackageName = cleanPkg.takeIf { PACKAGE_REGEX.matches(it) }
        )
    }

    fun cleanGoogleGroupUrl(url: String): String {
        var trimmed = url.trim()
        if (trimmed.isBlank()) return trimmed
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed = "https://$trimmed"
        }
        return try {
            val uri = URI(trimmed)
            val host = uri.host?.lowercase() ?: return trimmed
            if (!host.endsWith("groups.google.com")) return trimmed

            // Handles /g/<name>, /u/<n>/g/<name>, /a/<domain>/g/<name>, /d/forum/<name>, /forum/#!forum/<name>
            val groupRegex = Regex(
                "groups\\.google\\.com/(?:u/\\d+/|a/[^/]+/)?(?:g/|d/forum/|forum/#\\!forum/|forum/)?([a-zA-Z0-9._-]+)",
                RegexOption.IGNORE_CASE
            )
            val match = groupRegex.find(trimmed)
            val groupName = match?.groupValues?.getOrNull(1)

            if (!groupName.isNullOrBlank() && groupName.lowercase() !in listOf("forum", "g", "u", "a", "d")) {
                "https://groups.google.com/g/$groupName"
            } else {
                trimmed
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    fun isValidGooglePlayOptInUrl(url: String, expectedPackage: String? = null): Boolean {
        var trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed = "https://$trimmed"
        }
        if (!isValidHttpsUrl(trimmed)) return false
        return try {
            val uri = URI(trimmed)
            val host = uri.host?.lowercase() ?: return false
            if (host != "play.google.com") return false
            val path = uri.path ?: ""
            val query = uri.query ?: ""

            val isTestingPath = path.startsWith("/apps/testing/") && path.removePrefix("/apps/testing/").isNotBlank()
            val isDetailsPath = (path == "/store/apps/details" || path == "/store/apps/details/") && query.contains("id=")

            if (!isTestingPath && !isDetailsPath) return false

            val cleanPkg = expectedPackage?.trim()
            if (!cleanPkg.isNullOrBlank()) {
                val matchesTestingPkg = path.trimEnd('/').removePrefix("/apps/testing/").equals(cleanPkg, ignoreCase = true)
                val matchesStorePkg = query.split("&").any { param ->
                    val key = param.substringBefore("=").trim()
                    val value = param.substringAfter("=", "").trim()
                    key.equals("id", ignoreCase = true) && value.equals(cleanPkg, ignoreCase = true)
                }
                matchesTestingPkg || matchesStorePkg
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isValidGoogleGroupUrl(url: String): Boolean {
        val cleaned = cleanGoogleGroupUrl(url)
        if (!isValidHttpsUrl(cleaned)) return false
        return try {
            val uri = URI(cleaned)
            val host = uri.host?.lowercase() ?: return false
            val path = uri.path ?: ""
            host == "groups.google.com" &&
                path.startsWith("/g/") &&
                path.removePrefix("/g/").trim('/').length >= 2
        } catch (_: Exception) {
            false
        }
    }

    fun isValidHttpsUrl(url: String): Boolean {
        return try {
            val uri = URI(url.trim())
            uri.scheme?.lowercase() == "https" && !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
