package com.example.myapplication.wash

import kotlin.math.min
import kotlin.math.sqrt

/** Фази автомата таймера. Назва WashPhase, а не WashState, щоб не плутати
 *  з WashState — класом екранного стану в WashContract.kt (конвенція MVI). */
enum class WashPhase { IDLE, WASHING, DONE }

/** Пороги (threshold), за яких система переходить між фазами. */
object WashThresholds {
    const val GRAVITY_ALPHA = 0.8f      // коефіцієнт low-pass фільтра гравітації
    const val ACTIVE_ACCEL = 1.5f       // м/с²: поріг різкого поштовху, що активує таймер
    const val TARGET_WASH_MS = 20_000L  // тривалість миття (ВООЗ) — 20 с
}

/** Знімок результату одного семпла — те, що ViewModel перекладає у WashState. */
data class WashSnapshot(
    val phase: WashPhase = WashPhase.IDLE,
    val x: Float = 0f, val y: Float = 0f, val z: Float = 0f,
    val intensity: Float = 0f,
    val washedMs: Long = 0L
)

/**
 * Чиста логіка без залежностей від Android — легко тестується окремо.
 * Один різкий поштовх (перетин порогу знизу вгору) активує таймер,
 * далі він рахується за годинником незалежно від подальшого руху.
 */
class HandWashEngine {
    private val gravity = FloatArray(3)
    private var initialized = false
    private var phase = WashPhase.IDLE
    private var wasAbove = false
    private var startMs = 0L

    fun onSample(x: Float, y: Float, z: Float, nowMs: Long): WashSnapshot {
        if (!initialized) {
            gravity[0] = x; gravity[1] = y; gravity[2] = z; initialized = true
        }
        val a = WashThresholds.GRAVITY_ALPHA
        gravity[0] = a * gravity[0] + (1 - a) * x
        gravity[1] = a * gravity[1] + (1 - a) * y
        gravity[2] = a * gravity[2] + (1 - a) * z
        val lx = x - gravity[0]; val ly = y - gravity[1]; val lz = z - gravity[2]
        val intensity = sqrt(lx * lx + ly * ly + lz * lz)
        val above = intensity > WashThresholds.ACTIVE_ACCEL

        if (phase == WashPhase.IDLE && above && !wasAbove) {
            phase = WashPhase.WASHING
            startMs = nowMs
        }
        wasAbove = above

        var washed = 0L
        if (phase != WashPhase.IDLE) {
            washed = min(nowMs - startMs, WashThresholds.TARGET_WASH_MS)
            if (washed >= WashThresholds.TARGET_WASH_MS) phase = WashPhase.DONE
        }
        return WashSnapshot(phase, x, y, z, intensity, washed)
    }

    fun reset() {
        phase = WashPhase.IDLE; wasAbove = false; startMs = 0L
    }
}