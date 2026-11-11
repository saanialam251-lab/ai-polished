package com.aismartmath.board.board

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.graphics.asAndroidBitmap
import com.aismartmath.board.data.ImageStore
import com.aismartmath.board.math.dist
import com.aismartmath.board.math.pts
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Pt
import kotlin.math.max
import kotlin.math.min

/** Turns what the student selected with Content into a picture and a short description for the AI. */
object AiVision {
    private val CLOSED = setOf("tri", "quad", "rect", "rtri", "pent", "hex", "trig")

    fun bounds(g: List<Fig>): DoubleArray? {
        val bs = g.filter { it.dieAt == 0L }.mapNotNull { figBounds(it) }
        if (bs.isEmpty()) return null
        return doubleArrayOf(bs.minOf { it[0] }, bs.minOf { it[1] }, bs.maxOf { it[2] }, bs.maxOf { it[3] })
    }

    private fun r2(v: Double) = (Math.round(v * 100.0) / 100.0).toString()

    fun describe(g: List<Fig>): String {
        val sb = StringBuilder()
        for (f in g.filter { it.dieAt == 0L }) {
            when (f.t) {
                "txt" -> sb.append("Text on the board: ").append(f.txt).append('\n')
                "img" -> sb.append("A picture (see the attached image).\n")
                "ink" -> sb.append("Hand-drawn pen strokes (see the attached image).\n")
                "fx" -> sb.append("Graph of: ").append(f.gd?.src ?: "?").append('\n')
                "sol" -> sb.append("3D solid: ").append(f.sk).append(" with ").append(f.sd).append('\n')
                "circ" -> sb.append("Circle centre (").append(r2(f.p[0].x)).append(", ").append(r2(f.p[0].y)).append("), radius ").append(r2(dist(f.p[0], f.p[1]))).append('\n')
                "pt" -> sb.append("Point (").append(r2(f.p[0].x)).append(", ").append(r2(f.p[0].y)).append(")\n")
                else -> {
                    val q = try { pts(f) } catch (e: Exception) { f.p }
                    sb.append(f.t).append(if (f.nm != null) " ${f.nm}" else "").append(": ")
                    sb.append(q.joinToString(" ") { "(" + r2(it.x) + ", " + r2(it.y) + ")" }).append('\n')
                }
            }
        }
        return sb.toString().trim()
    }

    fun render(g: List<Fig>): Bitmap? {
        val b = bounds(g) ?: return null
        val padX = max(0.05 * (b[2] - b[0]), 0.3)
        val padY = max(0.05 * (b[3] - b[1]), 0.3)
        val x0 = b[0] - padX
        val y1 = b[3] + padY
        val w = (b[2] + padX) - x0
        val h = y1 - (b[1] - padY)
        val s = (1400.0 / max(w, h)).coerceAtMost(400.0)
        val bw = (w * s).toInt().coerceAtLeast(40)
        val bh = (h * s).toInt().coerceAtLeast(40)
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        fun X(x: Double) = ((x - x0) * s).toFloat()
        fun Y(y: Double) = ((y1 - y) * s).toFloat()
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 4f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
        val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 34f }
        for (f in g.filter { it.dieAt == 0L }) {
            when (f.t) {
                "img" -> {
                    val img = ImageStore.get(f.imgKey)?.asAndroidBitmap()
                    if (img != null && f.p.size >= 2) {
                        val cr = f.crop
                        val src = Rect(
                            (cr[0] * img.width).toInt().coerceIn(0, img.width - 1),
                            (cr[1] * img.height).toInt().coerceIn(0, img.height - 1),
                            (cr[2] * img.width).toInt().coerceIn(1, img.width),
                            (cr[3] * img.height).toInt().coerceIn(1, img.height),
                        )
                        c.drawBitmap(img, src, RectF(X(f.p[0].x), Y(f.p[0].y), X(f.p[1].x), Y(f.p[1].y)), Paint(Paint.FILTER_BITMAP_FLAG))
                    }
                }
                "ink" -> {
                    if (f.p.size >= 2) {
                        val path = Path()
                        path.moveTo(X(f.p[0].x), Y(f.p[0].y))
                        for (i in 1 until f.p.size) path.lineTo(X(f.p[i].x), Y(f.p[i].y))
                        c.drawPath(path, line)
                    } else if (f.p.size == 1) {
                        c.drawCircle(X(f.p[0].x), Y(f.p[0].y), 3f, fill)
                    }
                }
                "seg" -> {
                    c.drawLine(X(f.p[0].x), Y(f.p[0].y), X(f.p[1].x), Y(f.p[1].y), line)
                    nameAt(c, txt, f.nm, listOf(f.p[0], f.p[1]), ::X, ::Y)
                }
                "circ" -> c.drawCircle(X(f.p[0].x), Y(f.p[0].y), (dist(f.p[0], f.p[1]) * s).toFloat(), line)
                "pt" -> c.drawCircle(X(f.p[0].x), Y(f.p[0].y), 6f, fill)
                "txt" -> {
                    txt.textSize = (f.fsz * s).toFloat().coerceIn(18f, 90f)
                    var y = Y(f.p[0].y) + txt.textSize
                    for (ln in f.txt.lines()) {
                        c.drawText(ln, X(f.p[0].x), y, txt)
                        y += txt.textSize * 1.25f
                    }
                    txt.textSize = 34f
                }
                else -> if (f.t in CLOSED) {
                    val q = try { pts(f) } catch (e: Exception) { f.p }
                    if (q.size >= 3) {
                        val path = Path()
                        path.moveTo(X(q[0].x), Y(q[0].y))
                        for (i in 1 until q.size) path.lineTo(X(q[i].x), Y(q[i].y))
                        path.close()
                        c.drawPath(path, line)
                        nameAt(c, txt, f.nm, q, ::X, ::Y)
                    }
                }
            }
        }
        return bmp
    }

    private fun nameAt(c: Canvas, p: Paint, nm: String?, q: List<Pt>, X: (Double) -> Float, Y: (Double) -> Float) {
        if (nm.isNullOrEmpty()) return
        for (i in 0 until min(nm.length, q.size)) c.drawText(nm[i].toString(), X(q[i].x) + 8f, Y(q[i].y) - 8f, p)
    }
}
