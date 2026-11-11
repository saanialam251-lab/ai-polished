package com.aismartmath.board.board

import com.aismartmath.board.math.MathNode
import com.aismartmath.board.math.SOLN
import com.aismartmath.board.math.dseg
import com.aismartmath.board.math.angDeg
import com.aismartmath.board.math.circumcenter
import com.aismartmath.board.math.dist
import com.aismartmath.board.math.incenter
import com.aismartmath.board.math.isCyclic
import com.aismartmath.board.math.plain
import com.aismartmath.board.math.polyArea
import com.aismartmath.board.math.pts
import com.aismartmath.board.math.segAngle
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Pt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.hypot
import kotlin.math.sqrt

/** One piece of a "Details" line: plain text, a stacked fraction, or a maths layout. */
sealed interface Part {
    class T(val s: String) : Part
    class F(val n: String, val d: String) : Part
    class M(val node: MathNode) : Part
}

/** Stable text of a list of parts (used as an animation key). */
fun partsKey(parts: List<Part>): String = parts.joinToString("") {
    when (it) {
        is Part.T -> it.s
        is Part.F -> "[" + it.n + "/" + it.d + "]"
        is Part.M -> it.node.plain()
    }
}

class Field(val key: String, val label: String, val value: Double, val ro: Boolean = false)

val FigNames = mapOf(
    "pt" to "Point", "seg" to "Line", "circ" to "Circle", "rect" to "Rectangle",
    "rtri" to "Right triangle", "tri" to "Triangle", "quad" to "Quadrilateral",
    "pent" to "Pentagon", "hex" to "Hexagon", "fx" to "Graph", "ink" to "Drawing", "txt" to "Text", "img" to "Picture",
    "trig" to "Trig triangle", "unit" to "Unit circle",
)

fun figName(f: Fig): String = if (f.t == "sol") (SOLN[f.sk] ?: "Solid") else (FigNames[f.t] ?: f.t)

/** Web: fields() - the number boxes in the edit panel. */
fun fieldsOf(f: Fig): List<Field> {
    if (f.t == "fx" || f.t == "ink" || f.t == "txt" || f.t == "img") return emptyList()
    if (f.t == "sol") {
        val keys = when (f.sk) {
            "cube" -> listOf("a" to "Side")
            "cuboid" -> listOf("l" to "Length", "w" to "Width", "h" to "Height")
            "pyr" -> listOf("l" to "Base length", "w" to "Base width", "h" to "Height")
            "prism" -> listOf("l" to "Base", "h" to "Triangle height", "w" to "Length")
            "cyl", "cone" -> listOf("r" to "Radius", "h" to "Height")
            "sph" -> listOf("r" to "Radius")
            else -> listOf("h" to "Height")
        }
        val out = keys.map { (k, label) -> Field(k, label, f.sd[k] ?: 1.0) }.toMutableList()
        out.add(Field("ry", "Rotate °", f.ry * 180 / PI))
        out.add(Field("rx", "Tilt °", f.rx * 180 / PI))
        return out
    }
    val p = f.p
    fun l(i: Int, j: Int) = dist(p[i], p[j])
    val q = p.getOrElse(1) { p[0] }
    val w = abs(q.x - p[0].x)
    val h = abs(q.y - p[0].y)
    val names = Nm.of(f.nm)
    return when (f.t) {
        "pt" -> listOf(Field("x", "x", p[0].x), Field("y", "y", p[0].y))
        "seg" -> listOf(Field("len", "Length", l(0, 1)), Field("ang", "Angle °", segAngle(p[0], p[1])))
        "circ" -> listOf(Field("r", "Radius", l(0, 1)))
        "rect" -> listOf(Field("w", "Width", w), Field("h", "Height", h))
        "rtri" -> listOf(Field("w", "Base", w), Field("h", "Height", h))
        "tri" -> listOf(Field("ab", "AB", l(0, 1)), Field("bc", "BC", l(1, 2)), Field("ca", "CA", l(2, 0)))
        "quad" -> listOf(
            Field("ab", "Side AB", l(0, 1)),
            Field("bc", "Side BC", l(1, 2)),
            Field("cd", "Side CD", l(2, 3)),
            Field("da", "Side DA", l(3, 0)),
            Field("ac", "Diagonal AC", l(0, 2)),
            Field("bd", "Diagonal BD", l(1, 3), true),
        )
        "trig" -> listOf(Field("th", "Angle θ°", f.ga), Field("h", "Hypotenuse", f.gb))
        "pent", "hex" -> p.indices.map { i ->
            val a = names[i]
            val b = names[(i + 1) % p.size]
            Field("s$i", "Side $a$b", l(i, (i + 1) % p.size), true)
        }
        else -> emptyList()
    }
}

/*
 * Everything the info bar and the Details tab say about a figure is typed maths and is drawn by the maths engine
 * (KaTeX style): variables are italic (A, P, r, ∠B), labels are CAPITALS in upright type, fractions are stacked,
 * powers are raised, roots are real roots. `\text{...}` is a word; letters joined with * stay one run of variables.
 */
private fun kx(s: String): List<Part> = listOf(Part.T(s))

/** A label in capitals, upright: `\text{AREA}`. */
private fun lb(s: String): String = "\\text{" + s.uppercase().replace("{", "").replace("}", "") + "}"

private fun classifyQuad(P: List<Pt>): String {
    val s = List(4) { dist(P[it], P[(it + 1) % 4]) }
    val eps = 1e-6 * maxOf(s[0], 1e-9)
    fun par(i: Int, j: Int): Boolean {
        val ux = P[(i + 1) % 4].x - P[i].x
        val uy = P[(i + 1) % 4].y - P[i].y
        val vx = P[(j + 1) % 4].x - P[j].x
        val vy = P[(j + 1) % 4].y - P[j].y
        return abs(ux * vy - uy * vx) < 1e-6 * hypot(ux, uy) * hypot(vx, vy)
    }
    val p1 = par(0, 2)
    val p2 = par(1, 3)
    val eq = s.all { abs(it - s[0]) < eps }
    val right = abs(angDeg(P[0], P[3], P[1]) - 90) < 1e-4
    return when {
        p1 && p2 && eq && right -> "square"
        p1 && p2 && right -> "rectangle"
        p1 && p2 && eq -> "rhombus"
        p1 && p2 -> "parallelogram"
        p1 || p2 -> "trapezium"
        (abs(s[0] - s[1]) < eps && abs(s[2] - s[3]) < eps) || (abs(s[1] - s[2]) < eps && abs(s[3] - s[0]) < eps) -> "kite"
        else -> "general quadrilateral"
    }
}

private fun triDetails(P: List<Pt>, nm: Nm): List<List<Part>> {
    val a = dist(P[1], P[2])
    val b = dist(P[2], P[0])
    val c = dist(P[0], P[1])
    val ar = polyArea(P)
    val per = a + b + c
    val an = List(3) { angDeg(P[it], P[(it + 2) % 3], P[(it + 1) % 3]) }
    val eps = 1e-6 * maxOf(a, b, c, 1e-9)
    val sideKind = if (abs(a - b) < eps && abs(b - c) < eps) "equilateral"
    else if (abs(a - b) < eps || abs(b - c) < eps || abs(a - c) < eps) "isosceles"
    else "scalene"
    val mx = an.max()
    val angKind = if (abs(mx - 90) < 1e-4) "right-angled" else if (mx > 90) "obtuse-angled" else "acute-angled"
    val out = mutableListOf<List<Part>>()
    out.add(kx(lb("Type: $sideKind, $angKind")))
    out.add(kx("${lb("Area")}  A = ${fmt(ar)}"))
    out.add(kx("${lb("Perimeter")}  P = ${fmt(per)}"))
    out.add(listOf(Part.T("${lb("Inradius")}  r = "), Part.F("A", "s"), Part.T(" = ${fmt(ar / (per / 2))}")))
    out.add(listOf(Part.T("${lb("Circumradius")}  R = "), Part.F("a*b*c", "4A"), Part.T(" = ${fmt(a * b * c / (4 * ar))}")))
    out.add(kx("${lb("Centroid")}  G(${fmt(P.sumOf { it.x } / 3)}, ${fmt(P.sumOf { it.y } / 3)})"))
    val ic = incenter(P[0], P[1], P[2])
    out.add(kx("${lb("Incentre")}  I(${fmt(ic.x)}, ${fmt(ic.y)})"))
    val oc = circumcenter(P[0], P[1], P[2])
    if (oc != null) {
        out.add(kx("${lb("Circumcentre")}  O(${fmt(oc.x)}, ${fmt(oc.y)})"))
        out.add(kx("${lb("Orthocentre")}  H(${fmt(P[0].x + P[1].x + P[2].x - 2 * oc.x)}, ${fmt(P[0].y + P[1].y + P[2].y - 2 * oc.y)})"))
    }
    out.add(kx(lb("Angles") + "  " + (0 until 3).joinToString(", ") { "∠${nm[it]} = ${fmt(an[it], 1)}°" } + ",  ${lb("Sum")} 180°"))
    for (i in 0 until 3) {
        out.add(
            kx(
                "${lb("Side")} ${nm[(i + 1) % 3]}*${nm[(i + 2) % 3]} ${lb("subtends")} ∠${nm[i]} = ${fmt(an[i], 1)}° " +
                    "${lb("at the circumference and")} ${fmt(2 * an[i], 1)}° ${lb("at the centre")} O",
            ),
        )
    }
    return out
}

/** Web: solInfo - volume, surface area and more for the selected solid. */
private fun solidDetails(f: Fig): List<List<Part>> {
    val d = f.sd
    fun g(k: String): Double = d[k] ?: 1.0
    fun t(s: String): Part = Part.T(s)
    fun fr(n: String, dd: String): Part = Part.F(n, dd)
    val out = mutableListOf<List<Part>>(listOf(t(lb(SOLN[f.sk] ?: "Solid"))))
    when (f.sk) {
        "cube" -> {
            val a = g("a")
            out.add(listOf(t("V = a^3 = ${fmt(a * a * a)}")))
            out.add(listOf(t("SA = 6a^2 = ${fmt(6 * a * a)}")))
            out.add(listOf(t("${lb("Space diagonal")}  d = a*sqrt(3) = ${fmt(a * sqrt(3.0))}")))
        }
        "cuboid" -> {
            val l = g("l")
            val w = g("w")
            val h = g("h")
            out.add(listOf(t("V = l*w*h = ${fmt(l * w * h)}")))
            out.add(listOf(t("SA = 2(l*w + l*h + w*h) = ${fmt(2 * (l * w + l * h + w * h))}")))
            out.add(listOf(t("${lb("Space diagonal")}  d = sqrt(l^2 + w^2 + h^2) = ${fmt(hypot(hypot(l, w), h))}")))
        }
        "pyr" -> {
            val l = g("l")
            val w = g("w")
            val h = g("h")
            val s1 = hypot(h, w / 2)
            val s2 = hypot(h, l / 2)
            out.add(listOf(t("V = "), fr("1", "3"), t(" l*w*h = ${fmt(l * w * h / 3)}")))
            out.add(listOf(t("SA = ${fmt(l * w + l * s1 + w * s2)}")))
            out.add(listOf(t("${lb("Slant heights")} ${fmt(s1)} ${lb("and")} ${fmt(s2)}")))
        }
        "prism" -> {
            val l = g("l")
            val w = g("w")
            val h = g("h")
            out.add(listOf(t("V = "), fr("1", "2"), t(" b*h*l = ${fmt(0.5 * l * h * w)}")))
            out.add(listOf(t("SA = ${fmt(l * h + (l + 2 * hypot(h, l / 2)) * w)}")))
        }
        "cyl" -> {
            val r = g("r")
            val h = g("h")
            out.add(listOf(t("V = pi*r^2*h = ${fmt(PI * r * r * h)}")))
            out.add(listOf(t("SA = 2*pi*r(r + h) = ${fmt(2 * PI * r * (r + h))}")))
        }
        "cone" -> {
            val r = g("r")
            val h = g("h")
            val sl = hypot(r, h)
            out.add(listOf(t("V = "), fr("1", "3"), t(" pi*r^2*h = ${fmt(PI * r * r * h / 3)}")))
            out.add(listOf(t("${lb("Slant")}  l = sqrt(r^2 + h^2) = ${fmt(sl)}")))
            out.add(listOf(t("SA = pi*r(r + l) = ${fmt(PI * r * (r + sl))}")))
        }
        "sph" -> {
            val r = g("r")
            out.add(listOf(t("V = "), fr("4", "3"), t(" pi*r^3 = ${fmt(4.0 / 3 * PI * r * r * r)}")))
            out.add(listOf(t("SA = 4*pi*r^2 = ${fmt(4 * PI * r * r)}")))
        }
        "ext", "apx" -> {
            val pl = f.poly
            val n = pl.size
            val h = g("h")
            val ar = polyArea(pl)
            val per = pl.indices.sumOf { dist(pl[it], pl[(it + 1) % n]) }
            if (f.sk == "ext") {
                out.add(listOf(t("${lb("Base area")}  A = ${fmt(ar)},  ${lb("perimeter")}  P = ${fmt(per)}")))
                out.add(listOf(t("V = A*h = ${fmt(ar * h)}")))
                out.add(listOf(t("SA = 2A + P*h = ${fmt(2 * ar + per * h)}")))
            } else {
                val cx = pl.sumOf { it.x } / n
                val cy = pl.sumOf { it.y } / n
                val cc = com.aismartmath.board.model.Pt(cx, cy)
                var lat = 0.0
                for (i in 0 until n) {
                    val q = pl[i]
                    val q2 = pl[(i + 1) % n]
                    lat += 0.5 * dist(q, q2) * hypot(h, dseg(cc, q, q2))
                }
                out.add(listOf(t("${lb("Base area")}  A = ${fmt(ar)}")))
                out.add(listOf(t("V = "), fr("1", "3"), t(" A*h = ${fmt(ar * h / 3)}")))
                out.add(listOf(t("SA = ${fmt(ar + lat)}")))
            }
        }
    }
    return out
}

/** Web: props()/desc() - the full description shown by Details, with fractions. [par] adds parallel-line facts. */
fun propsOf(f: Fig, par: ParGeo? = null): List<List<Part>> {
    if (f.t == "fx") return listOf(graphParts(f))
    if (f.t == "ink") return listOf(kx(lb("A pen or highlighter stroke")))
    if (f.t == "txt") return listOf(kx("${lb("Text")}  ·  ${f.txt.length} ${lb("letters")}"))
    if (f.t == "img") return listOf(kx(lb("A picture — drag the corners to resize, ✂ to crop")))
    if (f.t == "sol") return solidDetails(f)
    fun t(s: String): Part = Part.T(s)
    fun fr(n: String, d: String): Part = Part.F(n, d)
    val P = pts(f)
    val n = P.size
    val a = f.p[0]
    val b = f.p.getOrElse(1) { a }
    val names = Nm.of(f.nm)
    when (f.t) {
        "pt" -> return listOf(listOf(t("${lb("Point")} (${fmt(a.x)}, ${fmt(a.y)})")))
        "seg" -> {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val out = mutableListOf(
                listOf(t("${lb("Length")}  ${fmt(dist(a, b))}")),
                listOf(t("${lb("Angle")}  ${fmt(segAngle(a, b), 1)}°")),
                listOf(t("${lb("Midpoint")}  (${fmt((a.x + b.x) / 2)}, ${fmt((a.y + b.y) / 2)})")),
                if (dx == 0.0) listOf(t(lb("Slope undefined")))
                else listOf(t("${lb("Slope")}  m = "), fr(fmt(dy), fmt(dx)), t(" = ${fmt(dy / dx)}")),
            )
            if (par != null) out.addAll(parDetails(f, par))
            return out
        }
        "circ" -> {
            val r = dist(a, b)
            return listOf(
                listOf(t("${lb("Radius")}  r = ${fmt(r)}")),
                listOf(t("${lb("Diameter")}  d = 2r = ${fmt(2 * r)}")),
                listOf(t("${lb("Circumference")}  C = 2*pi*r = ${fmt(2 * PI * r)}")),
                listOf(t("${lb("Area")}  A = pi*r^2 = ${fmt(PI * r * r)}")),
                listOf(t("${lb("Quarter arc")}  = "), fr("pi*r", "2"), t(" = ${fmt(PI * r / 2)}")),
            )
        }
        "rect" -> {
            val w = abs(b.x - a.x)
            val h = abs(b.y - a.y)
            return listOf(
                listOf(t("${lb(if (w == h) "Square" else "Rectangle")}  ${fmt(w)} × ${fmt(h)}")),
                listOf(t("${lb("Area")}  A = w × h = ${fmt(w * h)}")),
                listOf(t("${lb("Perimeter")}  P = ${fmt(2 * (w + h))}")),
                listOf(t("${lb("Diagonal")}  ${fmt(hypot(w, h))}")),
                listOf(t(lb("Cyclic: its diagonals are diameters of the circumcircle"))),
            )
        }
        "rtri" -> {
            val j = dist(P[0], P[1])
            val o = dist(P[1], P[2])
            val h = dist(P[0], P[2])
            val k = names[0]
            val out = mutableListOf(
                listOf(t("${lb("Right angle at")} ${names[1]}")),
                listOf(t("${lb("Legs")} ${fmt(j)} ${lb("and")} ${fmt(o)},  ${lb("hypotenuse")} ${fmt(h)}")),
                listOf(t("∠$k = ${fmt(atan2(o, j) * 180 / PI, 1)}°")),
                listOf(t("sin $k = "), fr(fmt(o), fmt(h)), t(" = ${fmt(o / h)}")),
                listOf(t("cos $k = "), fr(fmt(j), fmt(h)), t(" = ${fmt(j / h)}")),
                listOf(t("tan $k = "), fr(fmt(o), fmt(j)), t(" = ${fmt(o / j)}")),
            )
            out.addAll(triDetails(P, names))
            return out
        }
        "tri" -> return triDetails(P, names)
        "trig" -> {
            val th = f.ga * PI / 180
            val h = f.gb
            val o = h * sin(th)
            val j = h * cos(th)
            return listOf(
                listOf(t("theta = ${fmt(f.ga, 1)}°  ·  ${lb("hypotenuse")} ${fmt(h)}  ·  ${lb("height (opposite)")} ${fmt(o)}  ·  ${lb("base (adjacent)")} ${fmt(j)}")),
                listOf(t("sin theta = "), fr(fmt(o), fmt(h)), t(" = ${fmt(sin(th))}")),
                listOf(t("cos theta = "), fr(fmt(j), fmt(h)), t(" = ${fmt(cos(th))}")),
                if (abs(j) < 1e-9) listOf(t("tan theta = ${lb("undefined")}"))
                else listOf(t("tan theta = "), fr(fmt(o), fmt(j)), t(" = ${fmt(o / j)}")),
            )
        }
        "unit" -> {
            val d = unitDegOf(b)
            val r = d * PI / 180
            val c = cos(r)
            return listOf(
                listOf(t("theta = $d° = ${fmt(r, 3)} ${lb("rad")}")),
                listOf(t("sin theta = ${fmt(sin(r))}")),
                listOf(t("cos theta = ${fmt(c)}")),
                listOf(t(if (abs(c) < 1e-9) "tan theta = ${lb("undefined")}" else "tan theta = ${fmt(kotlin.math.tan(r))}")),
            )
        }
    }
    // quadrilateral, pentagon, hexagon
    val out = mutableListOf<List<Part>>()
    val per = P.indices.sumOf { dist(P[it], P[(it + 1) % n]) }
    val an = List(n) { angDeg(P[it], P[(it + n - 1) % n], P[(it + 1) % n]) }
    val regular = P.indices.all { abs(dist(P[it], P[(it + 1) % n]) - dist(P[0], P[1])) < 1e-6 * dist(P[0], P[1]) } &&
        an.all { abs(it - an[0]) < 1e-4 }
    if (n == 4) {
        out.add(kx(lb("Type: " + classifyQuad(P))))
    } else {
        out.add(kx(lb((if (regular) "Regular " else "Irregular ") + FigNames[f.t]?.lowercase())))
    }
    out.add(kx("${lb("Area")}  A = ${fmt(polyArea(P))}"))
    out.add(kx("${lb("Perimeter")}  P = ${fmt(per)}"))
    if (n == 4) {
        out.add(kx("${lb("Diagonals")}  d_1 = ${fmt(dist(P[0], P[2]))},  d_2 = ${fmt(dist(P[1], P[3]))}"))
    } else {
        out.add(kx("${n * (n - 3) / 2} ${lb("diagonals")}"))
    }
    out.add(kx(lb("Angles") + "  " + (0 until n).joinToString(", ") { "∠${names[it]} = ${fmt(an[it], 1)}°" }))
    out.add(kx("${lb("Angle sum")}  ${(n - 2) * 180}° = ${n - 2} × 180°"))
    if (isCyclic(P)) {
        val oc = circumcenter(P[0], P[1], P[2])
        if (oc != null) out.add(kx("${lb("Cyclic: all corners lie on a circle, radius")} ${fmt(dist(oc, P[0]))}"))
        if (n == 4) out.add(kx("${lb("Opposite angles add up to")} 180°"))
    }
    return out
}
