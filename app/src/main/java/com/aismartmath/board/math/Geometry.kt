package com.aismartmath.board.math

import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Pt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/** Ported from the web helpers D, ang, area, pts, center, ok, dseg, inPoly, segAng, solveTri, isect. */

fun dist(a: Pt, b: Pt): Double = hypot(a.x - b.x, a.y - b.y)

fun angDeg(p: Pt, a: Pt, b: Pt): Double {
    val ux = a.x - p.x
    val uy = a.y - p.y
    val vx = b.x - p.x
    val vy = b.y - p.y
    val d = hypot(ux, uy) * hypot(vx, vy)
    val c = ((ux * vx + uy * vy) / (if (d == 0.0) 1.0 else d)).coerceIn(-1.0, 1.0)
    return acos(c) * 180 / PI
}

fun polyArea(p: List<Pt>): Double {
    var s = 0.0
    for (i in p.indices) {
        val a = p[i]
        val b = p[(i + 1) % p.size]
        s += a.x * b.y - b.x * a.y
    }
    return abs(s) / 2
}

fun ptsOf(t: String, p: List<Pt>): List<Pt> = when (t) {
    "rect" -> {
        val a = p[0]
        val b = p[1]
        listOf(a, Pt(b.x, a.y), b, Pt(a.x, b.y))
    }
    "rtri" -> {
        val a = p[0]
        val b = p[1]
        listOf(a, Pt(b.x, a.y), b)
    }
    else -> p
}

/** The trig triangle: right angle at B, angle theta (degrees) at A, hypotenuse h. */
fun trigPts(a: Pt, th: Double, h: Double): List<Pt> {
    val t = th * PI / 180
    val bx = a.x + h * kotlin.math.cos(t)
    return listOf(a, Pt(bx, a.y), Pt(bx, a.y + h * kotlin.math.sin(t)))
}

fun pts(f: Fig): List<Pt> = if (f.t == "trig") trigPts(f.p[0], f.ga, f.gb) else ptsOf(f.t, f.p)

fun centerOf(t: String, p: List<Pt>): Pt {
    if (t == "fx") return Pt(0.0, 0.0)
    if (t == "circ" || t == "pt") return p[0]
    val q = ptsOf(t, p)
    return Pt(q.sumOf { it.x } / q.size, q.sumOf { it.y } / q.size)
}

fun center(f: Fig): Pt {
    if (f.t == "unit") return Pt(0.0, 0.0)
    if (f.t == "trig") {
        val q = pts(f)
        return Pt(q.sumOf { it.x } / 3, q.sumOf { it.y } / 3)
    }
    return centerOf(f.t, f.p)
}

/** Web: ok(t,p) - does this make a real figure? */
fun figOk(t: String, p: List<Pt>): Boolean = when (t) {
    "pt", "trig" -> true
    "seg", "circ", "unit" -> dist(p[0], p[1]) > 1e-9
    "rect", "rtri", "img", "txt" -> p[0].x != p[1].x && p[0].y != p[1].y
    else -> polyArea(p) > 1e-9
}

fun dseg(p: Pt, a: Pt, b: Pt): Double {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val l = dx * dx + dy * dy
    val t = if (l != 0.0) (((p.x - a.x) * dx + (p.y - a.y) * dy) / l).coerceIn(0.0, 1.0) else 0.0
    return hypot(p.x - a.x - t * dx, p.y - a.y - t * dy)
}

fun inPoly(p: Pt, q: List<Pt>): Boolean {
    var c = false
    var j = q.size - 1
    for (i in q.indices) {
        if ((q[i].y > p.y) != (q[j].y > p.y) &&
            p.x < (q[j].x - q[i].x) * (p.y - q[i].y) / (q[j].y - q[i].y) + q[i].x
        ) c = !c
        j = i
    }
    return c
}

fun segAngle(a: Pt, b: Pt): Double = (((atan2(b.y - a.y, b.x - a.x) * 180 / PI) % 180) + 180) % 180

/** Web: solveTri - rebuild a triangle after one side length is edited; null if the sides cannot close. */
fun solveTri(p: List<Pt>, k: String, v: Double): List<Pt>? {
    var ab = dist(p[0], p[1])
    var bc = dist(p[1], p[2])
    var ca = dist(p[2], p[0])
    when (k) {
        "ab" -> ab = v
        "bc" -> bc = v
        "ca" -> ca = v
    }
    if (ab + bc <= ca || ab + ca <= bc || bc + ca <= ab) return null
    val a = p[0]
    val th = atan2(p[1].y - a.y, p[1].x - a.x)
    val ux = kotlin.math.cos(th)
    val uy = kotlin.math.sin(th)
    val nx = -uy
    val ny = ux
    val cr = (p[1].x - a.x) * (p[2].y - a.y) - (p[1].y - a.y) * (p[2].x - a.x)
    val g = if (cr < 0) -1.0 else 1.0
    val x = (ab * ab + ca * ca - bc * bc) / (2 * ab)
    val y = sqrt(max(0.0, ca * ca - x * x))
    return listOf(
        a,
        Pt(a.x + ab * ux, a.y + ab * uy),
        Pt(a.x + x * ux + g * y * nx, a.y + x * uy + g * y * ny),
    )
}

class IsectHit(val p: Pt, val ang: Double)

/** Web: isect - where two segments cross (null when parallel or not touching). */
fun isect(a: Pt, b: Pt, c: Pt, d: Pt): IsectHit? {
    val rx = b.x - a.x
    val ry = b.y - a.y
    val sx = d.x - c.x
    val sy = d.y - c.y
    val cross = rx * sy - ry * sx
    if (abs(cross) < 1e-12) return null
    val t = ((c.x - a.x) * sy - (c.y - a.y) * sx) / cross
    val u = ((c.x - a.x) * ry - (c.y - a.y) * rx) / cross
    if (t < -1e-9 || t > 1 + 1e-9 || u < -1e-9 || u > 1 + 1e-9) return null
    val ang = acos(minOf(1.0, abs(rx * sx + ry * sy) / (hypot(rx, ry) * hypot(sx, sy)))) * 180 / PI
    return IsectHit(Pt(a.x + t * rx, a.y + t * ry), ang)
}

// ---------------- triangle centres, circles, quadrilaterals, regular polygons ----------------

fun circumcenter(a: Pt, b: Pt, c: Pt): Pt? {
    val d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y))
    if (abs(d) < 1e-12) return null
    val a2 = a.x * a.x + a.y * a.y
    val b2 = b.x * b.x + b.y * b.y
    val c2 = c.x * c.x + c.y * c.y
    val ux = (a2 * (b.y - c.y) + b2 * (c.y - a.y) + c2 * (a.y - b.y)) / d
    val uy = (a2 * (c.x - b.x) + b2 * (a.x - c.x) + c2 * (b.x - a.x)) / d
    return Pt(ux, uy)
}

fun incenter(a: Pt, b: Pt, c: Pt): Pt {
    val la = dist(b, c)
    val lb = dist(c, a)
    val lc = dist(a, b)
    val s = la + lb + lc
    return Pt((la * a.x + lb * b.x + lc * c.x) / s, (la * a.y + lb * b.y + lc * c.y) / s)
}

/** Foot of the perpendicular from p onto the line a-b. */
fun footOn(p: Pt, a: Pt, b: Pt): Pt {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val l = dx * dx + dy * dy
    val t = if (l == 0.0) 0.0 else ((p.x - a.x) * dx + (p.y - a.y) * dy) / l
    return Pt(a.x + t * dx, a.y + t * dy)
}

/** True when every vertex lies on one circle (needs at least 3 vertices). */
fun isCyclic(p: List<Pt>): Boolean {
    if (p.size < 3) return false
    val o = circumcenter(p[0], p[1], p[2]) ?: return false
    val r = dist(o, p[0])
    return p.drop(3).all { abs(dist(o, it) - r) < 1e-6 * max(1.0, r) }
}

/** If the four corners are an axis-parallel rectangle, return it as two opposite corners. */
fun rectOf(q: List<Pt>): List<Pt>? {
    if (q.size != 4) return null
    val an = q.indices.map { i -> angDeg(q[i], q[(i + 3) % 4], q[(i + 1) % 4]) }
    val ok = an.all { abs(it - 90) < 0.6 } && q.indices.all { i ->
        val p = q[i]
        val r = q[(i + 1) % 4]
        abs(r.x - p.x) < 1e-9 || abs(r.y - p.y) < 1e-9
    }
    if (!ok) return null
    val xs = q.map { it.x }
    val ys = q.map { it.y }
    return listOf(Pt(xs.min(), ys.min()), Pt(xs.max(), ys.max()))
}

/**
 * Edit one measurement of a quadrilateral ABCD: sides "ab", "bc", "cd", "da" or diagonal "ac".
 * A stays put and AB keeps its direction; B, C and D are re-solved so every length matches.
 * Returns null when the lengths cannot close into a quadrilateral.
 */
fun solveQuad(p: List<Pt>, k: String, v: Double): List<Pt>? {
    val a = p[0]
    val b = p[1]
    val c = p[2]
    val d = p[3]
    var ab = dist(a, b)
    var bc = dist(b, c)
    var cd = dist(c, d)
    var da = dist(d, a)
    var ac = dist(a, c)
    when (k) {
        "ab" -> ab = v
        "bc" -> bc = v
        "cd" -> cd = v
        "da" -> da = v
        "ac" -> ac = v
        else -> return null
    }
    if (ab < 1e-9 || ac < 1e-9) return null
    if (ab + bc <= ac || ab + ac <= bc || bc + ac <= ab) return null
    if (ac + cd <= da || ac + da <= cd || cd + da <= ac) return null
    val th = atan2(b.y - a.y, b.x - a.x)
    val ux = kotlin.math.cos(th)
    val uy = kotlin.math.sin(th)
    val crossC = ux * (c.y - a.y) - uy * (c.x - a.x)
    val sgnC = if (crossC < 0) -1.0 else 1.0
    val x = (ab * ab + ac * ac - bc * bc) / (2 * ab)
    val y = sqrt(max(0.0, ac * ac - x * x))
    val nb = Pt(a.x + ab * ux, a.y + ab * uy)
    val nc = Pt(a.x + x * ux - sgnC * y * uy, a.y + x * uy + sgnC * y * ux)
    val wx = (nc.x - a.x) / ac
    val wy = (nc.y - a.y) / ac
    val crossD = wx * (d.y - a.y) - wy * (d.x - a.x)
    val sgnD = if (crossD < 0) -1.0 else 1.0
    val x2 = (ac * ac + da * da - cd * cd) / (2 * ac)
    val y2 = sqrt(max(0.0, da * da - x2 * x2))
    val nd = Pt(a.x + x2 * wx - sgnD * y2 * wy, a.y + x2 * wy + sgnD * y2 * wx)
    return listOf(a, nb, nc, nd)
}

/** A regular polygon with the same centre; its radius is the average distance of the old corners. */
fun regularPolygon(p: List<Pt>): List<Pt> {
    val n = p.size
    val cx = p.sumOf { it.x } / n
    val cy = p.sumOf { it.y } / n
    val r = p.sumOf { hypot(it.x - cx, it.y - cy) } / n
    val th0 = atan2(p[0].y - cy, p[0].x - cx)
    var s = 0.0
    for (i in 0 until n) {
        val q = p[i]
        val w = p[(i + 1) % n]
        s += q.x * w.y - w.x * q.y
    }
    val dir = if (s < 0) -1.0 else 1.0
    return List(n) { i ->
        val th = th0 + dir * 2 * PI * i / n
        Pt(cx + r * kotlin.math.cos(th), cy + r * kotlin.math.sin(th))
    }
}
