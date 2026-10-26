package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.helpers.NetworkMonitor
import com.example.myapplication.repository.CloudApiFactory
import com.example.myapplication.repository.SensorRepository

/**
 * Найпростіша "ручна" DI: один SensorRepository на весь додаток (Singleton),
 * щоб усі екрани писали в один і той самий репозиторій, а не кожен у свій.
 * Коли дійде до Hilt/Koin — заміниться лише цей файл.
 */
object AppContainer {
    @Volatile private var repository: SensorRepository? = null

    fun sensorRepository(context: Context): SensorRepository =
        repository ?: synchronized(this) {
            repository ?: SensorRepository(
                api = CloudApiFactory.create(),
                network = NetworkMonitor(context.applicationContext)
            ).also { repository = it }
        }
}