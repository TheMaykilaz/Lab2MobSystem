package com.example.myapplication.helpers

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Mock-реалізація IBleConnector. Імітує типовий GATT-протокол:
 * з'єднання -> виявлення сервісів -> підписка на Notify -> обмін даними.
 * Усі затримки симулюють реальний час відповіді BLE-стека.
 */
class MockBleConnector : IBleConnector {
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState

    private val _log = MutableStateFlow<List<String>>(emptyList())
    override val log: StateFlow<List<String>> = _log

    private fun appendLog(message: String) {
        Log.d("BLE", message)
        _log.value = _log.value + message
    }

    override suspend fun connect(device: BleDevice) {
        _connectionState.value = BleConnectionState.CONNECTING
        appendLog("connect(): ${device.name} [${device.address}]")
        delay(800) // імітація встановлення GATT-з'єднання

        appendLog("GATT: discoverServices()")
        delay(500) // імітація виявлення сервісів
        appendLog("Service знайдено: ${BleProfile.SERVICE_UUID}")

        appendLog("setCharacteristicNotification(STATUS, true)")
        _connectionState.value = BleConnectionState.CONNECTED
        appendLog("Підключено. Готові до обміну даними (Data Transfer).")
    }

    override fun disconnect() {
        appendLog("disconnect()")
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    override fun sendData(data: String) {
        if (_connectionState.value != BleConnectionState.CONNECTED) {
            appendLog("sendData(\"$data\") відхилено: пристрій не підключено")
            return
        }
        appendLog("WRITE -> ${BleProfile.COMMAND_CHARACTERISTIC_UUID}: \"$data\"")
        // Імітація підтвердження від пристрою через характеристику STATUS (Notify)
        appendLog("NOTIFY <- ${BleProfile.STATUS_CHARACTERISTIC_UUID}: \"ACK:$data\"")
    }
}