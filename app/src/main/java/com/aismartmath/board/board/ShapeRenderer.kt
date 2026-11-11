package com.aismartmath.board.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.aismartmath.board.math.angDeg
import com.aismartmath.board.math.center
import com.aismartmath.board.math.circumcenter
import com.aismartmath.board.math.dist
import com.aismartmath.board.math.footOn
import com.aismartmath.board.math.incenter
import com.aismartmath.board.math.isCyclic
import com.aismartmath.board.math.isect
import com.aismartmath.board.math.polyArea
import com.aismartmath.board.math.pts
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Pt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

val PointColor = Color(0xFFF87171)
val SegColor = Color(0xFFFBBF24)

/** Web: COL */
fun figColor(t: String): Color = when (t) {
    "pt" -> PointColor
    "seg" -> SegColor
    "tri" -> Color(0xFF818CF8)
    "rtri" -> Color(0xFFA78BFA)
    "quad" -> Color(0xFFF472B6)
    "rect" -> Color(0xFFFB7185)
    "pent" -> Color(0xFFFB923C)
    "hex" -> Color(0xFF22D3EE)
    "trig" -> Color(0xFF22D3EE)
    "unit" -> Color(0xFF34D399)
    "circ" -> Color(0xFF2DD4BF)
    else -> Color(0xFF38BDF8)
}

/** Web: PXC - every angle has its own colour. */
val AngleColors = listOf(Color(0xFFF472B6), Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFF60A5FA))

/** Web: SW - colour swatches. */
val Swatches = listOf(
    Color(0xFF818CF8), Color(0xFFF472B6), Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFF60A5FA),
    Color(0xFFFB923C), Color(0xFFA78BFA), Color(0xFFF87171), Color(0xFF2DD4BF), Color(0xFFE5E7EB),
)

/** Web: pop-in curve (ease-out-back). */
fun easeOutBack(k: Float): Float =
    if (k >= 1f) 1f else 1f + 2.70158f * (k - 1f).pow(3) + 1.70158f * (k - 1f).pow(2)

private fun Float.toDeg(): Float = (this * 180f / PI.toFloat())

private fun aw(p: Offset, q: Offset, d: Float): Offset {
    val dx = p.x - q.x
    val dy = p.y - q.y
    val l = hypot(dx, dy).let { if (it == 0f) 1f else it }
    return Offset(p.x + dx / l * d, p.y + dy / l * d)
}

private fun DrawScope.scr(cam: Camera, p: Pt) = Offset(cam.sx(p.x), cam.sy(p.y))

private fun DrawScope.dot(cam: Camera, p: Pt, c: Color, a: Float, r: Float = 4f) {
    drawCircle(c.copy(alpha = a), r.dp.toPx(), scr(cam, p))
}

private fun DrawScope.seg(cam: Camera, p: Pt, q: Pt, c: Color, w: Float, a: Float, dashed: Boolean = false) {
    drawLine(
        c.copy(alpha = c.alpha * a),
        scr(cam, p),
        scr(cam, q),
        w.dp.toPx(),
        StrokeCap.Round,
        if (dashed) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())) else null,
    )
}

private fun DrawScope.ring(cam: Camera, o: Pt, r: Double, c: Color, w: Float, a: Float) {
    drawCircle(c.copy(alpha = c.alpha * a), (r * cam.u).toFloat(), scr(cam, o), style = Stroke(w.dp.toPx()))
}

/** A thin, crisp angle wedge: light fill, one fine edge line. [rad] in pixels, [strokeW] in dp. */
fun DrawScope.wedgeFill(x: Float, y: Float, a1: Float, a2: Float, rad: Float, c: Color, fillA: Float, strokeW: Float) {
    val tau = 2 * PI.toFloat()
    val dl = (((a2 - a1) % tau) + tau) % tau
    val s1 = if (dl <= PI) a1 else a2
    val sweep = if (dl <= PI) dl else tau - dl
    val tl = Offset(x - rad, y - rad)
    val sz = Size(rad * 2, rad * 2)
    drawArc(c.copy(alpha = fillA), s1.toDeg(), sweep.toDeg(), true, tl, sz, style = Fill)
    drawArc(c, s1.toDeg(), sweep.toDeg(), true, tl, sz, style = Stroke(strokeW.dp.toPx(), join = StrokeJoin.Round))
}

/** Web: arc() - right-angle square or a thin wedge at a vertex, in the vertex's own colour. */
private fun DrawScope.angleMark(cam: Camera, p: Pt, q: Pt, r: Pt, an: Double, c: Color, a: Float, radDp: Float = 12f) {
    val px = cam.sx(p.x)
    val py = cam.sy(p.y)
    val a1 = atan2(cam.sy(q.y) - py, cam.sx(q.x) - px)
    val a2 = atan2(cam.sy(r.y) - py, cam.sx(r.x) - px)
    val rad = radDp.dp.toPx()
    if (abs(an - 90) < 0.6) {
        val ux = cos(a1) * rad * 0.7f
        val uy = sin(a1) * rad * 0.7f
        val wx = cos(a2) * rad * 0.7f
        val wy = sin(a2) * rad * 0.7f
        val path = Path().apply {
            moveTo(px + ux, py + uy)
            lineTo(px + ux + wx, py + uy + wy)
            lineTo(px + wx, py + wy)
        }
        drawPath(path, c.copy(alpha = a), style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
    } else {
        wedgeFill(px, py, a1, a2, rad, c.copy(alpha = a), 0.18f * a, 1.5f)
    }
}

private fun DrawScope.drawPoly(
    cam: Camera, m: TextMeasurer, P: List<Pt>, c: Color, nm: String?, quiet: Boolean, axes: Boolean, a: Float,
    hide: Set<String> = emptySet(), only: Int = -1, skipV: Set<Int> = emptySet(),
) {
    val n = P.size
    if (n < 2) {
        P.firstOrNull()?.let { dot(cam, it, c, a) }
        return
    }
    val path = Path().apply {
        P.forEachIndexed { i, p -> if (i == 0) moveTo(cam.sx(p.x), cam.sy(p.y)) else lineTo(cam.sx(p.x), cam.sy(p.y)) }
        if (n > 2) close()
    }
    if (n > 2) drawPath(path, c.copy(alpha = 0.16f * a), style = Fill)
    drawPath(path, c.copy(alpha = a), style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
    if (quiet) {
        P.forEach { dot(cam, it, c, a) }
        return
    }
    val g = Offset(P.sumOf { cam.sx(it.x).toDouble() }.toFloat() / n, P.sumOf { cam.sy(it.y).toDouble() }.toFloat() / n)
    val names = Nm.of(nm)
    // tiny figures: skip the numbers so the rest stays readable
    var minSide = Float.MAX_VALUE
    for (i in 0 until n) {
        val p = P[i]
        val q = P[(i + 1) % n]
        minSide = minOf(minSide, hypot(cam.sx(p.x) - cam.sx(q.x), cam.sy(p.y) - cam.sy(q.y)))
    }
    val compact = minSide < 52.dp.toPx()
    // "only" = one angle picked with a double tap on its corner: everything else stays quiet
    val iso = n > 2 && only in 0 until n
    for (i in 0 until n) {
        val p = P[i]
        val nx = P[(i + 1) % n]
        dot(cam, p, c, a)
        if (!iso && !compact && "s$i" !in hide && !(n == 2 && i == 1)) {
            val mid = Offset((cam.sx(p.x) + cam.sx(nx.x)) / 2, (cam.sy(p.y) + cam.sy(nx.y)) / 2)
            val lp = aw(mid, g, 11.dp.toPx())
            outlinedText(m, fmt(dist(p, nx)), lp.x, lp.y, Color.White, 10.5f, a)
        }
        if (n > 2) {
            if (iso && i != only) continue
            val q = P[(i + n - 1) % n]
            val an = angDeg(p, q, nx)
            val ps = scr(cam, p)
            val letter = if (i < names.length) names[i] else ""
            val label = letter + if (axes) "(${fmt(p.x)}, ${fmt(p.y)})" else ""
            if ((iso || "v" !in hide) && i !in skipV) {
                val vp = aw(ps, g, (if (axes) 26 else 15).dp.toPx())
                outlinedText(m, label, vp.x, vp.y, c, 12f, a, serif = true)
            }
            val col = AngleColors[i % 4]
            if (iso) {
                // the chosen angle on its own: a larger wedge and a clear number
                angleMark(cam, p, q, nx, an, col, a, 22f)
                val tp = aw(ps, g, (-38).dp.toPx())
                outlinedText(m, fmt(an, 1) + "°", tp.x, tp.y, col, 12.5f, a)
            } else if ("a$i" !in hide) {
                angleMark(cam, p, q, nx, an, col, a)
                if (!compact) {
                    val tp = aw(ps, g, (-27).dp.toPx())
                    outlinedText(m, fmt(an, 0) + "°", tp.x, tp.y, col, 10f, a)
                }
            }
        }
    }
}

/** Centres, circles and helper lines the edit panel can switch on. */
private fun DrawScope.drawOverlays(cam: Camera, m: TextMeasurer, f: Fig, P: List<Pt>, a: Float) {
    val white = Color.White.copy(alpha = 0.7f)
    if (f.t == "circ") {
        if ("dia" in f.ov) {
            val o = f.p[0]
            val e = f.p[1]
            val far = Pt(2 * o.x - e.x, 2 * o.y - e.y)
            seg(cam, far, e, white, 1.5f, a, dashed = true)
            outlinedText(m, "d = " + fmt(2 * dist(o, e)), cam.sx(o.x), cam.sy(o.y) + 14.dp.toPx(), Color(0xFFE5E7EB), 11f, a)
        }
        return
    }
    val n = P.size
    if (n < 3) return

    fun lab(p: Pt, s: String, c: Color) {
        outlinedText(m, s, cam.sx(p.x) + 10.dp.toPx(), cam.sy(p.y) - 10.dp.toPx(), c, 11f, a)
    }

    val mids = List(n) { i -> Pt((P[i].x + P[(i + 1) % n].x) / 2, (P[i].y + P[(i + 1) % n].y) / 2) }
    if ("diag" in f.ov && n >= 4) {
        for (i in 0 until n) for (j in i + 2 until n) {
            if (i == 0 && j == n - 1) continue
            seg(cam, P[i], P[j], white, 1.5f, a, dashed = true)
        }
    }
    val cen = Pt(P.sumOf { it.x } / n, P.sumOf { it.y } / n)
    if ("mid" in f.ov) mids.forEach { dot(cam, it, Color(0xFFE5E7EB), a, 3f) }
    if ("cen" in f.ov) {
        dot(cam, cen, Color.White, a, 3.5f)
        lab(cen, "G", Color.White)
    }

    if (n == 3) {
        val ta = P[0]
        val tb = P[1]
        val tc = P[2]
        val oc = circumcenter(ta, tb, tc)
        if ("med" in f.ov) {
            for (i in 0 until 3) seg(cam, P[i], mids[(i + 1) % 3], white, 1.5f, a, dashed = true)
        }
        if ("alt" in f.ov) {
            val green = Color(0xFF34D399)
            for (i in 0 until 3) {
                val foot = footOn(P[i], P[(i + 1) % 3], P[(i + 2) % 3])
                seg(cam, P[i], foot, green, 1.5f, a, dashed = true)
                dot(cam, foot, green, a, 2.5f)
            }
        }
        if ("bis" in f.ov) {
            val pink = Color(0xFFF0ABFC)
            for (i in 0 until 3) {
                val p = P[i]
                val q = P[(i + 1) % 3]
                val r = P[(i + 2) % 3]
                val lq = dist(p, q)
                val lr = dist(p, r)
                val d = Pt((lr * q.x + lq * r.x) / (lq + lr), (lr * q.y + lq * r.y) / (lq + lr))
                seg(cam, p, d, pink, 1.5f, a, dashed = true)
            }
        }
        if ("orth" in f.ov && oc != null) {
            val h = Pt(ta.x + tb.x + tc.x - 2 * oc.x, ta.y + tb.y + tc.y - 2 * oc.y)
            dot(cam, h, Color(0xFFFB923C), a, 4f)
            lab(h, "H", Color(0xFFFB923C))
        }
        if ("inc" in f.ov) {
            val ic = incenter(ta, tb, tc)
            val s = (dist(ta, tb) + dist(tb, tc) + dist(tc, ta)) / 2
            val r = polyArea(P) / s
            ring(cam, ic, r, Color(0xFF34D399), 1.5f, a)
            dot(cam, ic, Color(0xFF34D399), a, 3.5f)
            lab(ic, "I", Color(0xFF34D399))
        }
        if ("circ" in f.ov && oc != null) {
            ring(cam, oc, dist(oc, ta), Color(0xFF60A5FA), 1.5f, a)
            dot(cam, oc, Color(0xFF60A5FA), a, 3.5f)
            lab(oc, "O", Color(0xFF60A5FA))
        }
        if ("subt" in f.ov && oc != null) {
            val blue = Color(0xFF60A5FA)
            for (i in 0 until 3) seg(cam, oc, P[i], blue, 1.2f, a, dashed = true)
            for (i in 0 until 3) {
                val p = P[i]
                val q = P[(i + 1) % 3]
                val ang = angDeg(oc, p, q)
                val mp = Pt((p.x + q.x) / 2, (p.y + q.y) / 2)
                val tx = oc.x + (mp.x - oc.x) * 0.45
                val ty = oc.y + (mp.y - oc.y) * 0.45
                outlinedText(m, fmt(ang, 0) + "°", cam.sx(tx), cam.sy(ty), blue, 10f, a)
            }
        }
    } else if ("cir" in f.ov && isCyclic(P)) {
        val oc = circumcenter(P[0], P[1], P[2])
        if (oc != null) {
            ring(cam, oc, dist(oc, P[0]), Color(0xFF60A5FA), 1.5f, a)
            dot(cam, oc, Color(0xFF60A5FA), a, 3.5f)
            lab(oc, "O", Color(0xFF60A5FA))
        }
    }
}

/** Web: drawS - draws one figure. [sc] is the pop-in/out scale, [a] the opacity. */
fun DrawScope.drawFig(
    cam: Camera, m: TextMeasurer, f: Fig, sc: Float, a: Float, quiet0: Boolean, axes: Boolean,
    all: Boolean = false, skipV: Set<Int> = emptySet(), lenT: Float = 0.5f,
) {
    if (a <= 0f) return
    // the eye of a Content selection ("eye" in ov) stays with the figure: it keeps its labels in overlaps and is not hidden
    // again when the selection box goes. What the Details box hides is respected (f.hide).
    val eye = all || "eye" in f.ov
    val hideS: Set<String> = f.hide
    val onlyS = f.only
    val quiet = quiet0 && !eye
    val c = f.col ?: figColor(f.t)
    val an = center(f)
    val P = pts(f)
    withTransform({ scale(sc, sc, Offset(cam.sx(an.x), cam.sy(an.y))) }) {
        when (f.t) {
            "pt" -> {
                val p = P[0]
                dot(cam, p, c, a, 5f)
                outlinedText(m, "(${fmt(p.x)}, ${fmt(p.y)})", cam.sx(p.x), cam.sy(p.y) - 14.dp.toPx(), Color.White, 12f, a)
            }
            "seg" -> {
                val p = P[0]
                val q = P[1]
                seg(cam, p, q, c, 2f, a)
                dot(cam, p, c, a, 4f)
                dot(cam, q, c, a, 4f)
                if (f.nm != null && "v" !in hideS) {
                    val sp = scr(cam, p)
                    val sq = scr(cam, q)
                    var ux = sp.x - sq.x
                    var uy = sp.y - sq.y
                    val ul = hypot(ux, uy).let { if (it < 1f) 1f else it }
                    ux /= ul
                    uy /= ul
                    val off = 15.dp.toPx()
                    val nz = Nm.of(f.nm)
                    if (0 !in skipV) outlinedText(m, nz[0], sp.x + ux * off, sp.y + uy * off, c, 12f, a, serif = true)
                    if (1 !in skipV) outlinedText(m, nz[1], sq.x - ux * off, sq.y - uy * off, c, 12f, a, serif = true)
                }
                if ("len" !in hideS) outlinedText(m, fmt(dist(p, q)), cam.sx(p.x) + (cam.sx(q.x) - cam.sx(p.x)) * lenT, cam.sy(p.y) + (cam.sy(q.y) - cam.sy(p.y)) * lenT - 12.dp.toPx(), Color.White, 12f, a)
            }
            "circ" -> {
                val o = P[0]
                val e = P[1]
                val r = dist(o, e)
                val rpx = (r * cam.u).toFloat()
                drawCircle(c.copy(alpha = 0.16f * a), rpx, scr(cam, o), style = Fill)
                drawCircle(c.copy(alpha = a), rpx, scr(cam, o), style = Stroke(2.dp.toPx()))
                dot(cam, o, c, a, 3f)
                if ("rad" in f.ov) {
                    seg(cam, o, e, c, 2f, a)
                    dot(cam, e, c, a, 4f)
                }
                if ("rad" in f.ov && "len" !in hideS) outlinedText(m, "r = " + fmt(r), (cam.sx(o.x) + cam.sx(e.x)) / 2, (cam.sy(o.y) + cam.sy(e.y)) / 2 - 12.dp.toPx(), Color.White, 12f, a)
            }
            "trig" -> {
                drawPoly(cam, m, P, c, f.nm ?: "ABC", quiet, axes, a, hideS, onlyS, skipV)
                drawTrigWords(cam, m, P, a)
            }
            else -> drawPoly(cam, m, P, c, f.nm, quiet, axes, a, hideS, onlyS, skipV)
        }
        if (!quiet && f.ov.isNotEmpty()) drawOverlays(cam, m, f, P, a)
    }
}

private val POLYS = setOf("tri", "quad", "rect", "rtri", "pent", "hex")

/** Web: quiet mode - when polygons overlap only the original (or the selected one) shows labels. */
fun quietSet(shapes: List<Fig>, active: Fig?): Set<Fig> {
    val pl = shapes.filter { it.dieAt == 0L && it.t in POLYS }
    val boxes = pl.associateWith { s ->
        val q = pts(s)
        doubleArrayOf(q.minOf { it.x }, q.maxOf { it.x }, q.minOf { it.y }, q.maxOf { it.y })
    }
    fun ov(a: Fig, b: Fig): Boolean {
        val p = boxes.getValue(a)
        val q = boxes.getValue(b)
        return p[0] < q[1] && q[0] < p[1] && p[2] < q[3] && q[2] < p[3]
    }
    val ac = active?.takeIf { it in pl }
    val out = HashSet<Fig>()
    pl.forEachIndexed { i, s ->
        val o = pl.filter { it !== s && ov(s, it) }
        if (o.isEmpty()) return@forEachIndexed
        val a = if (ac != null && (ac === s || o.contains(ac))) ac else null
        val quiet = if (a != null) s !== a else o.any { pl.indexOf(it) < i }
        if (quiet) out.add(s)
    }
    return out
}

private fun edgesOf(f: Fig): List<Pair<Pt, Pt>> {
    if (f.t == "seg") return listOf(f.p[0] to f.p[1])
    if (f.t !in POLYS) return emptyList()
    val q = pts(f)
    return q.indices.map { q[it] to q[(it + 1) % q.size] }
}

/**
 * Shaded angle wedges where figures cross (limited to 40). Thin and crisp, and without numbers - until the eye of a
 * Content selection is on: then every angle at a crossing of selected figures shows its degrees (at most four).
 */
fun DrawScope.drawCrossings(cam: Camera, m: TextMeasurer, shapes: List<Fig>, quiet: Set<Fig>, deg: Set<Fig>) {
    val figs = shapes.filter { it.dieAt == 0L && it !in quiet && edgesOf(it).isNotEmpty() }
    var n = 0
    for (i in figs.indices) for (j in i + 1 until figs.size) {
        val showDeg = figs[i] in deg && figs[j] in deg
        for ((a, b) in edgesOf(figs[i])) for ((c, d) in edgesOf(figs[j])) {
            if (n >= 40) return
            val h = isect(a, b, c, d) ?: continue
            val q = h.p
            if (listOf(a, b, c, d).any { dist(q, it) < 1e-6 }) continue
            n++
            val ux = b.x - a.x
            val uy = b.y - a.y
            var vx = d.x - c.x
            var vy = d.y - c.y
            if (ux * vx + uy * vy < 0) {
                vx = -vx
                vy = -vy
            }
            val a1 = atan2(-uy, ux).toFloat()
            val a2 = atan2(-vy, vx).toFloat()
            val cx = cam.sx(q.x)
            val cy = cam.sy(q.y)
            wedgeFill(cx, cy, a1, a2, 15.dp.toPx(), Color(0xFFFBBF24), 0.22f, 1.5f)
            if (showDeg) {
                val ul = hypot(ux, uy)
                val vl = hypot(vx, vy)
                val th = acos(((ux * vx + uy * vy) / (ul * vl)).coerceIn(-1.0, 1.0)) * 180 / PI
                // the four directions: u, v, -u, -v (screen angles)
                val dirs = floatArrayOf(a1, a2, a1 + PI.toFloat(), a2 + PI.toFloat())
                for (k in 0 until 4) {
                    val s1 = dirs[k]
                    val s2 = dirs[(k + 1) % 4]
                    var bx = cos(s1) + cos(s2)
                    var by = sin(s1) + sin(s2)
                    val bl = hypot(bx, by)
                    if (bl < 1e-4f) { bx = -sin(s1); by = cos(s1) } else { bx /= bl; by /= bl }
                    // k = 0 and 2 are the angle between u and v (th); k = 1 and 3 the other one
                    val value = if (k % 2 == 0) th else 180 - th
                    val col = if (k % 2 == 0) Color(0xFFFBBF24) else Color(0xFF60A5FA)
                    outlinedText(m, fmt(value, 1) + "°", cx + bx * 30.dp.toPx(), cy + by * 30.dp.toPx(), col, 10.5f, 1f)
                }
            }
        }
    }
}

/** Selected figure: marching dashed border that pulses between white and the figure's own colour. */
fun DrawScope.drawSelection(cam: Camera, f: Fig, phase: Float, pulse: Float, active: Boolean) {
    val c = f.col ?: figColor(f.t)
    val line = if (active) Color(0xFFFBBF24) else lerp(Color.White, c, pulse)
    val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), phase.dp.toPx())
    val P = pts(f)
    when (f.t) {
        "circ" -> {
            val r = (dist(P[0], P[1]) * cam.u).toFloat() + 8.dp.toPx()
            drawCircle(line, r, scr(cam, P[0]), style = Stroke(2.dp.toPx(), pathEffect = dash))
        }
        "pt" -> {
            drawCircle(line, 14.dp.toPx(), scr(cam, P[0]), style = Stroke(2.dp.toPx(), pathEffect = dash))
        }
        else -> {
            val path = Path().apply {
                P.forEachIndexed { i, p -> if (i == 0) moveTo(cam.sx(p.x), cam.sy(p.y)) else lineTo(cam.sx(p.x), cam.sy(p.y)) }
                if (P.size > 2) close()
            }
            drawPath(path, line, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round, pathEffect = dash))
        }
    }
}

/** Drag handles: a white dot with a breathing ring in the figure's colour. */
fun DrawScope.drawHandles(cam: Camera, f: Fig, pulse: Float) {
    val c = f.col ?: figColor(f.t)
    for (i in 0 until hcount(f)) {
        val o = scr(cam, f.p[i])
        drawCircle(c.copy(alpha = 0.55f - 0.3f * pulse), (9f + 4f * pulse).dp.toPx(), o, style = Stroke(1.5.dp.toPx()))
        drawCircle(Color.White, 6.dp.toPx(), o)
        drawCircle(c, 6.dp.toPx(), o, style = Stroke(2.dp.toPx()))
    }
}

/** Ghost while the finger is down: the points already tapped, lines between them and the cursor. */
fun DrawScope.drawPending(cam: Camera, m: TextMeasurer, pend: List<Pt>, cursor: Pt?, tool: String) {
    val c = figColor(tool)
    val all = if (cursor != null) pend + cursor else pend
    for (i in 0 until all.size - 1) seg(cam, all[i], all[i + 1], c, 2f, 0.7f, dashed = true)
    pend.forEach { dot(cam, it, c, 1f, 4f) }
    if (cursor != null) {
        dot(cam, cursor, c, 0.7f, 4f)
        outlinedText(m, "(${fmt(cursor.x)}, ${fmt(cursor.y)})", cam.sx(cursor.x), cam.sy(cursor.y) - 14.dp.toPx(), Color.White, 11f, 0.8f)
    }
}
