package com.example.myapplication.actuator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.helpers.MockCloudControl
import com.example.myapplication.helpers.SensorReader
import com.example.myapplication.helpers.SmartActuator
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ActuatorViewModel(app: Application) : AndroidViewModel(app) {
    private val sensorReader = SensorReader(app)
    private val actuator = SmartActuator(app)
    private val decisionEngine = DecisionEngine()
    private val intensityFilter = AccelIntensityFilter()

    private val _state = MutableStateFlow(ActuatorState())
    val state: StateFlow<ActuatorState> = _state.asStateFlow()

    private val _events = Channel<ActuatorEvent>()
    val events = _events.receiveAsFlow()

    private var sensorJob: Job? = null
    private var pollJob: Job? = null

    /** ЄДИНИЙ вхід у ViewModel — всі дії приходять сюди. */
    fun onAction(action: ActuatorAction) {
        when (action) {
            ActuatorAction.Start -> { startSensor(); startPolling() }
            ActuatorAction.Stop -> { stopSensor(); stopPolling(); allOff() }
            is ActuatorAction.ManualFlashlightToggle -> {
                _state.update { it.copy(manualFlashlightOn = action.on) }
                applyFlashlightHardware()
            }
            ActuatorAction.ManualAlarmTrigger -> actuator.triggerAlarm()
            ActuatorAction.SimulateCloudToggle -> viewModelScope.launch {
                val newValue = !_state.value.remoteLedOn
                MockCloudControl.setRemoteCommand(newValue) // імітація запису в хмару ("консоль")
            }
        }
    }

    /** А. Локальне автоматичне керування: IF intensity > threshold THEN activate. */
    private fun startSensor() {
        if (sensorJob != null) return
        sensorJob = viewModelScope.launch {
            sensorReader.readings().collect { v ->
                val intensity = intensityFilter.process(v[0], v[1], v[2])
                val shouldActivate = decisionEngine.shouldActivate(intensity)
                val wasActive = _state.value.isAlarmActive
                _state.update { it.copy(intensity = intensity, isAlarmActive = shouldActivate) }
                if (shouldActivate && !wasActive) {
                    actuator.triggerAlarm()
                    _events.send(ActuatorEvent.ShowMessage("Тривога: різкий рух!"))
                }
                if (shouldActivate != wasActive) applyFlashlightHardware()
            }
        }
    }

    private fun stopSensor() {
        sensorJob?.cancel()
        sensorJob = null
    }

    /** Б. Дистанційне керування: Long Polling опитує MockCloudControl кожні 3с. */
    private fun startPolling() {
        if (pollJob != null) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                val remoteOn = MockCloudControl.fetchCommand()
                if (remoteOn != _state.value.remoteLedOn) {
                    _state.update { it.copy(remoteLedOn = remoteOn) }
                    applyFlashlightHardware()
                }
                delay(ActuatorThresholds.POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /** Фізичний ліхтарик — один апаратний ресурс, тому поєднуємо всі джерела команди. */
    private fun applyFlashlightHardware() {
        val s = _state.value
        actuator.toggleFlashlight(s.manualFlashlightOn || s.remoteLedOn || s.isAlarmActive)
    }

    private fun allOff() {
        actuator.toggleFlashlight(false)
        _state.update { it.copy(isAlarmActive = false) }
    }
}