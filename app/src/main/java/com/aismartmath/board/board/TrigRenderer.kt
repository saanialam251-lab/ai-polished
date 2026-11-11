package com.aismartmath.board.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Pt
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sin

/** Web: TH - the angle of a point in whole degrees, 0 to 359. */
fun unitDegOf(b: Pt): Int = ((round(atan2(b.y, b.x) * 180 / PI)).toInt() + 360) % 360

/** Web: the words adjacent / opposite / hypotenuse beside the sides of the trig triangle. */
fun DrawScope.drawTrigWords(cam: Camera, m: TextMeasurer, P: List<Pt>, a: Float) {
    val g = Offset(P.sumOf { cam.sx(it.x).toDouble() }.toFloat() / 3, P.sumOf { cam.sy(it.y).toDouble() }.toFloat() / 3)
    val sides = listOf(Triple(0, 1, "adjacent"), Triple(1, 2, "opposite"), Triple(0, 2, "hypotenuse"))
    for ((i, j, word) in sides) {
        val mx = (cam.sx(P[i].x) + cam.sx(P[j].x)) / 2
        val my = (cam.sy(P[i].y) + cam.sy(P[j].y)) / 2
        val dx = mx - g.x
        val dy = my - g.y
        val l = hypot(dx, dy).let { if (it == 0f) 1f else it }
        val d = 36.dp.toPx()
        outlinedText(m, word, mx + dx / l * d, my + dy / l * d, Color(0xFFCBD5E1), 11f, a)
    }
}

/** Web: the unit circle with the angle, the cos side (blue) and the sin side (red). */
fun DrawScope.drawUnit(cam: Camera, m: TextMeasurer, f: Fig, a: Float) {
    if (a <= 0f || f.p.size < 2) return
    val c = f.col ?: figColor("unit")
    val deg = unitDegOf(f.p[1])
    val t = deg * PI / 180
    val ru = 3.0
    val ox = cam.sx(0.0)
    val oy = cam.sy(0.0)
    val pxw = ru * cos(t)
    val pyw = ru * sin(t)
    val pp = Offset(cam.sx(pxw), cam.sy(pyw))
    val foot = Offset(cam.sx(pxw), oy)
    val origin = Offset(ox, oy)
    val rpx = (ru * cam.u).toFloat()
    val blue = Color(0xFF60A5FA)
    val red = Color(0xFFF87171)
    drawCircle(c.copy(alpha = 0.07f * a), rpx, origin, style = Fill)
    drawCircle(c.copy(alpha = a), rpx, origin, style = Stroke(2.dp.toPx()))
    drawLine(Color.White.copy(alpha = a), origin, pp, 2.dp.toPx())
    drawLine(blue.copy(alpha = a), origin, foot, 2.dp.toPx())
    drawLine(red.copy(alpha = a), foot, pp, 2.dp.toPx())
    drawCircle(c.copy(alpha = a), 5.dp.toPx(), pp)
    // the angle: an arc of radius 1, counter-clockwise from the x axis
    if (deg > 0) {
        val u = cam.u.toFloat()
        drawArc(
            Color(0xFFFBBF24).copy(alpha = a), 0f, -deg.toFloat(), false,
            Offset(ox - u, oy - u), Size(2 * u, 2 * u), style = Stroke(2.dp.toPx()),
        )
    }
    outlinedText(m, "cos " + fmt(cos(t)), cam.sx(pxw / 2), oy + (if (pyw >= 0) 16 else -16).dp.toPx(), blue, 12f, a)
    outlinedText(m, "sin " + fmt(sin(t)), pp.x + (if (pxw >= 0) 36 else -36).dp.toPx(), cam.sy(pyw / 2), red, 12f, a)
    outlinedText(
        m, "$deg°",
        ox + (cos(t / 2) * cam.u * 1.5).toFloat(), oy - (sin(t / 2) * cam.u * 1.5).toFloat(),
        Color(0xFFFBBF24), 12f, a,
    )
}
