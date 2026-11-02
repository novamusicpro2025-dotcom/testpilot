package com.nova.a12tester.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.a12tester.ui.theme.BrandIndigo
import com.nova.a12tester.ui.theme.BrandTealDark

private data class OnboardPage(val emoji: String, val title: String, val body: String)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardPage("🤝", "Reciprocal Testing", "Find genuine testers for your Android apps, and earn coins by testing others through Google Play closed testing."),
        OnboardPage("🎯", "Real Feedback, Honest Reviews", "Submit structured feedback and report bugs. Never fabricate reviews or incentivize positive ratings."),
        OnboardPage("🔒", "Privacy & Compliance", "We never ask for your Google password. Opt-in and Google Group steps are completed by you via official links.")
    )
    var index by remember { mutableIntStateOf(0) }
    val page = pages[index]

    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(150.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(page.emoji, fontSize = 64.sp) }
                }
                Text(page.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(page.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
            pages.indices.forEach { i ->
                Box(Modifier.size(if (i == index) 24.dp else 10.dp, 10.dp).background(if (i == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (index > 0) OutlinedButton(onClick = { index-- }, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(
                onClick = { if (index == pages.lastIndex) onFinish() else index++ },
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text(if (index == pages.lastIndex) "Get Started" else "Next") }
        }
        TextButton(onClick = onFinish, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Skip") }
        Spacer(Modifier.height(16.dp))
    }
}
