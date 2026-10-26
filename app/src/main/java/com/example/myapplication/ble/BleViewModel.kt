package com.example.myapplication.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.helpers.BleConnectionState
import com.example.myapplication.helpers.MockBleConnector
import com.example.myapplication.helpers.MockBleScanner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BleViewModel(
    private val scanner: MockBleScanner = MockBleScanner(),
    private val connector: MockBleConnector = MockBleConnector()
) : ViewModel() {

    private val _events = Channel<BleEvent>()
    val events = _events.receiveAsFlow()

    /** State екрана складається комбінацією трьох StateFlow помічників. */
    val state: StateFlow<BleState> = combine(
        scanner.isScanning, scanner.foundDevices, connector.connectionState, connector.log
    ) { scanning, devices, conn, log ->
        BleState(scanning, devices, conn, log)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BleState())

    fun onAction(action: BleAction) {
        when (action) {
            BleAction.StartScan -> viewModelScope.launch { scanner.startScan() }
            is BleAction.Connect -> viewModelScope.launch { connector.connect(action.device) }
            BleAction.SendWashDone -> {
                connector.sendData("WASH_DONE")
                if (connector.connectionState.value != BleConnectionState.CONNECTED) {
                    viewModelScope.launch { _events.send(BleEvent.ShowMessage("Спершу підключіться до пристрою")) }
                }
            }
        }
    }
}