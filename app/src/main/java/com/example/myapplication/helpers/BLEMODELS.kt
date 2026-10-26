package com.example.myapplication.helpers

import java.util.UUID

/**
 * Профіль віртуального BLE-пристрою "Hand Wash Hub".
 * UUID у форматі 16-бітних BLE ідентифікаторів, розширених базовим Bluetooth SIG шаблоном.
 */
object BleProfile {
    /** Сервіс, що об'єднує характеристики пристрою. */
    val SERVICE_UUID: UUID = UUID.fromString("0000af01-0000-1000-8000-00805f9b34fb")

    /** Read/Notify: поточний статус пристрою (увімкнено/вимкнено, рівень сигналу). */
    val STATUS_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000af02-0000-1000-8000-00805f9b34fb")

    /** Write: команди від хаба до пристрою (наприклад, WASH_DONE, RESET). */
    val COMMAND_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000af03-0000-1000-8000-00805f9b34fb")
}

/** Стани з'єднання для State Machine (Scanning -> Connecting -> Connected -> Data Transfer). */
enum class BleConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED }

/** Опис знайденого (віртуального) пристрою. */
data class BleDevice(
    val address: String,
    val name: String,
    val rssi: Int = -55
)