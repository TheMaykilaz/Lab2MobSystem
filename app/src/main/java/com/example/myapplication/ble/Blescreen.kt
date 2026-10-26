package com.example.myapplication.ble

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.helpers.BleConnectionState

@Composable
fun BleScreen(vm: BleViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is BleEvent.ShowMessage -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }
    BleContent(state = state, onAction = vm::onAction)
}

@Composable
fun BleContent(state: BleState, onAction: (BleAction) -> Unit) {
    val (statusText, statusColor) = when (state.connectionState) {
        BleConnectionState.DISCONNECTED -> "Disconnected" to Color.Gray
        BleConnectionState.SCANNING -> "Scanning..." to Color(0xFF1E88E5)
        BleConnectionState.CONNECTING -> "Connecting..." to Color(0xFFFB8C00)
        BleConnectionState.CONNECTED -> "Connected" to Color(0xFF43A047)
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("BLE Hand Wash Hub", fontSize = 22.sp)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(statusColor, shape = CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(statusText, color = statusColor)
        }

        Button(onClick = { onAction(BleAction.StartScan) }, enabled = !state.isScanning) {
            Text(if (state.isScanning) "Пошук..." else "Пошук пристроїв")
        }
        if (state.isScanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        Text("Знайдені пристрої:")
        state.devices.forEach { device ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(device.name)
                    Text("${device.address}  RSSI ${device.rssi} dBm", fontSize = 12.sp, color = Color.Gray)
                }
                Button(onClick = { onAction(BleAction.Connect(device)) }) { Text("Підключити") }
            }
        }

        Button(
            onClick = { onAction(BleAction.SendWashDone) },
            enabled = state.connectionState == BleConnectionState.CONNECTED
        ) { Text("Надіслати WASH_DONE") }

        Text("Журнал обміну:")
        LazyColumn(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0D1117)).padding(8.dp)) {
            items(state.log) { line ->
                Text(line, color = Color(0xFF9FEF9F), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}