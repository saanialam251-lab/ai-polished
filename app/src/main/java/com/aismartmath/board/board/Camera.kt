package com.aismartmath.board.board

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Infinite board camera. Port of the web version's U, U0, Z, ox, oy, zoomAt, SNf.
 * World units: 1 unit = [u] pixels. Zoom z runs 0.01 .. 500 (1% .. 50,000%).
 */
class Camera {
    /** Screen density, so "30 px" in the web version means 30 dp here. */
    var density = 1.0

    var w by mutableDoubleStateOf(0.0)
    var h by mutableDoubleStateOf(0.0)
    var z by mutableDoubleStateOf(1.0)
    var ox by mutableDoubleStateOf(0.0)
    var oy by mutableDoubleStateOf(0.0)

    /** Web: U0 = min(W/11, H/9) - at 100% about 11 units fit across the board. */
    val u0: Double get() = if (w > 0.0 && h > 0.0) minOf(w / 11.0, h / 9.0) else 30.0 * density
    val u: Double get() = u0 * z

    fun sx(x: Double): Float = (w / 2 + ox + x * u).toFloat()
    fun sy(y: Double): Float = (h / 2 + oy - y * u).toFloat()
    fun wx(px: Float): Double = (px - w / 2 - ox) / u
    fun wy(py: Float): Double = -(py - h / 2 - oy) / u

    fun pan(dx: Float, dy: Float) {
        ox += dx
        oy += dy
    }

    /** Zoom by factor k keeping the world point under screen (rx, ry) fixed. */
    fun zoomAt(rx: Double, ry: Double, k: Double) {
        if (!k.isFinite() || k <= 0.0) return
        val wx = (rx - w / 2 - ox) / u
        val wy = -(ry - h / 2 - oy) / u
        z = (z * k).coerceIn(0.01, 500.0)
        ox = rx - w / 2 - wx * u
        oy = ry - h / 2 + wy * u
    }

    /** Nice step 1/2/5/10 x 10^n that is at least minPx pixels wide on screen. */
    private fun niceStep(minPx: Double): Double {
        val r = minPx / u
        val p = 10.0.pow(floor(log10(r)))
        return listOf(1.0, 2.0, 5.0, 10.0).map { it * p }.first { it >= r }
    }

    /** Grid line spacing in world units (web: 30/U). */
    val gridStep: Double get() = niceStep(30.0 * density)

    /** Snap spacing in world units (web: SNf, 10/U). */
    val snapStep: Double get() = niceStep(10.0 * density)

    // ---- animated moves (the web version jumped instantly) ----
    suspend fun animateZoomAt(rx: Double, ry: Double, k: Double) {
        var prev = 0f
        animate(0f, 1f, animationSpec = tween(220, easing = FastOutSlowInEasing)) { t, _ ->
            zoomAt(rx, ry, k.pow((t - prev).toDouble()))
            prev = t
        }
    }

    suspend fun animateReset(oyTarget: Double) {
        val z0 = z
        val x0 = ox
        val y0 = oy
        animate(0f, 1f, animationSpec = tween(350, easing = FastOutSlowInEasing)) { t, _ ->
            z = z0 + (1.0 - z0) * t
            ox = x0 * (1.0 - t)
            oy = y0 + (oyTarget - y0) * t
        }
    }
}
