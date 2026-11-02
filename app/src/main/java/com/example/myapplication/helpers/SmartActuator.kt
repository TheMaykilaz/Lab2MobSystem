package com.example.myapplication.helpers

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Обгортка над апаратними виконавчими пристроями (актуаторами): вібрація і ліхтарик.
 * Перед кожним викликом перевіряє апаратну можливість (вимога методички —
 * hasSystemFeature) і ніколи не кидає виняток, якщо фічі немає — просто логує й виходить.
 */
class SmartActuator(private val context: Context) {

    private val vibratorInstance: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private val hasVibrator: Boolean = vibratorInstance?.hasVibrator() == true

    private val hasFlash: Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    private val vibrator: Vibrator? = vibratorInstance

    private val cameraManager: CameraManager? =
        if (!hasFlash) null else context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    /** Id задньої камери з ліхтариком — визначається один раз, лінивим способом. */
    private val torchCameraId: String? by lazy {
        if (!hasFlash) return@lazy null
        cameraManager?.cameraIdList?.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    /** "Сирена": короткий вібровідгук, що імітує тривогу. */
    fun triggerAlarm() {
        Log.i("SmartActuator", "[АКТУАТОР] Сирена (вібрація) активована: час вийшов, руки помиті!")
        if (!hasVibrator || vibrator == null) {
            Log.w("SmartActuator", "Фізичний вібратор відсутній (емулюється в логах)")
            return
        }
        val pattern = longArrayOf(0, 200, 100, 200, 100, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    /** "Прожектор": увімкнути/вимкнути ліхтарик. */
    fun toggleFlashlight(on: Boolean) {
        Log.i("SmartActuator", "[АКТУАТОР] Ліхтарик: ${if (on) "УВІМКНЕНО (час вийшов, руки помиті)" else "вимкнено"}")
        val id = torchCameraId
        if (!hasFlash || cameraManager == null || id == null) {
            Log.w("SmartActuator", "Фізичний спалах/ліхтарик відсутній (емулюється в логах)")
            return
        }
        try {
            cameraManager.setTorchMode(id, on)
        } catch (e: Exception) {
            Log.e("SmartActuator", "Не вдалось керувати ліхтариком: ${e.message}")
        }
    }
}