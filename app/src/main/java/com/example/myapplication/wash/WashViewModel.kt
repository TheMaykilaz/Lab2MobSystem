package com.example.myapplication.wash

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.di.AppContainer
import com.example.myapplication.helpers.SensorReader
import com.example.myapplication.repository.SensorReading
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SYNC_INTERVAL_MS = 5_000L   // не частіше ніж раз на 5 с...
private const val SYNC_DELTA = 1.0f           // ...або одразу, якщо інтенсивність змінилась на 1.0+

class WashViewModel(app: Application) : AndroidViewModel(app) {
    private val sensorReader = SensorReader(app)
    private val repository = AppContainer.sensorRepository(app)
    private val engine = HandWashEngine()

    private val _state = MutableStateFlow(WashState())
    val state: StateFlow<WashState> = _state.asStateFlow()

    private val _events = Channel<WashEvent>()
    val events = _events.receiveAsFlow()

    private var sensorJob: Job? = null
    private var countdownJob: Job? = null
    private var lastSyncAt = 0L
    private var lastSyncedIntensity = 0f

    /** ЄДИНИЙ вхід у ViewModel — всі дії приходять сюди. */
    fun onAction(action: WashAction) {
        when (action) {
            WashAction.Start -> startSensor()
            WashAction.Stop -> stopSensor()
            WashAction.ResetClick -> {
                stopCountdown()
                engine.reset()
                lastSyncAt = 0; lastSyncedIntensity = 0f
                _state.update { WashState() }
            }
            WashAction.RemoteStart -> {
                stopCountdown()
                engine.reset()
                val now = SystemClock.elapsedRealtime()
                engine.start(now)
                _state.update {
                    it.copy(
                        phase = WashPhase.WASHING,
                        washedMs = 0L
                    )
                }
                startCountdown()
            }
        }
    }

    private fun startSensor() {
        if (sensorJob != null) return
        sensorJob = viewModelScope.launch {
            sensorReader.readings().collect { v ->
                val wasIdle = _state.value.phase == WashPhase.IDLE
                val snap = engine.onSample(v[0], v[1], v[2], SystemClock.elapsedRealtime())
                _state.update {
                    it.copy(
                        phase = snap.phase, x = snap.x, y = snap.y, z = snap.z,
                        intensity = snap.intensity, washedMs = snap.washedMs
                    )
                }
                if (wasIdle && snap.phase == WashPhase.WASHING) {
                    startCountdown()
                }
                maybeSync(snap)
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive && _state.value.phase == WashPhase.WASHING) {
                kotlinx.coroutines.delay(100L)
                val snap = engine.tick(SystemClock.elapsedRealtime())
                _state.update {
                    it.copy(
                        phase = snap.phase,
                        washedMs = snap.washedMs
                    )
                }
                if (snap.phase == WashPhase.DONE) {
                    break
                }
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    private fun stopSensor() {
        sensorJob?.cancel() // скасування Flow -> unregisterListener() у SensorReader
        sensorJob = null
    }

    /** Відправка в хмару не миттєва: раз на 5 с або при різкій зміні інтенсивності. */
    private fun maybeSync(snap: WashSnapshot) {
        val now = System.currentTimeMillis()
        val delta = abs(snap.intensity - lastSyncedIntensity)
        if (now - lastSyncAt < SYNC_INTERVAL_MS && delta < SYNC_DELTA) return
        lastSyncAt = now
        lastSyncedIntensity = snap.intensity

        val reading = SensorReading(
            deviceId = "emulator-1",
            type = "accelerometer",
            value = snap.intensity,
            x = snap.x, y = snap.y, z = snap.z,
            timestamp = now
        )
        viewModelScope.launch {
            repository.save(reading)
                .onSuccess { _state.update { it.copy(syncStatus = SyncStatus.SYNCED) } }
                .onFailure {
                    _state.update { it.copy(syncStatus = SyncStatus.PENDING) }
                    _events.send(WashEvent.ShowMessage("Немає мережі — дані у черзі"))
                }
        }
    }
}