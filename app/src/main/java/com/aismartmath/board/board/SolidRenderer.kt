package com.aismartmath.board.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.aismartmath.board.math.Solid
import com.aismartmath.board.math.solidGeo
import com.aismartmath.board.model.Fig
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** A projected corner: rotated x, y, depth z, and where it lands on the screen. */
class P3(val x: Double, val y: Double, val z: Double, val sx: Float, val sy: Float)

/** Web: proj(s) - turns the corners of a solid into screen points, with a little perspective. */
class SolidProj(
    val g: Solid,
    val ox: Float,
    val oy: Float,
    private val cy: Double,
    private val sn: Double,
    private val cr: Double,
    private val sr: Double,
    private val u: Double,
) {
    fun at(x: Double, y: Double, z: Double): P3 {
        val x1 = x * cy + z * sn
        val z1 = -x * sn + z * cy
        val y1 = y * cr - z1 * sr
        val z2 = y * sr + z1 * cr
        val pf = 1 / (1 - z2 / 14)
        return P3(x1, y1, z2, (ox + x1 * pf * u).toFloat(), (oy - y1 * pf * u).toFloat())
    }

    val r: List<P3> = g.v.map { at(it[0], it[1], it[2]) }
}

fun projectSolid(cam: Camera, s: Fig): SolidProj {
    val g = solidGeo(s)
    return SolidProj(
        g, cam.sx(s.p[0].x), cam.sy(s.p[0].y),
        cos(s.ry), sin(s.ry), cos(s.rx), sin(s.rx), cam.u,
    )
}

/** Screen box (left, top, right, bottom) around a solid - used for taps and for the selection outline. */
fun solidBox(cam: Camera, s: Fig): FloatArray {
    val pj = projectSolid(cam, s)
    if (s.sk == "sph") {
        val rr = ((s.sd["r"] ?: 1.0) * cam.u).toFloat()
        return floatArrayOf(pj.ox - rr, pj.oy - rr, pj.ox + rr, pj.oy + rr)
    }
    var x0 = Float.MAX_VALUE
    var y0 = Float.MAX_VALUE
    var x1 = -Float.MAX_VALUE
    var y1 = -Float.MAX_VALUE
    for (q in pj.r) {
        if (q.sx < x0) x0 = q.sx
        if (q.sx > x1) x1 = q.sx
        if (q.sy < y0) y0 = q.sy
        if (q.sy > y1) y1 = q.sy
    }
    return floatArrayOf(x0, y0, x1, y1)
}

private fun avgZ(pj: SolidProj, q: IntArray): Double {
    var z = 0.0
    for (i in q) z += pj.r[i].z
    return z / q.size
}

/** Web: drawSol - shaded faces (far to near), edges, corner letters and the optional helper lines. */
fun DrawScope.drawSolid(cam: Camera, m: TextMeasurer, s: Fig, a: Float) {
    if (a <= 0f || s.p.isEmpty()) return
    val c = s.col ?: figColor("sol")
    val pj = projectSolid(cam, s)
    val d = s.sd
    val wire = "wire" in s.sflags
    val o = Offset(pj.ox, pj.oy)

    if (s.sk == "sph") {
        val rr = ((d["r"] ?: 1.0) * cam.u).toFloat()
        if (!wire) drawCircle(c.copy(alpha = 0.2f * a), rr, o, style = Fill)
        drawCircle(c.copy(alpha = a), rr, o, style = Stroke(2.dp.toPx()))
    }

    if (!wire) {
        val order = pj.g.f.sortedBy { avgZ(pj, it) }
        for (q in order) {
            if (q.size < 3) continue
            val ra = pj.r[q[0]]
            val rb = pj.r[q[1]]
            val re = pj.r[q[2]]
            val ux = rb.x - ra.x
            val uy = rb.y - ra.y
            val uz = rb.z - ra.z
            val wx = re.x - ra.x
            val wy = re.y - ra.y
            val wz = re.z - ra.z
            val nx = uy * wz - uz * wy
            val ny = uz * wx - ux * wz
            val nz = ux * wy - uy * wx
            val nl = hypot(hypot(nx, ny), nz).let { if (it == 0.0) 1.0 else it }
            val sh = (abs(nz) / nl).toFloat()
            val path = Path()
            q.forEachIndexed { j, i ->
                if (j == 0) path.moveTo(pj.r[i].sx, pj.r[i].sy) else path.lineTo(pj.r[i].sx, pj.r[i].sy)
            }
            path.close()
            drawPath(path, c.copy(alpha = a * (0.16f + 0.32f * sh)), style = Fill)
        }
    }
    for (e in pj.g.e) {
        drawLine(
            c.copy(alpha = a),
            Offset(pj.r[e[0]].sx, pj.r[e[0]].sy),
            Offset(pj.r[e[1]].sx, pj.r[e[1]].sy),
            2.dp.toPx(),
        )
    }

    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
    fun ln(p: P3, q: P3, col: Color, text: String) {
        drawLine(col.copy(alpha = a), Offset(p.sx, p.sy), Offset(q.sx, q.sy), 2.5.dp.toPx(), StrokeCap.Round, dash)
        outlinedText(m, text, (p.sx + q.sx) / 2 + 16.dp.toPx(), (p.sy + q.sy) / 2, col, 12f, a)
    }
    fun gk(k: String): Double = d[k] ?: 1.0
    val hh = if (s.sk == "cube") gk("a") else gk("h")

    // corner letters
    if (s.sk in setOf("cube", "cuboid", "pyr", "prism") && "nolab" !in s.sflags) {
        val letters = "ABCDEFGH"
        pj.r.forEachIndexed { i, q ->
            val dx = q.sx - pj.ox
            val dy = q.sy - pj.oy
            val l = hypot(dx, dy).let { if (it == 0f) 1f else it }
            drawCircle(c.copy(alpha = a), 4.dp.toPx(), Offset(q.sx, q.sy))
            outlinedText(m, letters[i].toString(), q.sx + dx / l * 16.dp.toPx(), q.sy + dy / l * 16.dp.toPx(), c, 13f, a)
        }
    }
    val red = Color(0xFFF87171)
    if ("diag" in s.sflags) {
        when (s.sk) {
            "cube" -> ln(pj.r[0], pj.r[6], red, "d=" + fmt(hypot(hypot(gk("a"), gk("a")), gk("a"))))
            "cuboid" -> ln(pj.r[0], pj.r[6], red, "d=" + fmt(hypot(hypot(gk("l"), gk("w")), gk("h"))))
            "pyr" -> ln(pj.r[0], pj.r[2], red, "d=" + fmt(hypot(gk("l"), gk("w"))))
            "sph" -> ln(P3(0.0, 0.0, 0.0, pj.ox, pj.oy), P3(0.0, 0.0, 0.0, pj.ox + (gk("r") * cam.u).toFloat(), pj.oy), red, "r=" + fmt(gk("r")))
            "cyl", "cone" -> ln(pj.at(0.0, -gk("h") / 2, 0.0), pj.at(gk("r"), -gk("h") / 2, 0.0), red, "r=" + fmt(gk("r")))
        }
    }
    if ("hgt" in s.sflags && s.sk != "sph") {
        ln(pj.at(0.0, -hh / 2, 0.0), pj.at(0.0, hh / 2, 0.0), Color(0xFF34D399), "h=" + fmt(hh))
    }
    if ("axes" in s.sflags) {
        val lx = (d.values.maxOrNull() ?: 1.0) * 0.9 + 0.5
        val axes = listOf(
            Triple(doubleArrayOf(1.0, 0.0, 0.0), Color(0xFFF87171), "x"),
            Triple(doubleArrayOf(0.0, 1.0, 0.0), Color(0xFF34D399), "y"),
            Triple(doubleArrayOf(0.0, 0.0, 1.0), Color(0xFF60A5FA), "z"),
        )
        for ((dir, col, name) in axes) {
            val b = pj.at(dir[0] * lx, dir[1] * lx, dir[2] * lx)
            drawLine(col.copy(alpha = a), o, Offset(b.sx, b.sy), 2.dp.toPx())
            outlinedText(m, name, b.sx + 8.dp.toPx(), b.sy - 8.dp.toPx(), col, 13f, a)
        }
    }
}

/** The selected solid: a marching dashed box around it. */
fun DrawScope.drawSolidSelection(cam: Camera, s: Fig, phase: Float, pulse: Float, active: Boolean) {
    val b = solidBox(cam, s)
    val pad = 8.dp.toPx()
    val col = if (active) Color(0xFFFBBF24) else androidx.compose.ui.graphics.lerp(Color.White, s.col ?: figColor("sol"), pulse)
    val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), phase.dp.toPx())
    drawRect(
        col,
        Offset(b[0] - pad, b[1] - pad),
        Size(max(1f, b[2] - b[0] + 2 * pad), max(1f, b[3] - b[1] + 2 * pad)),
        style = Stroke(2.dp.toPx(), pathEffect = dash),
    )
}
