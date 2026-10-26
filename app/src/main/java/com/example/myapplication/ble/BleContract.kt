package com.example.myapplication.ble

import com.example.myapplication.helpers.BleConnectionState
import com.example.myapplication.helpers.BleDevice

data class BleState(
    val isScanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val log: List<String> = emptyList()
)

sealed interface BleAction {
    data object StartScan : BleAction
    data class Connect(val device: BleDevice) : BleAction
    data object SendWashDone : BleAction
}

sealed interface BleEvent {
    data class ShowMessage(val text: String) : BleEvent
}