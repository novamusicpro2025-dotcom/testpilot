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
import com.nova.a12tester.ui.theme.BrandIndigo
import com.nova.a12tester.ui.theme.BrandTealDark

@Composable
fun SplashScreen(onReady: () -> Unit) {
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(900); onReady() }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BrandTealDark, BrandIndigo))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
            Text("TestPilot", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
            Text("Closed Testing Community", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f))
        }
    }
}
