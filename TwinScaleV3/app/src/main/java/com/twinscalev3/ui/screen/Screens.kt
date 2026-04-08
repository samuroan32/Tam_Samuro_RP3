package com.twinscalev3.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.twinscalev3.data.model.GrowthMode
import com.twinscalev3.notification.ChatPresenceTracker
import com.twinscalev3.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200)
        onDone()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TwinScaleV3") }
}

@Composable
fun RoomScreen(vm: AppViewModel, onReady: () -> Unit) {
    var roomId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var initialGrowth by remember { mutableStateOf("1000") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Create or Join Room")
        OutlinedTextField(roomId, { roomId = it }, label = { Text("Room ID") })
        OutlinedTextField(name, { name = it }, label = { Text("Your Name") })
        OutlinedTextField(initialGrowth, { initialGrowth = it }, label = { Text("Initial Growth (only here)") })
        Button(onClick = {
            vm.enterRoom(roomId, name, initialGrowth)
            onReady()
        }) { Text("Continue") }
    }
}

@Composable
fun GrowthScreen(vm: AppViewModel, onChat: () -> Unit, onCompare: () -> Unit, onSettings: () -> Unit) {
    val state by vm.uiState.collectAsState()

    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFEEE4FF), Color(0xFFD8F0FF)))
        ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mode: ${state.mode}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GrowthMode.entries.forEach { m ->
                Button(onClick = { vm.switchMode(m) }) { Text(m.name) }
            }
        }
        Text("Your Growth: ${state.selfGrowth}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::growSelf) { Text("Grow") }
            Button(onClick = vm::shrinkSelf) { Text("Shrink") }
            Button(onClick = vm::boostPartner) { Text("Boost Partner") }
            Button(onClick = vm::drainPartner) { Text("Drain Partner") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onChat) { Text("Chat") }
            Button(onClick = onCompare) { Text("Compare") }
            Button(onClick = onSettings) { Text("Settings") }
        }
    }
}

@Composable
fun ChatScreen(vm: AppViewModel) {
    val state by vm.uiState.collectAsState()
    var text by remember { mutableStateOf("") }

    DisposableEffect(state.partnerId) {
        ChatPresenceTracker.isChatOpen = true
        ChatPresenceTracker.activePartnerId = state.partnerId
        onDispose {
            ChatPresenceTracker.isChatOpen = false
            ChatPresenceTracker.activePartnerId = null
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyColumn(Modifier.weight(1f)) {
            items(state.messages) { msg ->
                Card(Modifier.fillMaxWidth().padding(4.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        Text(msg.text.ifBlank { msg.imageFallbackText ?: "[image]" })
                        Text("t=${msg.timestamp}")
                    }
                }
            }
        }
        OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Message") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                vm.sendText(text)
                text = ""
            }) { Text("Send") }
            Button(onClick = vm::sendSampleImageFallback) { Text("Send Image") }
        }
    }
}

@Composable
fun ComparisonScreen(vm: AppViewModel) {
    val state by vm.uiState.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Comparison")
        Text("You: ${state.selfGrowth}")
        Text("Partner: ${state.partnerGrowth}")
        Text("Ratio: ${state.ratioText}")
        Text("Larger: ${state.largerLabel}")

        val selfScale = state.selfScale
        val partnerScale = state.partnerScale

        Canvas(
            modifier = Modifier.fillMaxWidth().height(250.dp)
                .background(Color(0xFFE8E8F2), RoundedCornerShape(16.dp))
        ) {
            val w = size.width
            val h = size.height
            drawCircle(Color(0xFF8C6FF7), radius = h * 0.18f * selfScale, center = Offset(w * 0.33f, h * 0.7f))
            drawCircle(Color(0xFF54B4D3), radius = h * 0.18f * partnerScale, center = Offset(w * 0.67f, h * 0.7f))
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val state by vm.uiState.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings")
        Text("Room: ${state.roomId}")
        Text("User: ${state.selfName}")
        Text("Online: ${state.online}")
    }
}
