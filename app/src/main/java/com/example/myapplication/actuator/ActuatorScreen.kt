package com.example.myapplication.actuator

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.wash.WashPhase

@Composable
fun ActuatorScreen(vm: ActuatorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is ActuatorEvent.ShowMessage -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }
    ActuatorContent(state = state, onAction = vm::onAction)
}

@Composable
fun ActuatorContent(state: ActuatorState, onAction: (ActuatorAction) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Manual Control", fontSize = 22.sp)

        // А. Локальна автоматична реакція
        StatusRow(
            label = if (state.isAlarmActive) "Тривога активна (автоматично)" else "Спокій",
            color = if (state.isAlarmActive) Color(0xFFE53935) else Color.Gray
        )
        Text("Інтенсивність руху: %.2f (поріг %.1f)".format(state.intensity, ActuatorThresholds.ALARM_ACCEL))
        LinearProgressIndicator(
            progress = { (state.intensity / 5f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp)
        )

        HorizontalDivider()

        // Ручне керування
        Text("Ручне керування")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ліхтарик (прожектор)")
            Switch(
                checked = state.manualFlashlightOn,
                onCheckedChange = { onAction(ActuatorAction.ManualFlashlightToggle(it)) }
            )
        }
        Button(onClick = { onAction(ActuatorAction.ManualAlarmTrigger) }) {
            Text("Вручну активувати сирену (вібрація)")
        }

        HorizontalDivider()

        // Б. Дистанційне керування (Cloud-to-Device, Long Polling)
        Text("Дистанційне керування (хмара)")
        val remoteLabel = when (state.washPhase) {
            WashPhase.IDLE -> if (state.remoteLedOn) "LED: увімкнено" else "LED: вимкнено (очікування команди)"
            WashPhase.WASHING -> "Хмарна команда: триває миття рук (20 с)..."
            WashPhase.DONE -> "Час вийшов, руки помиті! (LED + вібрація)"
        }
        val remoteColor = when (state.washPhase) {
            WashPhase.IDLE -> if (state.remoteLedOn) Color(0xFF43A047) else Color.Gray
            WashPhase.WASHING -> Color(0xFF1E88E5)
            WashPhase.DONE -> Color(0xFF43A047)
        }
        StatusRow(
            label = remoteLabel,
            color = remoteColor
        )
        Button(onClick = { onAction(ActuatorAction.SimulateCloudToggle) }) {
            Text("Імітувати зміну в хмарній консолі")
        }
        Text(
            "Опитування (Long Polling) кожні ${ActuatorThresholds.POLL_INTERVAL_MS / 1000} с",
            fontSize = 12.sp, color = Color.Gray
        )
    }
}

@Composable
private fun StatusRow(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, shape = CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(label, color = color)
    }
}