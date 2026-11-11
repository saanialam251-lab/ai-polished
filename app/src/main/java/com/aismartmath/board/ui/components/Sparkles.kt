package com.aismartmath.board.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private class Spark(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val spin: Float,
    val born: Long,
    val ring: Boolean,
)

private val SparkColors = listOf(
    Color(0xFFFBBF24), Color(0xFFF1D3A5), Color(0xFFE8A94B),
    Color(0xFFFFFFFF), Color(0xFFD08A4E), Color(0xFFC68B2C),
)

/**
 * Wraps the whole app: every quick tap (not a drag) throws a burst of glittering stars and a soft ring
 * from the spot you touched. It only watches touches - it never blocks them.
 */
@Composable
fun SparkleHost(enabled: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val sparks = remember { mutableStateListOf<Spark>() }
    var now by remember { mutableLongStateOf(0L) }
    val density = LocalDensity.current.density

    LaunchedEffect(sparks.size) {
        while (sparks.isNotEmpty()) {
            val t = withFrameNanos { it }
            now = t
            sparks.removeAll { t - it.born > 800_000_000L }
        }
    }

    Box(
        modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val start = down.position
                val t0 = System.nanoTime()
                var moved = false
                do {
                    val ev = awaitPointerEvent(PointerEventPass.Initial)
                    val ch = ev.changes.firstOrNull()
                    if (ch != null && (ch.position - start).getDistance() > 14f * density) moved = true
                } while (ev.changes.any { it.pressed })
                if (!moved && System.nanoTime() - t0 < 400_000_000L) {
                    val t = System.nanoTime()
                    now = t
                    if (sparks.size < 60) {
                        val rnd = java.util.Random()
                        sparks.add(Spark(start.x, start.y, 0f, 0f, Color.White, 0f, 0f, t, true))
                        repeat(11) {
                            val ang = rnd.nextFloat() * 2f * PI.toFloat()
                            val speed = (90f + rnd.nextFloat() * 170f) * density
                            sparks.add(
                                Spark(
                                    start.x, start.y,
                                    cos(ang) * speed, sin(ang) * speed - 60f * density,
                                    SparkColors[rnd.nextInt(SparkColors.size)],
                                    (3f + rnd.nextFloat() * 4f) * density,
                                    (rnd.nextFloat() - 0.5f) * 6f,
                                    t, false,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) {
        content()
        Canvas(Modifier.fillMaxSize()) {
            val t = now
            for (s in sparks) {
                val age = max(0f, (t - s.born) / 1e9f)
                if (s.ring) {
                    val p = (age / 0.5f).coerceIn(0f, 1f)
                    drawCircle(
                        Color.White.copy(alpha = (1f - p) * 0.55f),
                        10f * density + 46f * density * p,
                        Offset(s.x, s.y),
                        style = Stroke(2f * density),
                    )
                    continue
                }
                val life = 0.75f
                if (age > life) continue
                val p = age / life
                val x = s.x + s.vx * age
                val y = s.y + s.vy * age + 420f * density * age * age
                val a = (1f - p).coerceIn(0f, 1f)
                val r = s.size * (1f - 0.5f * p)
                val rot = s.spin * age
                val path = Path()
                for (i in 0 until 8) {
                    val ang = i * PI.toFloat() / 4f + rot
                    val rad = if (i % 2 == 0) r else r * 0.38f
                    val px = x + cos(ang) * rad
                    val py = y + sin(ang) * rad
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, s.color.copy(alpha = a), style = Fill)
            }
        }
    }
}
