package com.example.myapplication

import kotlin.math.min
import kotlin.math.sqrt

/** Стани таймера миття рук. */
enum class WashState { IDLE, WASHING, DONE }

/** Пороги (threshold), за яких система переходить між станами. */
object Thresholds {
    const val GRAVITY_ALPHA = 0.8f      // коефіцієнт low-pass фільтра гравітації
    const val ACTIVE_ACCEL = 1.5f       // м/с²: поріг різкого поштовху, що активує таймер
    const val TARGET_WASH_MS = 20_000L  // тривалість миття (ВООЗ) — 20 с
}

/** Знімок стану для UI. */
data class WashSnapshot(
    val state: WashState = WashState.IDLE,
    val x: Float = 0f, val y: Float = 0f, val z: Float = 0f,
    val intensity: Float = 0f,
    val washedMs: Long = 0L
) {
    val progress: Float get() = (washedMs.toFloat() / Thresholds.TARGET_WASH_MS).coerceIn(0f, 1f)
}

/** Логіка без залежностей від Android: один різкий поштовх запускає таймер за годинником. */
class HandWashEngine {
    private val gravity = FloatArray(3)
    private var initialized = false
    private var state = WashState.IDLE
    private var wasAbove = false
    private var startMs = 0L

    fun onSample(x: Float, y: Float, z: Float, nowMs: Long): WashSnapshot {
        // 1. Виділяємо гравітацію low-pass фільтром, віднімаємо — лінійне прискорення
        if (!initialized) {
            gravity[0] = x; gravity[1] = y; gravity[2] = z; initialized = true
        }
        val a = Thresholds.GRAVITY_ALPHA
        gravity[0] = a * gravity[0] + (1 - a) * x
        gravity[1] = a * gravity[1] + (1 - a) * y
        gravity[2] = a * gravity[2] + (1 - a) * z
        val lx = x - gravity[0]; val ly = y - gravity[1]; val lz = z - gravity[2]
        val intensity = sqrt(lx * lx + ly * ly + lz * lz)
        val above = intensity > Thresholds.ACTIVE_ACCEL

        // 2. Один різкий поштовх (перетин порогу знизу вгору) одразу запускає таймер
        if (state == WashState.IDLE && above && !wasAbove) {
            state = WashState.WASHING
            startMs = nowMs
        }
        wasAbove = above

        // 3. Таймер рахується за годинником, незалежно від подальшого руху
        var washed = 0L
        if (state != WashState.IDLE) {
            washed = min(nowMs - startMs, Thresholds.TARGET_WASH_MS)
            if (washed >= Thresholds.TARGET_WASH_MS) state = WashState.DONE
        }
        return WashSnapshot(state, x, y, z, intensity, washed)
    }

    fun reset() {
        state = WashState.IDLE; wasAbove = false; startMs = 0L
    }
}