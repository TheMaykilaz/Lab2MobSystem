package com.example.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppRoot() } }
    }
}

/** Простий перемикач між екраном Л.Р. №1 (таймер) і Л.Р. №2 (BLE), без Navigation-бібліотеки. */
@Composable
fun AppRoot() {
    var showBle by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = if (showBle) 1 else 0) {
            Tab(selected = !showBle, onClick = { showBle = false }, text = { Text("Hand Wash") })
            Tab(selected = showBle, onClick = { showBle = true }, text = { Text("BLE") })
        }
        Box(Modifier.weight(1f)) {
            if (showBle) BleScreen() else HandWashScreen()
        }
    }
}

/**
 * Обгортка над SensorManager: реєструє слухач при ON_RESUME і ОБОВ'ЯЗКОВО
 * знімає його при ON_PAUSE (енергозбереження).
 */
@Composable
fun rememberWashSnapshot(): Pair<State<WashSnapshot>, () -> Unit> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snapshot = remember { mutableStateOf(WashSnapshot()) }
    val engine = remember { HandWashEngine() }

    DisposableEffect(lifecycleOwner) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val nowMs = SystemClock.elapsedRealtime()
                snapshot.value = engine.onSample(e.values[0], e.values[1], e.values[2], nowMs)
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) = Unit
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
                Lifecycle.Event.ON_PAUSE -> {
                    sm.unregisterListener(listener)
                    engine.reset()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sm.unregisterListener(listener)
        }
    }
    return snapshot to { engine.reset(); snapshot.value = WashSnapshot() }
}

@Composable
fun HandWashScreen() {
    val (state, reset) = rememberWashSnapshot()
    val s by state
    val remaining = ((Thresholds.TARGET_WASH_MS - s.washedMs) / 1000f).coerceAtLeast(0f)
    val color = when (s.state) {
        WashState.IDLE -> Color.Gray
        WashState.WASHING -> Color(0xFF1E88E5)
        WashState.DONE -> Color(0xFF43A047)
    }
    val label = when (s.state) {
        WashState.IDLE -> "Різко струсніть телефон"
        WashState.WASHING -> "Миття..."
        WashState.DONE -> "Готово! Руки чисті"
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Hand Wash Timer", fontSize = 24.sp)

        // Кільцевий індикатор прогресу (Canvas)
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 24f, cap = StrokeCap.Round)
                drawArc(Color.LightGray, -90f, 360f, false, style = stroke)
                drawArc(color, -90f, 360f * s.progress, false, style = stroke)
            }
            Text("%.0f c".format(remaining), fontSize = 40.sp)
        }
        Text(label, fontSize = 20.sp, color = color)

        // Поточні значення акселерометра
        Text("X: %.2f   Y: %.2f   Z: %.2f  (м/с²)".format(s.x, s.y, s.z))

        // Індикатор інтенсивності руху (ProgressBar)
        Text("Інтенсивність руху: %.2f (поріг %.1f)".format(s.intensity, Thresholds.ACTIVE_ACCEL))
        LinearProgressIndicator(
            progress = { (s.intensity / 6f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(12.dp)
        )

        // Кнопка повторного запуску
        if (s.state != WashState.IDLE) {
            Button(onClick = reset) { Text(if (s.state == WashState.DONE) "Скинути" else "Скасувати") }
        }
    }
}