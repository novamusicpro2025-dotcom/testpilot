package com.nova.a12tester

import com.nova.a12tester.domain.ValidationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationEngineTest {

    @Test
    fun testGoogleGroupUrlValidationAndCleaning() {
        // Multi-account Google Group URL from user's browser
        val multiAccountUrl = "https://groups.google.com/u/2/g/pclink"
        assertTrue(ValidationEngine.isValidGoogleGroupUrl(multiAccountUrl))
        assertEquals("https://groups.google.com/g/pclink", ValidationEngine.cleanGoogleGroupUrl(multiAccountUrl))

        // Standard direct link
        val directUrl = "https://groups.google.com/g/pclink"
        assertTrue(ValidationEngine.isValidGoogleGroupUrl(directUrl))
        assertEquals("https://groups.google.com/g/pclink", ValidationEngine.cleanGoogleGroupUrl(directUrl))

        // Workspace domain link
        val workspaceUrl = "https://groups.google.com/a/example.com/g/pclink"
        assertTrue(ValidationEngine.isValidGoogleGroupUrl(workspaceUrl))
        assertEquals("https://groups.google.com/g/pclink", ValidationEngine.cleanGoogleGroupUrl(workspaceUrl))

        // Legacy /d/forum/ link
        val legacyForumUrl = "https://groups.google.com/d/forum/pclink"
        assertTrue(ValidationEngine.isValidGoogleGroupUrl(legacyForumUrl))
        assertEquals("https://groups.google.com/g/pclink", ValidationEngine.cleanGoogleGroupUrl(legacyForumUrl))
    }

    @Test
    fun testPlayStoreUrls() {
        val storeDetailsUrl = "https://play.google.com/store/apps/details?id=com.pcmaster.mobile"
        assertTrue(ValidationEngine.isValidGooglePlayOptInUrl(storeDetailsUrl, "com.pcmaster.mobile"))

        val testingOptInUrl = "https://play.google.com/apps/testing/com.pcmaster.mobile"
        assertTrue(ValidationEngine.isValidGooglePlayOptInUrl(testingOptInUrl, "com.pcmaster.mobile"))
    }
}
