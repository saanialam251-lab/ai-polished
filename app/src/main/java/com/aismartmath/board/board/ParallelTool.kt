package com.aismartmath.board.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Pt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Two parallel lines [a] and [b] cut by the transversal [tr]. Port of the web geoPar / findPar. */
class ParGeo(
    val d: Pt,
    val t: Pt,
    val xs: List<Pt>,
    val al: Double,
    val v: DoubleArray,
    val a: Fig,
    val b: Fig,
    val tr: Fig,
)

val PXG: Map<String, List<List<Int>>> = mapOf(
    "cor" to listOf(listOf(1, 5), listOf(2, 6), listOf(3, 7), listOf(4, 8)),
    "alt" to listOf(listOf(3, 6), listOf(4, 5)),
    "ext" to listOf(listOf(1, 8), listOf(2, 7)),
    "co" to listOf(listOf(3, 5), listOf(4, 6)),
    "vert" to listOf(listOf(1, 4), listOf(2, 3), listOf(5, 8), listOf(6, 7)),
)

val PXN: Map<String, String> = mapOf(
    "cor" to "Corresponding angles",
    "alt" to "Alternate interior angles",
    "ext" to "Alternate exterior angles",
    "co" to "Co-interior angles",
    "vert" to "Vertically opposite angles",
)

val PXC = listOf(Color(0xFFF472B6), Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFF60A5FA))

fun geoPar(a: Fig, b: Fig, t: Fig): ParGeo? {
    val pa = a.p
    val pb = b.p
    val pt = t.p
    var dx = pa[1].x - pa[0].x
    var dy = pa[1].y - pa[0].y
    val l = hypot(dx, dy)
    if (l == 0.0) return null
    dx /= l
    dy /= l
    if (dx < -1e-12 || (abs(dx) < 1e-12 && dy < 0)) {
        dx = -dx
        dy = -dy
    }
    var nx = -dy
    var ny = dx
    if (ny < -1e-12 || (abs(ny) < 1e-12 && nx < 0)) {
        nx = -nx
        ny = -ny
    }
    fun inter(q: List<Pt>): Pt? {
        val rx = pt[1].x - pt[0].x
        val ry = pt[1].y - pt[0].y
        val qx = q[1].x - q[0].x
        val qy = q[1].y - q[0].y
        val den = rx * qy - ry * qx
        if (den == 0.0) return null
        val u = ((q[0].x - pt[0].x) * qy - (q[0].y - pt[0].y) * qx) / den
        return Pt(pt[0].x + u * rx, pt[0].y + u * ry)
    }
    val x1 = inter(pa) ?: return null
    val x2 = inter(pb) ?: return null
    var tx = pt[1].x - pt[0].x
    var ty = pt[1].y - pt[0].y
    val tl = hypot(tx, ty)
    if (tl == 0.0) return null
    tx /= tl
    ty /= tl
    if (tx * nx + ty * ny < 0) {
        tx = -tx
        ty = -ty
    }
    val top = if (x1.x * nx + x1.y * ny >= x2.x * nx + x2.y * ny) listOf(x1, x2) else listOf(x2, x1)
    val al = acos((tx * dx + ty * dy).coerceIn(-1.0, 1.0)) * 180 / PI
    val v = doubleArrayOf(0.0, 180 - al, al, al, 180 - al, 180 - al, al, al, 180 - al)
    return ParGeo(Pt(dx, dy), Pt(tx, ty), top, al, v, a, b, t)
}

/** Web: findPar - look through the lines for two parallel ones and a third line crossing them. */
fun findPar(segs: List<Fig>): ParGeo? {
    for (i in segs.indices) {
        for (j in i + 1 until segs.size) {
            val a = segs[i].p
            val b = segs[j].p
            val dax = a[1].x - a[0].x
            val day = a[1].y - a[0].y
            val dbx = b[1].x - b[0].x
            val dby = b[1].y - b[0].y
            if (abs(dax * dby - day * dbx) > 1e-9 * hypot(dax, day) * hypot(dbx, dby) + 1e-12) continue
            if (abs(dax * (b[0].y - a[0].y) - day * (b[0].x - a[0].x)) < 1e-9) continue
            for (tr in segs) {
                if (tr === segs[i] || tr === segs[j]) continue
                val t = tr.p
                if (abs((t[1].x - t[0].x) * day - (t[1].y - t[0].y) * dax) < 1e-9) continue
                return geoPar(segs[i], segs[j], tr)
            }
        }
    }
    return null
}

fun segDist(p: Pt, a: Pt, b: Pt): Double {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val l2 = dx * dx + dy * dy
    val t = if (l2 == 0.0) 0.0 else (((p.x - a.x) * dx + (p.y - a.y) * dy) / l2).coerceIn(0.0, 1.0)
    return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
}

/** Where the line t0->t1 cuts the segment s0->s1: (position along t, position along s), or null when they are parallel. */
private fun cutAt(t0: Pt, t1: Pt, s0: Pt, s1: Pt): Pair<Double, Double>? {
    val rx = t1.x - t0.x
    val ry = t1.y - t0.y
    val qx = s1.x - s0.x
    val qy = s1.y - s0.y
    val den = rx * qy - ry * qx
    if (abs(den) < 1e-12 * (hypot(rx, ry) * hypot(qx, qy) + 1e-12)) return null
    val wx = s0.x - t0.x
    val wy = s0.y - t0.y
    val u = (wx * qy - wy * qx) / den
    val v = (wx * ry - wy * rx) / den
    return u to v
}

/**
 * Every (pair of parallel lines + a line that really cuts through both of them). A line that only touches the
 * parallel lines at its own end (like the side of a triangle meeting them at a corner) is not a transversal.
 */
fun findAllPar(segs: List<Fig>): List<ParGeo> {
    val out = mutableListOf<ParGeo>()
    val eps = 0.02
    for (i in segs.indices) {
        for (j in i + 1 until segs.size) {
            val a = segs[i].p
            val b = segs[j].p
            val dax = a[1].x - a[0].x
            val day = a[1].y - a[0].y
            val dbx = b[1].x - b[0].x
            val dby = b[1].y - b[0].y
            if (hypot(dax, day) == 0.0 || hypot(dbx, dby) == 0.0) continue
            if (abs(dax * dby - day * dbx) > 1e-9 * hypot(dax, day) * hypot(dbx, dby) + 1e-12) continue
            if (abs(dax * (b[0].y - a[0].y) - day * (b[0].x - a[0].x)) < 1e-9 * hypot(dax, day) + 1e-12) continue
            for (tr in segs) {
                if (tr === segs[i] || tr === segs[j]) continue
                val t = tr.p
                if (hypot(t[1].x - t[0].x, t[1].y - t[0].y) == 0.0) continue
                val ca = cutAt(t[0], t[1], a[0], a[1]) ?: continue
                val cb = cutAt(t[0], t[1], b[0], b[1]) ?: continue
                // it must pass through the inside of the transversal and land on both parallel lines
                if (ca.first <= eps || ca.first >= 1 - eps || cb.first <= eps || cb.first >= 1 - eps) continue
                if (ca.second < -eps || ca.second > 1 + eps || cb.second < -eps || cb.second > 1 + eps) continue
                val g = geoPar(segs[i], segs[j], tr) ?: continue
                out.add(g)
                if (out.size >= 16) return out
            }
        }
    }
    return out
}

private fun pxCol(g: ParGeo, k: Int, mode: String, hl: Int): Color {
    val v = g.v
    if (hl != 0) {
        return if (abs(v[k] - v[hl]) < 1e-6) Color(0xFFFBBF24)
        else if (abs(v[k] + v[hl] - 180) < 1e-6) Color(0xFF60A5FA)
        else Color(0xFF475569)
    }
    if (mode == "all") return if (abs(v[k] - g.al) < 1e-6) Color(0xFF60A5FA) else Color(0xFFF472B6)
    val i = PXG[mode]?.indexOfFirst { k in it } ?: -1
    return if (i < 0) Color(0xFF475569) else PXC[i % 4]
}

/** Web: drawPX - the eight numbered angles at the two crossings (thin, crisp wedges). */
fun DrawScope.drawParallel(cam: Camera, m: TextMeasurer, g: ParGeo, mode: String, hl: Int, bis: Boolean, light: Boolean = false) {
    for (k in 1..8) {
        val xp = g.xs[if (k > 4) 1 else 0]
        val j = (k - 1) % 4
        val vx = if (j < 2) g.t.x else -g.t.x
        val vy = if (j < 2) g.t.y else -g.t.y
        val hx = if (j % 2 == 1) g.d.x else -g.d.x
        val hy = if (j % 2 == 1) g.d.y else -g.d.y
        val a1 = atan2(-vy, vx).toFloat()
        val a2 = atan2(-hy, hx).toFloat()
        val px = cam.sx(xp.x)
        val py = cam.sy(xp.y)
        val c = pxCol(g, k, mode, hl)
        wedgeFill(px, py, a1, a2, 20.dp.toPx(), c, 0.22f, 1.5f)
        var bx = cos(a1) + cos(a2)
        var by = sin(a1) + sin(a2)
        val bl = hypot(bx, by).let { if (it == 0f) 1f else it }
        bx /= bl
        by /= bl
        if (light) {
            outlinedText(m, fmt(g.v[k], 0) + "°", px + bx * 34.dp.toPx(), py + by * 34.dp.toPx(), Color(0xFFCBD5E1), 9f)
            continue
        }
        outlinedText(m, "$k", px + bx * 32.dp.toPx(), py + by * 32.dp.toPx(), c, 12f)
        outlinedText(m, fmt(g.v[k], 0) + "°", px + bx * 48.dp.toPx(), py + by * 48.dp.toPx(), Color(0xFFCBD5E1), 9f)
        if (bis) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))
            drawLine(
                Color(0xFFF0ABFC),
                Offset(px, py),
                Offset(px + bx * 70.dp.toPx(), py + by * 70.dp.toPx()),
                1.5.dp.toPx(),
                pathEffect = dash,
            )
        }
    }
}

private fun relName(hl: Int, j: Int): String {
    for (gk in listOf("cor", "alt", "ext", "vert")) {
        if (PXG.getValue(gk).any { hl in it && j in it }) return PXN.getValue(gk)
    }
    return "equal"
}

/** Web: pxText - what the picked angle (or the picked relationship) means. */
fun pxText(g: ParGeo, mode: String, hl: Int): List<Part> {
    val v = g.v
    if (hl != 0) {
        val ids = (1..8).filter { it != hl }
        val eq = ids.filter { abs(v[it] - v[hl]) < 1e-6 }
        val sp = ids.filter { abs(v[it] + v[hl] - 180) < 1e-6 && abs(v[it] - v[hl]) > 1e-6 }
        var s = "∠$hl = ${fmt(v[hl], 1)}° · equal to " + eq.joinToString(", ") { "∠$it (${relName(hl, it)})" }
        if (sp.isNotEmpty()) {
            s += " · supplementary to " + sp.joinToString(", ") { "∠$it" } + " (${fmt(180 - v[hl], 1)}°)"
        }
        return listOf(Part.T(s))
    }
    val gp = PXG[mode]
    if (gp != null) {
        val body = gp.joinToString(", ") { p ->
            if (mode == "co") "∠${p[0]} + ∠${p[1]} = 180°" else "∠${p[0]} = ∠${p[1]} = ${fmt(v[p[0]], 1)}°"
        }
        return listOf(Part.T(PXN.getValue(mode) + ": " + body))
    }
    return listOf(Part.T("Transversal makes ${fmt(g.al, 1)}° and ${fmt(180 - g.al, 1)}° with the parallel lines — tap an angle"))
}

/** Details lines for a line that belongs to a detected set of parallel lines + transversal. */
fun parDetails(f: Fig, g: ParGeo): List<List<Part>> {
    // typed maths, drawn by the maths engine (KaTeX style): capitals for words, italic for variables
    fun lb(w: String) = "\\text{" + w.uppercase().replace("{", "").replace("}", "") + "}"
    fun line(s: String): List<Part> = listOf(Part.T(s))
    val out = mutableListOf<List<Part>>()
    val v = g.v
    if (f === g.tr) {
        out.add(line(lb("Transversal — it crosses both parallel lines")))
    } else if (f === g.a || f === g.b) {
        val other = if (f === g.a) g.b else g.a
        val gap = abs((other.p[0].x - f.p[0].x) * g.d.y - (other.p[0].y - f.p[0].y) * g.d.x)
        out.add(line("${lb("Parallel")} (∥) ${lb("to the other line, distance between them")} ${fmt(gap)}"))
    } else {
        return emptyList()
    }
    out.add(line("${lb("The transversal makes")} ${fmt(g.al, 1)}° ${lb("and")} ${fmt(180 - g.al, 1)}° ${lb("with the parallel lines (angles")} ∠1 ${lb("to")} ∠8${lb(")")}"))
    for (key in listOf("cor", "alt", "ext", "co", "vert")) {
        val gp = PXG.getValue(key)
        val body = gp.joinToString(",  ") { p ->
            if (key == "co") "∠${p[0]} + ∠${p[1]} = 180°" else "∠${p[0]} = ∠${p[1]} = ${fmt(v[p[0]], 1)}°"
        }
        out.add(line(lb(PXN.getValue(key)) + "  " + body))
    }
    return out
}
