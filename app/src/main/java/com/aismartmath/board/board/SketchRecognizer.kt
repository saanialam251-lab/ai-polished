package com.aismartmath.board.board

import androidx.compose.ui.geometry.Offset
import com.aismartmath.board.math.angDeg
import com.aismartmath.board.model.Pt
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Web: rdp - Ramer-Douglas-Peucker line simplification. */
private fun rdp(p: List<Offset>, e: Float): List<Offset> {
    if (p.size < 3) return p
    var mx = 0f
    var ix = 0
    val a = p.first()
    val b = p.last()
    val dx = b.x - a.x
    val dy = b.y - a.y
    val l = hypot(dx, dy).let { if (it == 0f) 1f else it }
    for (i in 1 until p.size - 1) {
        val d = abs(dy * (p[i].x - a.x) - dx * (p[i].y - a.y)) / l
        if (d > mx) {
            mx = d
            ix = i
        }
    }
    return if (mx > e) rdp(p.subList(0, ix + 1), e).dropLast(1) + rdp(p.subList(ix, p.size), e) else listOf(a, b)
}

/**
 * Web: recog - turns a rough stroke (screen points) into a neat figure:
 * line, triangle, rectangle, quadrilateral or circle. Returns the figure type and its snapped points.
 */
fun recognize(pts: List<Offset>, st: BoardState): Pair<String, List<Pt>>? {
    val n = pts.size
    if (n < 6) return null
    var len = 0f
    var x0 = Float.MAX_VALUE
    var x1 = -Float.MAX_VALUE
    var y0 = Float.MAX_VALUE
    var y1 = -Float.MAX_VALUE
    pts.forEachIndexed { i, p ->
        if (i > 0) len += hypot(p.x - pts[i - 1].x, p.y - pts[i - 1].y)
        x0 = min(x0, p.x)
        x1 = max(x1, p.x)
        y0 = min(y0, p.y)
        y1 = max(y1, p.y)
    }
    val dg = hypot(x1 - x0, y1 - y0)
    if (dg < 30f * st.camera.density.toFloat()) return null
    val closed = hypot(pts[0].x - pts[n - 1].x, pts[0].y - pts[n - 1].y) < 0.22f * len
    val q = st.camera.snapStep
    val ep = 0.07f * dg

    val s: MutableList<Offset> = if (closed) {
        var fi = 0
        var fd = 0f
        pts.forEachIndexed { i, p ->
            val d = hypot(p.x - pts[0].x, p.y - pts[0].y)
            if (d > fd) {
                fd = d
                fi = i
            }
        }
        (rdp(pts.subList(0, fi + 1), ep).dropLast(1) + rdp(pts.subList(fi, n), ep)).toMutableList()
    } else {
        rdp(pts, ep).toMutableList()
    }

    if (!closed) return if (s.size == 2) "seg" to listOf(st.snap(s[0]), st.snap(s[1])) else null

    if (hypot(s.first().x - s.last().x, s.first().y - s.last().y) < 0.12f * dg) s.removeAt(s.lastIndex)
    if (s.size == 3) return "tri" to s.map { st.snap(it) }
    if (s.size == 4) {
        val pq = s.map { st.snap(it) }
        val an = pq.mapIndexed { i, p -> angDeg(p, pq[(i + 3) % 4], pq[(i + 1) % 4]) }
        val square = an.all { abs(it - 90) < 16 } && pq.indices.all { i ->
            val p = pq[i]
            val r = pq[(i + 1) % 4]
            val dx = abs(r.x - p.x)
            val dy = abs(r.y - p.y)
            min(dx, dy) < 0.3 * max(dx, dy)
        }
        if (square) {
            val xs = pq.map { it.x }
            val ys = pq.map { it.y }
            return "rect" to listOf(Pt(xs.min(), ys.min()), Pt(xs.max(), ys.max()))
        }
        return "quad" to pq
    }

    // round shape? every point about the same distance from the centre
    val cx = (pts.sumOf { it.x.toDouble() } / n).toFloat()
    val cy = (pts.sumOf { it.y.toDouble() } / n).toFloat()
    val r = pts.map { hypot(it.x - cx, it.y - cy) }
    val m = (r.sumOf { it.toDouble() } / n).toFloat()
    val sd = sqrt(r.sumOf { ((it - m) * (it - m)).toDouble() } / n).toFloat()
    // clearly corner-ed five or six sided shapes are pentagons and hexagons
    if ((s.size == 5 || s.size == 6) && m > 0f && sd / m >= 0.1f) {
        return (if (s.size == 5) "pent" else "hex") to s.map { st.snap(it) }
    }
    if (m > 0f && sd / m < 0.2f) {
        val cw = st.snap(Offset(cx, cy))
        val rad = max(q, floor(m / st.camera.u / q + 0.5) * q)
        return "circ" to listOf(cw, Pt(cw.x + rad, cw.y))
    }
    return null
}
