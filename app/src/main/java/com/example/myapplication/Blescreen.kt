package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun BleScreen(
    scanner: MockBleScanner = remember { MockBleScanner() },
    connector: IBleConnector = remember { MockBleConnector() }
) {
    val scope = rememberCoroutineScope()
    val isScanning by scanner.isScanning.collectAsState()
    val devices by scanner.foundDevices.collectAsState()
    val state by connector.connectionState.collectAsState()
    val log by connector.log.collectAsState()

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("BLE Hand Wash Hub", fontSize = 22.sp)

        // Індикатор статусу з'єднання
        val (statusText, statusColor) = when (state) {
            BleConnectionState.DISCONNECTED -> "Disconnected" to Color.Gray
            BleConnectionState.SCANNING -> "Scanning..." to Color(0xFF1E88E5)
            BleConnectionState.CONNECTING -> "Connecting..." to Color(0xFFFB8C00)
            BleConnectionState.CONNECTED -> "Connected" to Color(0xFF43A047)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(12.dp).background(statusColor, shape = androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(statusText, color = statusColor)
        }

        // Кнопка пошуку + ProgressBar
        Button(
            onClick = { scope.launch { scanner.startScan() } },
            enabled = !isScanning
        ) { Text(if (isScanning) "Пошук..." else "Пошук пристроїв") }
        if (isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Список знайдених пристроїв
        Text("Знайдені пристрої:")
        devices.forEach { device ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(device.name)
                    Text("${device.address}  RSSI ${device.rssi} dBm", fontSize = 12.sp, color = Color.Gray)
                }
                Button(onClick = { scope.launch { connector.connect(device) } }) {
                    Text("Підключити")
                }
            }
        }

        // Тестова команда (демонструє sendData)
        Button(
            onClick = { connector.sendData("WASH_DONE") },
            enabled = state == BleConnectionState.CONNECTED
        ) { Text("Надіслати WASH_DONE") }

        // Термінал обміну даними
        Text("Журнал обміну:")
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0D1117)).padding(8.dp)
        ) {
            items(log) { line ->
                Text(line, color = Color(0xFF9FEF9F), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }
    }
}