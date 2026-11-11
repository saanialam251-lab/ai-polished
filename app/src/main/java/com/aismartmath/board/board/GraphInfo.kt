package com.aismartmath.board.board

import com.aismartmath.board.math.MathNode
import com.aismartmath.board.math.eqMath
import com.aismartmath.board.math.toMath
import com.aismartmath.board.math.usesParams
import com.aismartmath.board.model.Fig
import kotlin.math.abs

private fun t(s: String): Part = Part.T(s)
private fun m(n: MathNode): Part = Part.M(n)
private fun txt(s: String) = MathNode.Txt(s)

private fun join(items: List<List<Part>>): List<Part> {
    val out = mutableListOf<Part>()
    items.forEachIndexed { i, p ->
        if (i > 0) out.add(t(" · "))
        out.addAll(p)
    }
    return out
}

private fun roots(g: (Double) -> Double): List<Double> {
    val r = mutableListOf<Double>()
    var p = g(-20.0)
    for (i in 1..2000) {
        val x = -20.0 + i * 0.02
        val q = g(x)
        if (p.isFinite() && q.isFinite() && p * q <= 0 && (p != 0.0 || q != 0.0)) {
            var lo = x - 0.02
            var hi = x
            repeat(40) {
                val mid = (lo + hi) / 2
                if (g(lo) * g(mid) <= 0) hi = mid else lo = mid
            }
            val z = (lo + hi) / 2
            if (abs(g(z)) < 1e-6 || p == 0.0 || q == 0.0) {
                if (r.isEmpty() || abs(z - r.last()) > 0.05) r.add(z)
            }
        }
        p = q
    }
    return r
}

/** Web: fxInfo - what the graph is, its intercepts and turning points (cached until it changes). */
fun graphParts(f: Fig): List<Part> {
    val g = f.gd ?: return emptyList()
    val key = g.src + "|" + g.dv + "|" + f.ga + "," + f.gb + "," + f.gc
    if (f.infoKey == key) (f.infoCache as? List<Part>)?.let { return it }

    val pd = g.par
    val out: List<Part> = if (pd != null) {
        listOf(
            t(if (pd.polar) "Polar curve  " else "Curve  "),
            m(eqMath(g.src)),
            t(if (pd.polar) " — r changes with the angle θ" else " — x and y both depend on t"),
        )
    } else if (g.dv > 0) {
        val sec = g.dv > 1
        listOf(
            m(MathNode.Seq(listOf(txt(if (sec) "f″(x) = " else "f′(x) = "), MathNode.Frac(txt(if (sec) "d²" else "d"), txt(if (sec) "dx²" else "dx")), MathNode.Paren(toMath(g.src))))),
            t(" — slope of "),
            m(MathNode.Seq(listOf(txt("y = "), toMath(g.src)))),
        )
    } else if (g.imp) {
        val xr = roots { x -> f.vAt(x, 0.0) }
        val yr = roots { y -> f.vAt(0.0, y) }
        join(
            listOf(
                listOf(m(eqMath(g.src)), t(" (curve)")),
                listOf(t(if (xr.isNotEmpty()) "x-intercepts " + xr.take(4).joinToString(", ") { fmt(it) } else "no x-intercepts")),
                listOf(t(if (yr.isNotEmpty()) "y-intercepts " + yr.take(4).joinToString(", ") { fmt(it) } else "no y-intercepts")),
            ),
        )
    } else {
        val xs = DoubleArray(2001) { -20.0 + it * 0.02 }
        val ys = DoubleArray(2001) { f.yAt(xs[it]) }
        val rt = mutableListOf<Double>()
        val tp = mutableListOf<Pair<Double, Double>>()
        for (i in 1..2000) {
            val p = ys[i - 1]
            val q = ys[i]
            if (p.isFinite() && q.isFinite() && p * q <= 0 && (p != 0.0 || q != 0.0)) {
                var lo = xs[i - 1]
                var hi = xs[i]
                repeat(40) {
                    val mid = (lo + hi) / 2
                    if (f.yAt(lo) * f.yAt(mid) <= 0) hi = mid else lo = mid
                }
                val r = (lo + hi) / 2
                if (abs(f.yAt(r)) < 1e-6 && (rt.isEmpty() || abs(r - rt.last()) > 0.05)) rt.add(r)
            }
            if (i < 2000 && (q - p) * (ys[i + 1] - q) < 0 && abs(q) < 1e4) tp.add(xs[i] to q)
        }
        val items = mutableListOf<List<Part>>()
        val head = mutableListOf<Part>(m(MathNode.Seq(listOf(txt("y = "), toMath(g.src)))))
        if (usesParams(g.src)) head.add(t(" (a=${fmt(f.ga, 1)}, b=${fmt(f.gb, 1)}, c=${fmt(f.gc, 1)})"))
        items.add(head)
        val f0 = f.yAt(0.0)
        if (abs(f.yAt(2.0) - 2 * f.yAt(1.0) + f0) < 1e-9) items.add(listOf(t("straight line, slope ${fmt(f.yAt(1.0) - f0)}")))
        if (f0.isFinite()) items.add(listOf(t("y-intercept ${fmt(f0)}")))
        items.add(listOf(t(if (rt.isNotEmpty()) "x-intercepts " + rt.take(4).joinToString(", ") { fmt(it) } else "no x-intercepts between -20 and 20")))
        if (tp.isNotEmpty()) items.add(listOf(t("turning points " + tp.take(3).joinToString(" ") { "(${fmt(it.first)}, ${fmt(it.second)})" })))
        join(items)
    }
    f.infoKey = key
    f.infoCache = out
    return out
}

/** Web: traceTx - the point and slope under a tapped x. */
fun traceParts(f: Fig, x: Double): List<Part> {
    val y = f.yAt(x)
    val s = (f.yAt(x + 1e-4) - f.yAt(x - 1e-4)) / 2e-4
    if (!y.isFinite() || !s.isFinite()) return emptyList()
    return listOf(t(" · At x = ${fmt(x)}: y = ${fmt(y)}, slope m = ${fmt(s)}"))
}
