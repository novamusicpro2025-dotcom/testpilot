package com.nova.a12tester.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Handles real Firebase Auth with Google Sign-In using GoogleSignInClient.
 * Opens the native Google Account Picker listing all Gmail accounts on the device.
 */
object FirebaseAuthManager {

    const val WEB_CLIENT_ID =
        "749424011732-l1kkphqu28uhsdfpgtgga7pl29hsoooa.apps.googleusercontent.com"

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    /** Returns the currently signed-in Firebase user, or null. */
    val currentFirebaseUser get() = auth.currentUser

    /**
     * Reads the server-minted `admin` custom claim from the current ID token.
     * This is the ONLY trustworthy admin signal — the users doc field named
     * `isAdminClaimVerified` is client-writable, so firestore.rules ignore it.
     * Returns false when signed out, when the claim is absent, or on any error.
     */
    suspend fun hasAdminClaim(): Boolean = try {
        // Note: firebase-auth 23.x exposes getIdToken (Task<GetTokenResult>);
        // getIdTokenResult(...) is not available in this artifact version.
        val result = auth.currentUser?.getIdToken(true)?.await()
        result?.claims?.get("admin") == true
    } catch (_: Exception) {
        false
    }

    /**
     * Builds GoogleSignInClient configured with profile and email.
     * When requestIdToken is false, Google Play Services opens the account picker and returns
     * the user's Google account without requiring SHA-1 to be verified by Google servers.
     */
    fun getGoogleSignInClient(context: Context, requestIdToken: Boolean = false): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        if (requestIdToken && WEB_CLIENT_ID.isNotBlank()) {
            builder.requestIdToken(WEB_CLIENT_ID)
        }
        return GoogleSignIn.getClient(context, builder.build())
    }

    /**
     * Signs in with the selected GoogleSignInAccount.
     * If idToken is present, exchanges it for a Firebase Auth credential.
     * If idToken is absent, authentication fails — no anonymous fallback is permitted.
     */
    suspend fun signInWithGoogleAccount(account: GoogleSignInAccount): SignInResult {
        return try {
            val email = account.email.orEmpty()
            val displayName = account.displayName ?: email.substringBefore('@').ifBlank { "User" }
            val photoUrl = account.photoUrl?.toString().orEmpty()

            val idToken = account.idToken
            if (!idToken.isNullOrBlank()) {
                try {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(credential).await()
                    val user = authResult.user
                    if (user != null) {
                        return SignInResult.Success(
                            uid = user.uid,
                            displayName = user.displayName ?: displayName,
                            email = user.email ?: email,
                            photoUrl = user.photoUrl?.toString() ?: photoUrl
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FirebaseAuthManager", "signInWithCredential failed: ${e.message}")
                }
            }

            // No anonymous fallback — idToken is required for Firebase authentication.
            return SignInResult.Failure("No identity token received from Google. Please sign in again.")
        } catch (e: Exception) {
            SignInResult.Failure(e.message ?: "Authentication failed.")
        }
    }

    /** Placeholder kept for API compatibility — anonymous sign-in is disabled. */
    @Deprecated("Anonymous authentication is disabled for security.")
    fun signInAnonymouslyDisabled(): Nothing = throw IllegalStateException("Anonymous sign-in is disabled")

    /** Signs out from Firebase Auth and clears Google account selection. */
    fun signOut(context: Context? = null) {
        try {
            auth.signOut()
        } catch (_: Exception) {}
        if (context != null) {
            try {
                getGoogleSignInClient(context, requestIdToken = false).signOut()
            } catch (_: Exception) {}
        }
    }

    sealed class SignInResult {
        data class Success(
            val uid: String,
            val displayName: String,
            val email: String,
            val photoUrl: String
        ) : SignInResult()
        data class Failure(val message: String) : SignInResult()
        object Cancelled : SignInResult()
    }
}
