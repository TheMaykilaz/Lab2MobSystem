package com.example.myapplication.actuator

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.helpers.MockCloudControl
import com.example.myapplication.helpers.SensorReader
import com.example.myapplication.helpers.SmartActuator
import com.example.myapplication.wash.WashAction
import com.example.myapplication.wash.WashPhase
import com.example.myapplication.wash.WashViewModel
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
    private var lastHandledCommandId = 0L

    private var washViewModel: WashViewModel? = null
    private var washObserveJob: Job? = null

    /** Підключення WashViewModel для взаємодії між модулями (дистанційний запуск і слухання стану). */
    fun attachWashViewModel(vm: WashViewModel) {
        washViewModel = vm
        if (washObserveJob != null) return
        washObserveJob = viewModelScope.launch {
            var previousPhase = vm.state.value.phase
            vm.state.collect { washState ->
                val currentPhase = washState.phase
                _state.update { it.copy(washPhase = currentPhase) }

                // Коли таймер завершується (WashPhase.DONE) — ActuatorViewModel, який слухає стан WashViewModel,
                // автоматично вмикає вібрацію й ліхтарик ("час вийшов, руки помиті") і пише в Logcat.
                if (previousPhase == WashPhase.WASHING && currentPhase == WashPhase.DONE) {
                    onWashCompleted()
                } else if (previousPhase == WashPhase.DONE && currentPhase == WashPhase.IDLE) {
                    _state.update { it.copy(remoteLedOn = false) }
                    applyFlashlightHardware()
                }
                previousPhase = currentPhase
            }
        }
    }

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
                Log.i("ActuatorViewModel", "Натиснуто «Імітувати зміну в хмарній консолі» -> відправка команди в MockCloudControl")
                _events.send(ActuatorEvent.ShowMessage("Відправлено команду в хмарну консоль..."))
                _state.update { it.copy(remoteLedOn = false) }
                applyFlashlightHardware()
                MockCloudControl.sendRemoteWashCommand()
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
                val commandId = MockCloudControl.fetchWashCommand()
                if (commandId > 0 && commandId != lastHandledCommandId) {
                    lastHandledCommandId = commandId
                    Log.i("ActuatorViewModel", "Long Polling (MockCloudControl): виявлено дистанційну команду ($commandId) -> запускаємо таймер миття рук")
                    _events.send(ActuatorEvent.ShowMessage("Хмара: дистанційний запуск таймера миття рук!"))
                    _state.update { it.copy(remoteLedOn = false) }
                    applyFlashlightHardware()
                    washViewModel?.onAction(WashAction.RemoteStart)
                }
                delay(ActuatorThresholds.POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /** Автоматична реакція після завершення таймера 20 секунд: вібрація + ліхтарик + Logcat. */
    private fun onWashCompleted() {
        Log.i("ActuatorViewModel", "=== ТАЙМЕР ЗАВЕРШЕНО (WashPhase.DONE): час вийшов, руки помиті! ===")
        Log.i("ActuatorViewModel", "Автоматично вмикаємо вібрацію та ліхтарик через SmartActuator")
        _state.update { it.copy(remoteLedOn = true) }
        actuator.triggerAlarm()
        applyFlashlightHardware()
        viewModelScope.launch {
            _events.send(ActuatorEvent.ShowMessage("Час вийшов, руки помиті! Ліхтарик і вібрація активні"))
        }
    }

    /** Фізичний ліхтарик — один апаратний ресурс, тому поєднуємо всі джерела команди. */
    private fun applyFlashlightHardware() {
        val s = _state.value
        actuator.toggleFlashlight(s.manualFlashlightOn || s.remoteLedOn || s.isAlarmActive)
    }

    private fun allOff() {
        actuator.toggleFlashlight(false)
        _state.update { it.copy(isAlarmActive = false, remoteLedOn = false) }
    }
}