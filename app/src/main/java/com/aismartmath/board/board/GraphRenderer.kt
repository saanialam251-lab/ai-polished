package com.aismartmath.board.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.aismartmath.board.model.Fig
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow

/** Web: GC - colours given to new graphs in turn. */
val GraphColors = listOf(Color(0xFFFBBF24), Color(0xFFF472B6), Color(0xFF34D399), Color(0xFF60A5FA), Color(0xFFFB923C))

private fun DrawScope.explicitPath(cam: Camera, f: Fig): Path {
    val path = Path()
    var py = Float.NaN
    val step = 2.dp.toPx()
    var i = 0f
    while (i <= size.width) {
        val x = (i - cam.w / 2 - cam.ox) / cam.u
        val y = f.yAt(x)
        if (!y.isFinite() || abs(y) > 1e5) {
            py = Float.NaN
            i += step
            continue
        }
        val yy = (cam.h / 2 + cam.oy - y * cam.u).toFloat()
        if (py.isNaN() || abs(yy - py) > size.height * 3) path.moveTo(i, yy) else path.lineTo(i, yy)
        py = yy
        i += step
    }
    return path
}

/** Parametric and polar curves: many small steps of t joined into a line. */
private fun DrawScope.parametricPath(cam: Camera, f: Fig): Path {
    val path = Path()
    val d = f.gd?.par ?: return path
    val n = 1200
    var pen = false
    for (i in 0..n) {
        val q = f.parAt(d.t0 + (d.t1 - d.t0) * i / n)
        if (q == null) {
            pen = false
            continue
        }
        val sx = cam.sx(q.x)
        val sy = cam.sy(q.y)
        if (!pen) {
            path.moveTo(sx, sy)
            pen = true
        } else {
            path.lineTo(sx, sy)
        }
    }
    return path
}

/** Web: drawImp - marching squares over the visible board to draw curves like x^2 + y^2 = 25. */
private fun DrawScope.implicitPath(cam: Camera, f: Fig): Path {
    val cs = 8.dp.toPx()
    val nx = ceil(size.width / cs).toInt() + 1
    val ny = ceil(size.height / cs).toInt() + 1
    val v = DoubleArray(nx * ny)
    for (j in 0 until ny) for (i in 0 until nx) {
        val x = (i * cs - cam.w / 2 - cam.ox) / cam.u
        val y = -(j * cs - cam.h / 2 - cam.oy) / cam.u
        v[j * nx + i] = f.vAt(x, y)
    }
    val path = Path()
    val ea = intArrayOf(0, 1, 3, 0)
    val eb = intArrayOf(1, 2, 2, 3)
    val cx = FloatArray(4)
    val cy = FloatArray(4)
    val px = FloatArray(4)
    val py = FloatArray(4)
    val vv = DoubleArray(4)
    for (j in 0 until ny - 1) for (i in 0 until nx - 1) {
        vv[0] = v[j * nx + i]
        vv[1] = v[j * nx + i + 1]
        vv[2] = v[(j + 1) * nx + i + 1]
        vv[3] = v[(j + 1) * nx + i]
        if (vv.any { !it.isFinite() }) continue
        cx[0] = i * cs; cx[1] = (i + 1) * cs; cx[2] = (i + 1) * cs; cx[3] = i * cs
        cy[0] = j * cs; cy[1] = j * cs; cy[2] = (j + 1) * cs; cy[3] = (j + 1) * cs
        var n = 0
        for (e in 0 until 4) {
            val a = ea[e]
            val b = eb[e]
            if ((vv[a] > 0) != (vv[b] > 0)) {
                val t = (vv[a] / (vv[a] - vv[b])).toFloat()
                px[n] = cx[a] + t * (cx[b] - cx[a])
                py[n] = cy[a] + t * (cy[b] - cy[a])
                n++
            }
        }
        if (n == 2 || n == 4) {
            path.moveTo(px[0], py[0]); path.lineTo(px[1], py[1])
            if (n == 4) { path.moveTo(px[2], py[2]); path.lineTo(px[3], py[3]) }
        }
    }
    return path
}

/** Web: drawS for graphs. They draw themselves in from the left (k = 0..1), with a soft glow under the line. */
fun DrawScope.drawGraph(cam: Camera, f: Fig, k: Float, a: Float) {
    val g = f.gd ?: return
    if (a <= 0f) return
    val c = f.col ?: GraphColors[0]
    val e = 1f - (1f - k).pow(3)
    clipRect(0f, 0f, size.width * e, size.height) {
        val path = if (g.par != null) parametricPath(cam, f) else if (g.imp) implicitPath(cam, f) else explicitPath(cam, f)
        drawPath(path, c.copy(alpha = a), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Web: the tangent trace - dashed guides to the axes, the red tangent line, a dot and the point's label. */
fun DrawScope.drawTrace(cam: Camera, m: TextMeasurer, f: Fig, x: Double, pulse: Float) {
    val y = f.yAt(x)
    val s = (f.yAt(x + 1e-4) - f.yAt(x - 1e-4)) / 2e-4
    if (!y.isFinite() || !s.isFinite()) return
    val o = Offset(cam.sx(x), cam.sy(y))
    val dash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))
    val guide = Color.White.copy(alpha = 0.6f)
    drawLine(guide, Offset(o.x, cam.sy(0.0)), o, 1.5.dp.toPx(), pathEffect = dash)
    drawLine(guide, Offset(cam.sx(0.0), o.y), o, 1.5.dp.toPx(), pathEffect = dash)
    val p1 = Offset(cam.sx(x - 2), cam.sy(y - 2 * s))
    val p2 = Offset(cam.sx(x + 2), cam.sy(y + 2 * s))
    val red = Color(0xFFF87171)
    drawLine(red, p1, p2, 2.5.dp.toPx(), StrokeCap.Round)
    drawCircle(Color.White.copy(alpha = 0.5f - 0.3f * pulse), (8f + 5f * pulse).dp.toPx(), o, style = Stroke(2.dp.toPx()))
    drawCircle(Color.White, 5.dp.toPx(), o)
    outlinedText(m, "(${fmt(x)}, ${fmt(y)})", o.x, o.y - 16.dp.toPx(), Color.White, 12f)
}
