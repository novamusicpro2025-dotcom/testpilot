package com.nova.a12tester.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.domain.ThemeMode
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import com.nova.a12tester.ui.components.ComplianceNote

@Composable
fun SettingsScreen(
    vm: TestPilotViewModel,
    onOpenFeatures: () -> Unit = {},
    onSignedOut: () -> Unit
) {
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return
    var name by remember { mutableStateOf(user.displayName) }
    var showDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // Features & Platform Guide Entry
        Text("Platform & Rules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Card(
            onClick = onOpenFeatures,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("App Features & Platform Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("14-day reciprocal testing, OS usage rules, quotas & VIP benefits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        Text("About & Legal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("App Version", style = MaterialTheme.typography.bodyMedium)
                    Text("1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    modifier = Modifier.fillMaxWidth().clickable {
                        try {
                            // NOTE: Replace 'your-username' with your actual GitHub username once you publish the repo
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://novamusicpro2025-dotcom.github.io/testpilot/docs/privacy.html"))
                            context.startActivity(intent)
                        } catch (e: Exception) { }
                    }.padding(vertical = 4.dp)
                ) {
                    Text("Privacy Policy", style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    modifier = Modifier.fillMaxWidth().clickable {
                        try {
                            // NOTE: Replace 'your-username' with your actual GitHub username once you publish the repo
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://novamusicpro2025-dotcom.github.io/testpilot/docs/terms.html"))
                            context.startActivity(intent)
                        } catch (e: Exception) { }
                    }.padding(vertical = 4.dp)
                ) {
                    Text("Terms of Service", style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        Text("Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = state.themeMode == ThemeMode.LIGHT, onClick = { vm.setTheme(ThemeMode.LIGHT) }, label = { Text("Light") })
            FilterChip(selected = state.themeMode == ThemeMode.DARK, onClick = { vm.setTheme(ThemeMode.DARK) }, label = { Text("Dark") })
            FilterChip(selected = state.themeMode == ThemeMode.SYSTEM, onClick = { vm.setTheme(ThemeMode.SYSTEM) }, label = { Text("System") })
        }

        Text("Profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.updateName(name) }) { Text("Save Profile") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        OutlinedButton(onClick = onSignedOut, modifier = Modifier.fillMaxWidth()) { Icon(Icons.AutoMirrored.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Sign Out") }

        Button(
            onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
        ) { Icon(Icons.Filled.DeleteForever, null); Spacer(Modifier.width(8.dp)); Text("Delete Account & Data") }

        ComplianceNote("Deleting your account permanently removes your profile, submitted apps, assignments, feedback, coin ledger and notifications. This action cannot be undone. You may also revoke TestPilot's Google access anytime at myaccount.google.com.")
        Spacer(Modifier.height(24.dp))
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete account?") },
            text = { Text("This permanently deletes your account and all associated data. This cannot be undone.") },
            confirmButton = { Button(onClick = { showDelete = false; vm.deleteAccount(); onSignedOut() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}
