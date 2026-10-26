package com.example.myapplication.repository

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/** Формат JSON, що йде в тіло POST-запиту (саме його просили продемонструвати в ТЗ). */
data class SensorPayload(
    @SerializedName("sensorType") val sensorType: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Варіант Б з ТЗ: REST + публічний mock-API замість Firebase, щоб не налаштовувати
 * google-services.json. JSONPlaceholder — стабільний безкоштовний сервіс для навчальних
 * POST-запитів: приймає будь-який JSON і повертає його ж з кодом 201 і присвоєним id.
 */
interface CloudApi {
    @POST("posts")
    suspend fun upload(@Body payload: SensorPayload): Response<SensorPayload>
}

/** Єдине місце створення Retrofit-клієнта. Логування запитів — щоб було що показати
 *  у звіті замість (або на додачу до) App Inspection → Network. */
object CloudApiFactory {
    fun create(): CloudApi {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        return Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudApi::class.java)
    }
}