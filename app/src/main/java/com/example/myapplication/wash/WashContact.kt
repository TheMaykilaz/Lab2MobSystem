package com.example.myapplication.wash

/** Статус останньої спроби відправки даних у хмару (для індикатора на екрані). */
enum class SyncStatus { IDLE, SYNCED, PENDING }

/** State — усе, що зараз видно на екрані. Тільки val, значення за замовчуванням обов'язкові. */
data class WashState(
    val phase: WashPhase = WashPhase.IDLE,
    val x: Float = 0f, val y: Float = 0f, val z: Float = 0f,
    val intensity: Float = 0f,
    val washedMs: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.IDLE
) {
    val progress: Float get() = (washedMs.toFloat() / WashThresholds.TARGET_WASH_MS).coerceIn(0f, 1f)
}

/** Action — усі дії користувача (або життєвого циклу) на цьому екрані. */
sealed interface WashAction {
    data object Start : WashAction       // onResume
    data object Stop : WashAction        // onPause
    data object ResetClick : WashAction
}

/** Event — разові повідомлення від ViewModel до екрана (Toast/Snackbar). */
sealed interface WashEvent {
    data class ShowMessage(val text: String) : WashEvent
}