package com.example.myapplication.helpers

import kotlinx.coroutines.flow.StateFlow

/**
 * Абстракція BLE-з'єднання. Реальна реалізація працювала б поверх BluetoothGatt,
 * MockBleConnector — симулює ту саму поведінку із затримками.
 */
interface IBleConnector {
    /** Поточний стан з'єднання (StateFlow — для реактивного оновлення UI). */
    val connectionState: StateFlow<BleConnectionState>

    /** Журнал обміну даними (показується в UI-терміналі та Logcat). */
    val log: StateFlow<List<String>>

    /** Підключення до пристрою: Connecting -> Connected. */
    suspend fun connect(device: BleDevice)

    /** Розрив з'єднання: будь-який стан -> Disconnected. */
    fun disconnect()

    /** Відправка команди на характеристику COMMAND (Write). */
    fun sendData(data: String)
}