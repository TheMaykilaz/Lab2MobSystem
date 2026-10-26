package com.example.myapplication.repository

import com.example.myapplication.helpers.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * ViewModel ніколи не викликає Retrofit напряму — лише repository.save().
 * Репозиторій сам вирішує, куди і як покласти дані; якщо згодом додасться
 * Room (Л.Р. №4+), зміниться тільки цей файл, ViewModel і Screen — ні.
 */
class SensorRepository(
    private val api: CloudApi,
    private val network: NetworkMonitor
) {
    suspend fun save(reading: SensorReading): Result<Unit> = withContext(Dispatchers.IO) {
        if (!network.isOnline()) {
            return@withContext Result.failure(IOException("Немає мережі"))
        }
        runCatching {
            val response = api.upload(reading.toPayload())
            if (!response.isSuccessful) error("HTTP ${response.code()}")
        }
    }
}