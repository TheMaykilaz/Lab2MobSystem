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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppRoot(washVm, bleVm) } }
    }

    override fun onResume() {
        super.onResume()
        washVm.onAction(WashAction.Start) // екран видно -> слухаємо акселерометр
    }

    override fun onPause() {
        washVm.onAction(WashAction.Stop) // екран сховано -> відписуємось, економимо батарею
        super.onPause()
    }
}

/** Перемикач вкладок між екраном Л.Р. №1 (Hand Wash) і Л.Р. №2/3 (BLE). */
@Composable
fun AppRoot(washVm: WashViewModel, bleVm: BleViewModel) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Hand Wash") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("BLE") })
        }
        Box(Modifier.weight(1f)) {
            if (tab == 0) WashScreen(washVm) else BleScreen(bleVm)
        }
    }
}