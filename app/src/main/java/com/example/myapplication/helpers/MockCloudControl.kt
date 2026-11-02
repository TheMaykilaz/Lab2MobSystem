package com.example.myapplication.helpers

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Імітація гілки "control/led_status" у хмарній базі (Firebase Realtime Database
 * у методичці). Оскільки проєкт послідовно використовує варіант Retrofit/REST
 * (Л.Р. №3), а не Firebase SDK, канал дистанційного керування теж емулюється —
 * методичка прямо дозволяє "імітацію через Long Polling/Retrofit".
 *
 * Роль "консолі", де викладач/студент вручну змінює значення, тут виконує
 * кнопка "Імітувати зміну в хмарі" на екрані — додаток її не знає заздалегідь
 * і дізнається про зміну лише через наступний опитувальний запит (fetchCommand),
 * так само як реальний клієнт дізнався б про зміну значення в консолі Firebase.
 */
object MockCloudControl {
    private val _ledCommand = MutableStateFlow(false)
    val ledCommand: StateFlow<Boolean> = _ledCommand.asStateFlow()

    /** Імітує запис у хмару (зміну значення на "консолі"). */
    suspend fun setRemoteCommand(on: Boolean) {
        delay(300) // мережева затримка запису
        _ledCommand.value = on
    }

    /** Імітує один GET-запит у циклі Long Polling. */
    suspend fun fetchCommand(): Boolean {
        delay(200) // мережева затримка читання
        return _ledCommand.value
    }
}