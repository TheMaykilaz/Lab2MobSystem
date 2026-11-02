package com.example.myapplication.actuator

/** Пороги для блоку прийняття рішення (критичні значення сенсора з Л.Р. №1). */
object ActuatorThresholds {
    const val ALARM_ACCEL = 2.5f       // м/с²: поріг різкого руху, що активує тривогу
    const val POLL_INTERVAL_MS = 3_000L // інтервал Long Polling до "хмари"
}

/**
 * Чиста логіка прийняття рішення: IF sensor_value > threshold THEN activate_actuator.
 * Без залежностей від Android — легко тестувати окремо від UI та сенсора.
 */
class DecisionEngine {
    /** @return true, якщо інтенсивність перевищує критичний поріг і актуатор треба активувати. */
    fun shouldActivate(intensity: Float): Boolean = intensity > ActuatorThresholds.ALARM_ACCEL
}