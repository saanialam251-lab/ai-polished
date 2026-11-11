package com.aismartmath.board.math

import com.aismartmath.board.model.Fig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Web: SOLD - the starting size of each solid. */
val SOLD: Map<String, Map<String, Double>> = mapOf(
    "cube" to mapOf("a" to 3.0),
    "cuboid" to mapOf("l" to 4.0, "w" to 2.0, "h" to 3.0),
    "pyr" to mapOf("l" to 3.0, "w" to 3.0, "h" to 4.0),
    "prism" to mapOf("l" to 3.0, "h" to 3.0, "w" to 4.0),
    "cyl" to mapOf("r" to 1.5, "h" to 4.0),
    "cone" to mapOf("r" to 1.5, "h" to 4.0),
    "sph" to mapOf("r" to 2.0),
)

/** Web: SOLN */
val SOLN: Map<String, String> = mapOf(
    "cube" to "Cube", "cuboid" to "Cuboid", "pyr" to "Square pyramid", "prism" to "Triangular prism",
    "cyl" to "Cylinder", "cone" to "Cone", "sph" to "Sphere", "ext" to "Prism (my base)", "apx" to "Pyramid (my base)",
)

val SOLT = setOf("cube", "cuboid", "pyr", "prism", "cyl", "cone", "sph")

/** Corners (x, y, z), edges and faces of a solid. */
class Solid(val v: List<DoubleArray>, val e: List<IntArray>, val f: List<IntArray>)

private fun ia(vararg x: Int): IntArray = x

private fun va(x: Double, y: Double, z: Double): DoubleArray = doubleArrayOf(x, y, z)

/** Web: geo(s) - the corners, edges and faces of a solid at its current size. */
fun solidGeo(s: Fig): Solid {
    val d = s.sd
    fun g(k: String): Double = d[k] ?: 1.0
    val n = 28
    val edges = mutableListOf<IntArray>()
    val faces = mutableListOf<IntArray>()

    fun ring(r: Double, y: Double): List<DoubleArray> =
        List(n) { i -> va(r * cos(i * 2 * PI / n), y, r * sin(i * 2 * PI / n)) }

    fun box(l: Double, w: Double, h: Double): List<DoubleArray> = listOf(
        va(-l / 2, -h / 2, -w / 2), va(l / 2, -h / 2, -w / 2), va(l / 2, -h / 2, w / 2), va(-l / 2, -h / 2, w / 2),
        va(-l / 2, h / 2, -w / 2), va(l / 2, h / 2, -w / 2), va(l / 2, h / 2, w / 2), va(-l / 2, h / 2, w / 2),
    )

    val verts: List<DoubleArray> = when (s.sk) {
        "cube", "cuboid" -> {
            edges.addAll(
                listOf(
                    ia(0, 1), ia(1, 2), ia(2, 3), ia(3, 0), ia(4, 5), ia(5, 6), ia(6, 7), ia(7, 4),
                    ia(0, 4), ia(1, 5), ia(2, 6), ia(3, 7),
                ),
            )
            faces.addAll(listOf(ia(0, 1, 2, 3), ia(4, 5, 6, 7), ia(0, 1, 5, 4), ia(1, 2, 6, 5), ia(2, 3, 7, 6), ia(3, 0, 4, 7)))
            if (s.sk == "cube") box(g("a"), g("a"), g("a")) else box(g("l"), g("w"), g("h"))
        }
        "pyr" -> {
            val l = g("l")
            val w = g("w")
            val h = g("h")
            edges.addAll(listOf(ia(0, 1), ia(1, 2), ia(2, 3), ia(3, 0), ia(0, 4), ia(1, 4), ia(2, 4), ia(3, 4)))
            faces.addAll(listOf(ia(0, 1, 2, 3), ia(0, 1, 4), ia(1, 2, 4), ia(2, 3, 4), ia(3, 0, 4)))
            listOf(va(-l / 2, -h / 2, -w / 2), va(l / 2, -h / 2, -w / 2), va(l / 2, -h / 2, w / 2), va(-l / 2, -h / 2, w / 2), va(0.0, h / 2, 0.0))
        }
        "prism" -> {
            val l = g("l")
            val w = g("w")
            val h = g("h")
            edges.addAll(listOf(ia(0, 1), ia(1, 2), ia(2, 0), ia(3, 4), ia(4, 5), ia(5, 3), ia(0, 3), ia(1, 4), ia(2, 5)))
            faces.addAll(listOf(ia(0, 1, 2), ia(3, 4, 5), ia(0, 1, 4, 3), ia(1, 2, 5, 4), ia(2, 0, 3, 5)))
            listOf(
                va(-l / 2, -h / 2, -w / 2), va(l / 2, -h / 2, -w / 2), va(0.0, h / 2, -w / 2),
                va(-l / 2, -h / 2, w / 2), va(l / 2, -h / 2, w / 2), va(0.0, h / 2, w / 2),
            )
        }
        "cyl", "cone" -> {
            val r = g("r")
            val h = g("h")
            val cyl = s.sk == "cyl"
            val b = ring(r, -h / 2)
            val t: List<DoubleArray> = if (cyl) ring(r, h / 2) else listOf(va(0.0, h / 2, 0.0))
            for (i in 0 until n) {
                edges.add(ia(i, (i + 1) % n))
                if (cyl) edges.add(ia(n + i, n + (i + 1) % n))
            }
            if (cyl) {
                for (i in 0 until n step 7) edges.add(ia(i, n + i))
                faces.add(IntArray(n) { it })
                faces.add(IntArray(n) { n + it })
                for (i in 0 until n) faces.add(ia(i, (i + 1) % n, n + (i + 1) % n, n + i))
            } else {
                for (i in 0 until n step 7) edges.add(ia(i, n))
                faces.add(IntArray(n) { it })
                for (i in 0 until n) faces.add(ia(i, (i + 1) % n, n))
            }
            b + t
        }
        "ext", "apx" -> {
            val pb = s.poly
            val m = pb.size
            val hh = g("h") / 2
            val ext = s.sk == "ext"
            val base = pb.map { va(it.x, -hh, -it.y) }
            val all: List<DoubleArray> = if (ext) base + pb.map { va(it.x, hh, -it.y) } else base + listOf(va(0.0, hh, 0.0))
            for (i in 0 until m) {
                val j = (i + 1) % m
                edges.add(ia(i, j))
                if (ext) {
                    edges.add(ia(m + i, m + j))
                    edges.add(ia(i, m + i))
                } else {
                    edges.add(ia(i, m))
                }
            }
            faces.add(IntArray(m) { it })
            if (ext) {
                faces.add(IntArray(m) { m + it })
                for (i in 0 until m) faces.add(ia(i, (i + 1) % m, m + (i + 1) % m, m + i))
            } else {
                for (i in 0 until m) faces.add(ia(i, (i + 1) % m, m))
            }
            all
        }
        else -> {
            // sphere: three great circles
            val out = ArrayList<DoubleArray>()
            val r = g("r")
            for (ax in 0..2) {
                for (i in 0..36) {
                    val a = i * PI / 18
                    val c = r * cos(a)
                    val q = r * sin(a)
                    out.add(if (ax == 0) va(c, q, 0.0) else if (ax == 1) va(c, 0.0, q) else va(0.0, c, q))
                    if (i > 0) edges.add(ia(out.size - 2, out.size - 1))
                }
            }
            out
        }
    }
    return Solid(verts, edges, faces)
}
