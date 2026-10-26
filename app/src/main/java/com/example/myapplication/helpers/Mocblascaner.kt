package com.example.myapplication.helpers

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Симулює BluetoothLeScanner: через 2 секунди "знаходить" віртуальний
 * IoT-пристрій (Hand Wash Hub), без доступу до реального Bluetooth-стеку.
 */
class MockBleScanner {
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _foundDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val foundDevices: StateFlow<List<BleDevice>> = _foundDevices

    /** Запускає сканування; suspend-функція, бо реальне сканування теж асинхронне. */
    suspend fun startScan() {
        _isScanning.value = true
        _foundDevices.value = emptyList()
        delay(2_000) // імітація часу виявлення пристрою по ефіру
        _foundDevices.value = listOf(
            BleDevice(address = "AA:BB:CC:00:11:22", name = "HandWash-SmartHub", rssi = -48)
        )
        _isScanning.value = false
    }
}