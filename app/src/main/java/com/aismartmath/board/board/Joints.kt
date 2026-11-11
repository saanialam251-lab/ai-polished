package com.aismartmath.board.board

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.aismartmath.board.math.dist
import com.aismartmath.board.math.pts
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Pt
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * Joints: where lines (or corners of shapes) meet in one point. They share one letter, make angles with each
 * other (∠BAD …), and those angles can be shown with their degrees and changed.
 */

private val JPOLY = setOf("tri", "quad", "rect", "rtri", "pent", "hex")

private class VEnt(val fig: Fig, val idx: Int, val at: Pt, val label: String, val rays: List<Pair<Pt, String>>)

private class Cluster(val at: Pt, val ents: MutableList<VEnt>)

private class JRay(val fig: Fig, val idx: Int, val ang: Double, val to: String)

private fun entriesOf(f: Fig): List<VEnt> {
    if (f.t == "seg") {
        val p = pts(f)
        if (p.size < 2) return emptyList()
        val nz = Nm.of(f.nm)
        return listOf(
            VEnt(f, 0, p[0], nz[0], listOf(p[1] to nz[1])),
            VEnt(f, 1, p[1], nz[1], listOf(p[0] to nz[0])),
        )
    }
    if (f.t in JPOLY) {
        val q = pts(f)
        val n = q.size
        val nz = Nm.of(f.nm)
        return q.indices.map { i ->
            VEnt(f, i, q[i], nz[i], listOf(q[(i + n - 1) % n] to nz[(i + n - 1) % n], q[(i + 1) % n] to nz[(i + 1) % n]))
        }
    }
    return emptyList()
}

private fun clustersOf(shapes: List<Fig>, tol: Double): List<Cluster> {
    val out = mutableListOf<Cluster>()
    for (f in shapes) {
        if (f.dieAt != 0L) continue
        for (e in entriesOf(f)) {
            val c = out.firstOrNull { dist(it.at, e.at) < tol }
            if (c == null) out.add(Cluster(e.at, mutableListOf(e))) else c.ents.add(e)
        }
    }
    return out
}

/** Which letters are not drawn (another figure's letter already sits on the same point) and where length labels go. */
class LabelPlan(val skip: Map<Fig, Set<Int>>, val lenT: Map<Fig, Float>)

fun labelPlan(cam: Camera, shapes: List<Fig>): LabelPlan {
    val tol = 6.0 * cam.density / cam.u
    val skip = HashMap<Fig, MutableSet<Int>>()
    for (c in clustersOf(shapes, tol)) {
        if (c.ents.size < 2) continue
        val first = c.ents[0]
        for (k in 1 until c.ents.size) {
            val e = c.ents[k]
            if (e.fig !== first.fig) skip.getOrPut(e.fig) { mutableSetOf() }.add(e.idx)
        }
    }
    // lines that cross at their middles: the later one shows its length further along the line
    val lenT = HashMap<Fig, Float>()
    val mids = ArrayList<Pt>()
    for (s in shapes) {
        if (s.dieAt != 0L || s.t != "seg") continue
        val q = pts(s)
        if (q.size < 2) continue
        val m = Pt((q[0].x + q[1].x) / 2, (q[0].y + q[1].y) / 2)
        if (mids.any { dist(it, m) < tol * 4 }) lenT[s] = 0.28f
        mids.add(m)
    }
    return LabelPlan(skip, lenT)
}

/** An angle at a joint of line [f] with another line, for the details panel. */
class JointInfo(val e: Int, val other: Fig, val oe: Int, val label: String, val aLab: String, val bLab: String, val deg: Double) {
    val name: String get() = "∠" + aLab + label + bLab
}

fun jointsOf(shapes: List<Fig>, f: Fig, tol: Double): List<JointInfo> {
    if (f.t != "seg") return emptyList()
    val pf = pts(f)
    if (pf.size < 2) return emptyList()
    val out = mutableListOf<JointInfo>()
    val nf = Nm.of(f.nm)
    val fi = shapes.indexOf(f)
    for (e in 0..1) {
        val v = pf[e]
        for (g in shapes) {
            if (g === f || g.dieAt != 0L || g.t != "seg") continue
            val pg = pts(g)
            if (pg.size < 2) continue
            for (oe in 0..1) {
                if (dist(pg[oe], v) >= tol) continue
                val a = pf[1 - e]
                val b = pg[1 - oe]
                val ux = a.x - v.x
                val uy = a.y - v.y
                val wx = b.x - v.x
                val wy = b.y - v.y
                val l = hypot(ux, uy) * hypot(wx, wy)
                if (l < 1e-12) continue
                val deg = acos(((ux * wx + uy * wy) / l).coerceIn(-1.0, 1.0)) * 180 / PI
                val ng = Nm.of(g.nm)
                val lab = if (fi <= shapes.indexOf(g)) nf[e] else ng[oe]
                out.add(JointInfo(e, g, oe, lab, nf[1 - e], ng[1 - oe], deg))
            }
        }
    }
    return out
}

/**
 * The angles at joints, as thin wedges. Their numbers (with names like ∠BAD) appear only for the figures in [deg]
 * (the Content eye) or for a line whose own joint eye is on.
 */
fun DrawScope.drawJoints(cam: Camera, m: TextMeasurer, shapes: List<Fig>, quiet: Set<Fig>, deg: Set<Fig>) {
    val tol = 6.0 * cam.density / cam.u
    var drawn = 0
    for (c in clustersOf(shapes.filter { it !in quiet }, tol)) {
        if (c.ents.map { it.fig }.distinct().size < 2) continue
        val rays = mutableListOf<JRay>()
        for (e in c.ents) for ((pt, lab) in e.rays) rays.add(JRay(e.fig, e.idx, atan2(pt.y - c.at.y, pt.x - c.at.x), lab))
        rays.sortBy { it.ang }
        val pairs = mutableListOf<Triple<JRay, JRay, Double>>()
        if (rays.size == 2) {
            var d = rays[1].ang - rays[0].ang
            if (d < 0) d += 2 * PI
            if (d <= PI) pairs.add(Triple(rays[0], rays[1], d)) else pairs.add(Triple(rays[1], rays[0], 2 * PI - d))
        } else {
            for (k in rays.indices) {
                val r1 = rays[k]
                val r2 = rays[(k + 1) % rays.size]
                var d = r2.ang - r1.ang
                if (d < 0) d += 2 * PI
                if (d < 1e-6 || d > PI + 1e-9) continue
                pairs.add(Triple(r1, r2, d))
            }
        }
        val label = c.ents[0].label
        val cx = cam.sx(c.at.x)
        val cy = cam.sy(c.at.y)
        for ((r1, r2, d) in pairs) {
            if (r1.fig === r2.fig) continue
            if (drawn >= 30) return
            drawn++
            val a1 = (-r1.ang).toFloat()
            val a2 = (-r2.ang).toFloat()
            wedgeFill(cx, cy, a1, a2, 17.dp.toPx(), Color(0xFFFBBF24), 0.22f, 1.5f)
            val own1 = r1.fig.t == "seg" && ("jd" + r1.idx) in r1.fig.ov
            val own2 = r2.fig.t == "seg" && ("jd" + r2.idx) in r2.fig.ov
            if (r1.fig in deg || r2.fig in deg || own1 || own2) {
                var bx = cos(a1) + cos(a2)
                var by = sin(a1) + sin(a2)
                val bl = hypot(bx, by)
                if (bl < 1e-4f) {
                    bx = -sin(a1)
                    by = cos(a1)
                } else {
                    bx /= bl
                    by /= bl
                }
                val text = "∠" + r1.to + label + r2.to + " " + fmt(d * 180 / PI, 1) + "°"
                outlinedText(m, text, cx + bx * 46.dp.toPx(), cy + by * 46.dp.toPx(), Color(0xFFFBBF24), 10.5f, 1f)
            }
        }
    }
}
