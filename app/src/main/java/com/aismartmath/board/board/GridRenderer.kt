package com.aismartmath.board.board

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

private val GridFaint = Color.White.copy(alpha = 0.07f)
private val AxisColor = Color.White.copy(alpha = 0.44f)
private val LabelColor = Color(0xFF9CA3AF)
private val OutlineColor = Color(0xFF1A110C)

/** Web: f(n,d) = String(+n.toFixed(d)). */
fun fmt(n: Double, d: Int = 2): String {
    var s = String.format(Locale.US, "%.${d}f", n)
    if (s.contains('.')) s = s.trimEnd('0').trimEnd('.')
    if (s == "-0" || s.isEmpty()) s = "0"
    return s
}

/**
 * Web: T(). A label on a small dark rounded backing, so numbers and letters stay sharp and readable over
 * lines and fills. The size does not change with zoom.
 */
fun DrawScope.outlinedText(
    m: TextMeasurer,
    text: String,
    cx: Float,
    cy: Float,
    color: Color = Color.White,
    sizeSp: Float = 12f,
    alpha: Float = 1f,
    bgAlpha: Float = 0.8f,
    serif: Boolean = false,
) {
    if (text.isEmpty()) return
    val style = if (serif) {
        TextStyle(fontSize = sizeSp.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, letterSpacing = 0.5.sp)
    } else {
        TextStyle(fontSize = sizeSp.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
    }
    val layout = m.measure(text, style)
    val w = layout.size.width.toFloat()
    val h = layout.size.height.toFloat()
    val tl = Offset(cx - w / 2f, cy - h / 2f)
    val padX = 3.dp.toPx()
    val padY = 0.5.dp.toPx()
    drawRoundRect(
        OutlineColor.copy(alpha = bgAlpha * alpha),
        Offset(tl.x - padX, tl.y - padY),
        Size(w + 2 * padX, h + 2 * padY),
        CornerRadius(5.dp.toPx()),
    )
    // a fine edge in the text's own colour gives the label a finished, premium look
    drawRoundRect(
        color.copy(alpha = 0.35f * alpha),
        Offset(tl.x - padX, tl.y - padY),
        Size(w + 2 * padX, h + 2 * padY),
        CornerRadius(5.dp.toPx()),
        style = androidx.compose.ui.graphics.drawscope.Stroke(0.7.dp.toPx()),
    )
    drawText(layout, color, tl, alpha)
}

/** Web: grid(). Grid and numbers re-space themselves at every zoom level; axes only when [axes]. */
fun DrawScope.drawGrid(cam: Camera, axes: Boolean, m: TextMeasurer) {
    if (cam.w <= 0.0 || cam.h <= 0.0) return
    val u = cam.u
    val st = cam.gridStep
    val x0 = -(cam.w / 2 + cam.ox) / u
    val x1 = (cam.w / 2 - cam.ox) / u
    val y1 = (cam.h / 2 + cam.oy) / u
    val y0 = -(cam.h / 2 - cam.oy) / u
    val i0 = floor(x0 / st).toInt()
    val i1 = ceil(x1 / st).toInt()
    val j0 = floor(y0 / st).toInt()
    val j1 = ceil(y1 / st).toInt()
    val w = size.width
    val h = size.height
    val lw = 1.dp.toPx()

    for (i in i0..i1) {
        val x = (cam.w / 2 + cam.ox + i * st * u).toFloat()
        drawLine(if (i == 0 && axes) AxisColor else GridFaint, Offset(x, 0f), Offset(x, h), lw)
    }
    for (j in j0..j1) {
        val y = (cam.h / 2 + cam.oy - j * st * u).toFloat()
        drawLine(if (j == 0 && axes) AxisColor else GridFaint, Offset(0f, y), Offset(w, y), lw)
    }
    if (axes) {
        val ly = (cam.h / 2 + cam.oy + 11.dp.toPx()).toFloat().coerceIn(9.dp.toPx(), h - 9.dp.toPx())
        val lx = (cam.w / 2 + cam.ox - 10.dp.toPx()).toFloat().coerceIn(12.dp.toPx(), w - 12.dp.toPx())
        for (i in i0..i1) if (i != 0) {
            outlinedText(m, fmt(i * st, 4), (cam.w / 2 + cam.ox + i * st * u).toFloat(), ly, LabelColor, 10f, 1f, 0.55f)
        }
        for (j in j0..j1) if (j != 0) {
            outlinedText(m, fmt(j * st, 4), lx, (cam.h / 2 + cam.oy - j * st * u).toFloat(), LabelColor, 10f, 1f, 0.55f)
        }
    }
}
