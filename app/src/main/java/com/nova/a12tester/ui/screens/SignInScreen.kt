package com.nova.a12tester.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.nova.a12tester.data.*
import com.nova.a12tester.domain.UserRole
import com.nova.a12tester.ui.components.ComplianceNote
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Sign-in screen using Google Play Services GoogleSignInClient.
 * Opens the native system account chooser listing all Gmail accounts on the phone.
 */
@Composable
fun SignInScreen(onSignedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showEmailDialog by remember { mutableStateOf(false) }
    var inputEmail by remember { mutableStateOf("") }

    val googleSignInClient = remember(context) {
        FirebaseAuthManager.getGoogleSignInClient(context, requestIdToken = true)
    }

    // System account chooser fallback
    val systemAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                val displayName = accountName.substringBefore('@').replaceFirstChar { it.uppercase() }
                scope.launch {
                    val anonUid = try {
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            ?: com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously().await().user?.uid
                    } catch (_: Exception) {
                        null
                    }
                    val uid = anonUid ?: ("google_" + kotlin.math.abs(accountName.hashCode()))

                    TestPilotRepository.completeGoogleSignIn(
                        googleEmail = accountName,
                        googleDisplayName = displayName,
                        googlePhotoUrl = "https://ui-avatars.com/api/?name=${displayName.take(2)}&background=2563EB&color=fff",
                        firebaseUid = uid
                    )
                    TestPilotRepository.acceptPrivacyConsentAndChooseRole(UserRole.BOTH, 35)

                    try {
                        val prefs = context.getSharedPreferences("testpilot_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("saved_user_id", uid)
                            .putString("saved_user_email", accountName)
                            .putString("saved_user_name", displayName)
                            .apply()
                    } catch (_: Exception) {}

                    onSignedIn()
                }
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data
        if (intent == null) {
            isLoading = false
            return@rememberLauncherForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                scope.launch {
                    val authResult = FirebaseAuthManager.signInWithGoogleAccount(account)
                    when (authResult) {
                        is FirebaseAuthManager.SignInResult.Success -> {
                            TestPilotRepository.completeGoogleSignIn(
                                googleEmail = authResult.email,
                                googleDisplayName = authResult.displayName,
                                googlePhotoUrl = authResult.photoUrl.ifBlank {
                                    "https://ui-avatars.com/api/?name=${authResult.displayName.take(2)}&background=2563EB&color=fff"
                                },
                                firebaseUid = authResult.uid
                            )
                            TestPilotRepository.acceptPrivacyConsentAndChooseRole(UserRole.BOTH, 35)
                            TestPilotRepository.applyAdminClaim(FirebaseAuthManager.hasAdminClaim())

                            try {
                                val prefs = context.getSharedPreferences("testpilot_prefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putString("saved_user_id", authResult.uid)
                                    .putString("saved_user_email", authResult.email)
                                    .putString("saved_user_name", authResult.displayName)
                                    .apply()
                            } catch (_: Exception) {}

                            isLoading = false
                            onSignedIn()
                        }
                        is FirebaseAuthManager.SignInResult.Failure -> {
                            errorMessage = authResult.message
                            isLoading = false
                        }
                        is FirebaseAuthManager.SignInResult.Cancelled -> {
                            isLoading = false
                        }
                    }
                }
            } else {
                isLoading = false
            }
        } catch (e: ApiException) {
            isLoading = false
            when (e.statusCode) {
                12501, 12502 -> {} // User cancelled
                else -> {
                    // If Google ID token is not yet ready, fall back to native account chooser
                    try {
                        val chooser = android.accounts.AccountManager.newChooseAccountIntent(
                            null, null, arrayOf("com.google"), null, null, null, null
                        )
                        systemAccountPickerLauncher.launch(chooser)
                    } catch (_: Exception) {
                        errorMessage = "Unable to sign in with Google (Error ${e.statusCode})."
                    }
                }
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Unable to sign in: ${e.message}"
        }
    }

    fun launchGoogleChooser() {
        isLoading = true
        errorMessage = null
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.RocketLaunch,
            contentDescription = null,
            Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "TestPilot",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "14-Day Reciprocal Closed Testing Community",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Every developer tests other apps to get 12 genuine testers for their own app on Google Play.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(36.dp))

        // Error message card
        errorMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // Google Sign-In button
        Button(
            onClick = { launchGoogleChooser() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text("Selecting account…", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Filled.AccountCircle, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Continue with Google", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Enter email manually button
        TextButton(
            onClick = { showEmailDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.MailOutline, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign in with Email", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))
        ComplianceNote("TestPilot uses Google Sign-In via Firebase Auth. We never request your Google password. Your email is used only for account identification within this community.")
    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Sign in with Email") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter your Gmail address to sign in to your TestPilot account:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Gmail / Email") },
                        placeholder = { Text("your.name@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = inputEmail.trim()
                        if (email.contains("@") && email.contains(".")) {
                            showEmailDialog = false
                            val displayName = email.substringBefore('@').replaceFirstChar { it.uppercase() }
                            val uid = "user_" + kotlin.math.abs(email.hashCode())

                            TestPilotRepository.completeGoogleSignIn(
                                googleEmail = email,
                                googleDisplayName = displayName,
                                googlePhotoUrl = "https://ui-avatars.com/api/?name=${displayName.take(2)}&background=2563EB&color=fff",
                                firebaseUid = uid
                            )
                            TestPilotRepository.acceptPrivacyConsentAndChooseRole(UserRole.BOTH, 35)

                            try {
                                val prefs = context.getSharedPreferences("testpilot_prefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putString("saved_user_id", uid)
                                    .putString("saved_user_email", email)
                                    .putString("saved_user_name", displayName)
                                    .apply()
                            } catch (_: Exception) {}

                            onSignedIn()
                        } else {
                            android.widget.Toast.makeText(context, "Please enter a valid email address", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
