package com.aismartmath.board.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.aismartmath.board.math.MathCache
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.data.ImageStore
import com.aismartmath.board.model.Fig

val TextDefault = Color(0xFFFFF1DC)
private val Copper = Color(0xFFD08A4E)
private val Cream = Color(0xFFF1D3A5)

/** A picture: the visible (cropped) part of the stored image, stretched into the box p[0] .. p[1]. */
fun DrawScope.drawPicture(cam: Camera, f: Fig, a: Float) {
    if (a <= 0f || f.p.size < 2) return
    val img = ImageStore.get(f.imgKey)
    val l = cam.sx(f.p[0].x)
    val t = cam.sy(f.p[0].y)
    val r = cam.sx(f.p[1].x)
    val b = cam.sy(f.p[1].y)
    val w = (r - l).toInt()
    val h = (b - t).toInt()
    if (w < 1 || h < 1) return
    if (img == null) {
        drawRect(Color(0xFF3B271B).copy(alpha = a), Offset(l, t), Size(w.toFloat(), h.toFloat()))
        return
    }
    val c = f.crop
    val sx = (c[0] * img.width).toInt().coerceIn(0, img.width - 1)
    val sy = (c[1] * img.height).toInt().coerceIn(0, img.height - 1)
    val sw = ((c[2] - c[0]) * img.width).toInt().coerceIn(1, img.width - sx)
    val sh = ((c[3] - c[1]) * img.height).toInt().coerceIn(1, img.height - sy)
    drawImage(
        image = img,
        srcOffset = IntOffset(sx, sy),
        srcSize = IntSize(sw, sh),
        dstOffset = IntOffset(l.toInt(), t.toInt()),
        dstSize = IntSize(w, h),
        alpha = a,
        filterQuality = FilterQuality.Medium,
    )
}

/** Pasted text: wraps inside the box p[0] .. p[1]; the letter height scales with the board. */
fun DrawScope.drawTextFig(cam: Camera, m: TextMeasurer, f: Fig, a: Float) {
    if (a <= 0f || f.p.size < 2 || f.txt.isEmpty()) return
    val px = (f.fsz * cam.u).toFloat()
    if (px < 2f || px > 1500f) return
    val col = f.col ?: TextDefault
    val wRatio = ((f.p[1].x - f.p[0].x) / f.fsz).toFloat().coerceAtLeast(0.5f)
    val mt = MathCache.text(f.txt, wRatio)
    val argb = col.copy(alpha = a).toArgb()
    val k = px / MathCache.REF
    val x0 = cam.sx(f.p[0].x)
    val y0 = cam.sy(f.p[0].y)
    drawIntoCanvas { c -> mt.draw(c.nativeCanvas, x0, y0, k, argb) }
}

private fun DrawScope.dashed(): PathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))

/** The box round everything that was picked with the sketch tool's "Drag" mode, with a corner to resize it. */
fun DrawScope.drawGroupBox(cam: Camera, b: DoubleArray, phase: Float) {
    val pad = 4.dp.toPx()
    val l = cam.sx(b[0]) - pad
    val t = cam.sy(b[3]) - pad
    val r = cam.sx(b[2]) + pad
    val bt = cam.sy(b[1]) + pad
    drawRect(Copper.copy(alpha = 0.10f), Offset(l, t), Size(r - l, bt - t), style = Fill)
    drawRect(
        Cream, Offset(l, t), Size(r - l, bt - t),
        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()), phase.dp.toPx())),
    )
    val hs = 7.dp.toPx()
    drawRect(Cream, Offset(r - hs, bt - hs), Size(hs * 2, hs * 2), style = Fill)
    drawRect(Copper, Offset(r - hs, bt - hs), Size(hs * 2, hs * 2), style = Stroke(2.dp.toPx()))
}

/** The rubber-band box while the finger drags it. */
fun DrawScope.drawLiveBox(a: Offset, b: Offset) {
    val l = minOf(a.x, b.x)
    val t = minOf(a.y, b.y)
    val w = kotlin.math.abs(a.x - b.x)
    val h = kotlin.math.abs(a.y - b.y)
    drawRect(Copper.copy(alpha = 0.14f), Offset(l, t), Size(w, h), style = Fill)
    drawRect(Cream, Offset(l, t), Size(w, h), style = Stroke(1.5.dp.toPx(), pathEffect = dashed()))
}

/** Crop frame of a picture: four big corner handles. */
fun DrawScope.drawCropFrame(cam: Camera, f: Fig) {
    if (f.p.size < 2) return
    val l = cam.sx(f.p[0].x)
    val t = cam.sy(f.p[0].y)
    val r = cam.sx(f.p[1].x)
    val b = cam.sy(f.p[1].y)
    drawRect(Cream, Offset(l, t), Size(r - l, b - t), style = Stroke(2.dp.toPx(), pathEffect = dashed()))
    val hs = 8.dp.toPx()
    for (o in listOf(Offset(l, t), Offset(r, t), Offset(r, b), Offset(l, b))) {
        drawRect(Cream, Offset(o.x - hs, o.y - hs), Size(hs * 2, hs * 2), style = Fill)
        drawRect(Copper, Offset(o.x - hs, o.y - hs), Size(hs * 2, hs * 2), style = Stroke(2.dp.toPx()))
    }
}

/** Selection frame for pictures and text. */
fun DrawScope.drawBoxSelection(cam: Camera, f: Fig, phase: Float, pulse: Float) {
    if (f.p.size < 2) return
    val l = cam.sx(f.p[0].x)
    val t = cam.sy(f.p[0].y)
    val r = cam.sx(f.p[1].x)
    val b = cam.sy(f.p[1].y)
    val line = androidx.compose.ui.graphics.lerp(Color.White, Copper, pulse)
    drawRect(
        line, Offset(l, t), Size(r - l, b - t),
        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), phase.dp.toPx())),
    )
}
