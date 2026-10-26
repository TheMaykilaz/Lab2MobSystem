package com.example.myapplication.wash

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
/** Підключає ViewModel до чистого Content: отримує State, надсилає Action, показує Event. */
@Composable
fun WashScreen(vm: WashViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is WashEvent.ShowMessage -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    WashContent(state = state, onAction = vm::onAction)
}

/** Чиста функція: State на вході, Action на виході — легко робити Preview і тестувати. */
@Composable
fun WashContent(state: WashState, onAction: (WashAction) -> Unit) {
    val remaining = ((WashThresholds.TARGET_WASH_MS - state.washedMs) / 1000f).coerceAtLeast(0f)
    val color = when (state.phase) {
        WashPhase.IDLE -> Color.Gray
        WashPhase.WASHING -> Color(0xFF1E88E5)
        WashPhase.DONE -> Color(0xFF43A047)
    }
    val label = when (state.phase) {
        WashPhase.IDLE -> "Різко струсніть телефон"
        WashPhase.WASHING -> "Миття..."
        WashPhase.DONE -> "Готово! Руки чисті"
    }
    val syncColor = when (state.syncStatus) {
        SyncStatus.IDLE -> Color.Gray
        SyncStatus.SYNCED -> Color(0xFF43A047)
        SyncStatus.PENDING -> Color(0xFFE53935)
    }
    val syncLabel = when (state.syncStatus) {
        SyncStatus.IDLE -> "Хмара: немає даних"
        SyncStatus.SYNCED -> "Хмара: синхронізовано"
        SyncStatus.PENDING -> "Хмара: очікує (немає мережі)"
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Hand Wash Timer", fontSize = 24.sp)

        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 24f, cap = StrokeCap.Round)
                drawArc(Color.LightGray, -90f, 360f, false, style = stroke)
                drawArc(color, -90f, 360f * state.progress, false, style = stroke)
            }
            Text("%.0f c".format(remaining), fontSize = 40.sp)
        }
        Text(label, fontSize = 20.sp, color = color)

        Text("X: %.2f   Y: %.2f   Z: %.2f  (м/с²)".format(state.x, state.y, state.z))

        Text("Інтенсивність руху: %.2f (поріг %.1f)".format(state.intensity, WashThresholds.ACTIVE_ACCEL))
        LinearProgressIndicator(
            progress = { (state.intensity / 6f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(12.dp)
        )

        // Індикатор "Хмарна синхронізація" (вимога Л.Р. №3)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(syncColor, shape = CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(syncLabel, fontSize = 13.sp, color = syncColor)
        }

        if (state.phase != WashPhase.IDLE) {
            Button(onClick = { onAction(WashAction.ResetClick) }) {
                Text(if (state.phase == WashPhase.DONE) "Скинути" else "Скасувати")
            }
        }
    }
}