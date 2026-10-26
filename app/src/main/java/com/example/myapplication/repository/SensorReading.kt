package com.example.myapplication.repository

/**
 * Модель за ТЗ лабораторної №3: id пристрою, timestamp, value (значення сенсора), type.
 * X/Y/Z додані окремо, бо вони потрібні для SensorPayload (формат хмари), а value —
 * це "підсумкове" скалярне значення (у нас — intensity), яке зручно показувати і логувати.
 */
data class SensorReading(
    val deviceId: String,
    val type: String,
    val value: Float,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
)

/** Перетворення доменної моделі у формат дроту (JSON), який очікує CloudApi. */
fun SensorReading.toPayload(): SensorPayload = SensorPayload(
    sensorType = type,
    x = x, y = y, z = z,
    timestamp = timestamp
)