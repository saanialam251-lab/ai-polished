package com.aismartmath.board.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class Pt(val x: Double, val y: Double)

/** x, y, a, b, c -> value. Explicit graphs ignore y; implicit curves are f(x,y) = 0. */
typealias Fn = (Double, Double, Double, Double, Double) -> Double

/** What a graph figure plots. dv = 0 normal, 1 = f', 2 = f''. */
/** A curve drawn from a parameter: (x(t), y(t)), or polar r(t) with t as the angle. */
class ParDef(val fx: Fn, val fy: Fn, val t0: Double, val t1: Double, val polar: Boolean)

class GraphDef(val src: String, val fn: Fn?, val imp: Boolean, val dv: Int = 0, val base: Fig? = null, val par: ParDef? = null)

/**
 * One figure on the board (web: a shapes[] entry). t = pt, seg, tri, rtri, quad, rect, circ, fx.
 * rect / rtri / seg / circ store 2 points, tri 3, quad 4 (same as the web version).
 * Graphs (t = fx) keep their data in [gd] and their sliders in ga / gb / gc.
 */
class Fig(t0: String, p0: List<Pt>, val born: Long, nm0: String? = null) {
    var t by mutableStateOf(t0)
    var p by mutableStateOf(p0)
    var nm by mutableStateOf(nm0)
    var col by mutableStateOf<Color?>(null)
    var sz by mutableDoubleStateOf(1.0)
    var rot by mutableDoubleStateOf(0.0)
    var base: List<Pt>? = null
    var ov by mutableStateOf<Set<String>>(emptySet())

    /** What [hide] was before the Content eye switched every detail on ("eye" in [ov]); the eye puts it back when switched off. */
    var eyeBefore: Set<String>? = null

    /** Labels switched off with the eye box: "s0".. = sides, "a0".. = angles, "v" = corner letters. */
    var hide by mutableStateOf<Set<String>>(emptySet())

    /** When 0 or more: show only this corner's angle (tap a corner twice). -1 = show everything that is not hidden. */
    var only by mutableIntStateOf(-1)
    var dieAt by mutableLongStateOf(0L)
    var layer by mutableIntStateOf(0)

    // 3D solids (t = "sol"): kind, dimensions, rotation, optional base polygon, switches. p[0] is where it sits.
    var sk by mutableStateOf("cube")
    var sd by mutableStateOf<Map<String, Double>>(emptyMap())
    var rx by mutableDoubleStateOf(-0.55)
    var ry by mutableDoubleStateOf(0.6)
    var poly: List<Pt> = emptyList()
    var sflags by mutableStateOf<Set<String>>(emptySet())

    var inkW = 4.0
    var hl = false
    /** Pen line style: 0 solid, 1 dashed, 2 dotted. */
    var inkStyle = 0

    // pasted text (t = "txt"): the words, and the letter height in board units. p[0] = top-left, p[1] = bottom-right.
    var txt by mutableStateOf("")
    var fsz by mutableDoubleStateOf(0.6)

    // pictures (t = "img"): key of the stored picture, and the visible part of it (left, top, right, bottom as 0..1).
    var imgKey: String = ""
    var crop by mutableStateOf(listOf(0f, 0f, 1f, 1f))

    /** Extras (diagonals, midpoints, circles...) that were switched off by "hide", so "show" can bring them back. */
    var stash by mutableStateOf<Set<String>>(emptySet())

    var gd: GraphDef? = null
    var ga by mutableDoubleStateOf(1.0)
    var gb by mutableDoubleStateOf(0.0)
    var gc by mutableDoubleStateOf(0.0)
    var infoKey: String = ""
    var infoCache: Any? = null

    /** y of an explicit graph (or of its derivative) at x; NaN when undefined. */
    fun yAt(x: Double): Double {
        val g = gd ?: return Double.NaN
        if (g.par != null) return Double.NaN
        return try {
            if (g.dv == 0) {
                g.fn!!(x, 0.0, ga, gb, gc)
            } else {
                val b = g.base ?: return Double.NaN
                if (g.dv == 1) {
                    val e = 1e-4
                    (b.yAt(x + e) - b.yAt(x - e)) / (2 * e)
                } else {
                    val e = 1e-3
                    (b.yAt(x + e) - 2 * b.yAt(x) + b.yAt(x - e)) / (e * e)
                }
            }
        } catch (e: Exception) {
            Double.NaN
        }
    }

    /** Point of a parametric or polar curve at parameter t, or null when it is undefined there. */
    fun parAt(t: Double): Pt? {
        val d = gd?.par ?: return null
        return try {
            val x: Double
            val y: Double
            if (d.polar) {
                val r = d.fx(t, 0.0, ga, gb, gc)
                x = r * kotlin.math.cos(t)
                y = r * kotlin.math.sin(t)
            } else {
                x = d.fx(t, 0.0, ga, gb, gc)
                y = d.fy(t, 0.0, ga, gb, gc)
            }
            if (x.isFinite() && y.isFinite() && kotlin.math.abs(x) < 1e5 && kotlin.math.abs(y) < 1e5) Pt(x, y) else null
        } catch (e: Exception) {
            null
        }
    }

    /** Value of an implicit curve f(x, y) at a point. */
    fun vAt(x: Double, y: Double): Double {
        val g = gd ?: return Double.NaN
        return try {
            g.fn!!(x, y, ga, gb, gc)
        } catch (e: Exception) {
            Double.NaN
        }
    }

    fun copyOf(newBorn: Long): Fig = Fig(t, p, newBorn, nm).also {
        it.col = col
        it.sz = sz
        it.rot = rot
        it.ov = ov
        it.hide = hide
        it.only = only
        it.layer = layer
        it.sk = sk
        it.sd = sd
        it.rx = rx
        it.ry = ry
        it.poly = poly
        it.sflags = sflags
        it.inkW = inkW
        it.hl = hl
        it.inkStyle = inkStyle
        it.txt = txt
        it.fsz = fsz
        it.imgKey = imgKey
        it.crop = crop
        it.stash = stash
        it.gd = gd
        it.ga = ga
        it.gb = gb
        it.gc = gc
    }
}
