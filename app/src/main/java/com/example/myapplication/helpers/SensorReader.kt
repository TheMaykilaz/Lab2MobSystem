package com.example.myapplication.helpers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Єдине місце, що знає про SensorManager. ViewModel ніколи не працює з
 * Android API напряму — лише викликає readings() і отримує Flow.
 * registerListener() викликається при підписці на Flow, unregisterListener() —
 * автоматично при скасуванні (Job.cancel() у ViewModel).
 */
class SensorReader(context: Context, private val sensorType: Int = Sensor.TYPE_ACCELEROMETER) {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = manager.getDefaultSensor(sensorType)

    fun readings(): Flow<FloatArray> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                trySend(e.values.clone())
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { manager.unregisterListener(listener) } // енергозбереження
    }
}