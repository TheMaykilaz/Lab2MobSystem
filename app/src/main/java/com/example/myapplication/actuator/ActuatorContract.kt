package com.example.myapplication.actuator

/** State — усе, що зараз видно на екрані "Manual Control". */
data class ActuatorState(
    val intensity: Float = 0f,
    val isAlarmActive: Boolean = false,      // локальна автоматична реакція (поріг з DecisionEngine)
    val manualFlashlightOn: Boolean = false, // ручне керування з панелі
    val remoteLedOn: Boolean = false         // стан, отриманий дистанційно через Long Polling
)

/** Action — усі дії користувача (або життєвого циклу) на цьому екрані. */
sealed interface ActuatorAction {
    data object Start : ActuatorAction               // onResume: слухати сенсор + почати Polling
    data object Stop : ActuatorAction                // onPause: відписатись, вимкнути актуатори
    data class ManualFlashlightToggle(val on: Boolean) : ActuatorAction
    data object ManualAlarmTrigger : ActuatorAction
    data object SimulateCloudToggle : ActuatorAction // імітує зміну значення на "консолі"
}

/** Event — разові повідомлення від ViewModel до екрана. */
sealed interface ActuatorEvent {
    data class ShowMessage(val text: String) : ActuatorEvent
}