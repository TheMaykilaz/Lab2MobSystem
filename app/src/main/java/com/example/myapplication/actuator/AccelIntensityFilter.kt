package com.example.myapplication.actuator

import kotlin.math.sqrt

/**
 * Той самий прийом, що й у HandWashEngine (Л.Р. №1): low-pass фільтр виділяє
 * гравітаційну складову, різниця з сирим значенням дає лінійне прискорення.
 * Винесено окремо, бо тут не потрібен весь автомат станів таймера — лише інтенсивність.
 */
class AccelIntensityFilter {
    private val gravity = FloatArray(3)
    private var initialized = false

    fun process(x: Float, y: Float, z: Float): Float {
        if (!initialized) {
            gravity[0] = x; gravity[1] = y; gravity[2] = z; initialized = true
        }
        val a = 0.8f
        gravity[0] = a * gravity[0] + (1 - a) * x
        gravity[1] = a * gravity[1] + (1 - a) * y
        gravity[2] = a * gravity[2] + (1 - a) * z
        val lx = x - gravity[0]; val ly = y - gravity[1]; val lz = z - gravity[2]
        return sqrt(lx * lx + ly * ly + lz * lz)
    }
}