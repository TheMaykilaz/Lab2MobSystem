package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.myapplication.actuator.ActuatorAction
import com.example.myapplication.actuator.ActuatorScreen
import com.example.myapplication.actuator.ActuatorViewModel
import com.example.myapplication.ble.BleScreen
import com.example.myapplication.ble.BleViewModel
import com.example.myapplication.wash.WashAction
import com.example.myapplication.wash.WashScreen
import com.example.myapplication.wash.WashViewModel

/**
 * Activity нічого не вирішує — лише показує екран і перетворює onResume/onPause
 * на Action для ViewModel (вимога "Екран не містить логіки" з архітектури).
 */
class MainActivity : ComponentActivity() {
    private val washVm: WashViewModel by viewModels()
    private val bleVm: BleViewModel by viewModels()
    private val actuatorVm: ActuatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actuatorVm.attachWashViewModel(washVm)
        setContent { MaterialTheme { AppRoot(washVm, bleVm, actuatorVm) } }
    }

    override fun onResume() {
        super.onResume()
        washVm.onAction(WashAction.Start)
        actuatorVm.onAction(ActuatorAction.Start)
    }

    override fun onPause() {
        washVm.onAction(WashAction.Stop)
        actuatorVm.onAction(ActuatorAction.Stop)
        super.onPause()
    }
}

/** Перемикач вкладок між екранами Л.Р. №1, №2/3 і №4. */
@Composable
fun AppRoot(washVm: WashViewModel, bleVm: BleViewModel, actuatorVm: ActuatorViewModel) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Hand Wash") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("BLE") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Control") })
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> WashScreen(washVm)
                1 -> BleScreen(bleVm)
                else -> ActuatorScreen(actuatorVm)
            }
        }
    }
}