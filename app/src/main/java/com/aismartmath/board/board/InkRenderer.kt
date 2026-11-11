package com.aismartmath.board.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Pt

/** A smooth path through screen points (each point is a curve control, curves meet at mid-points). */
private fun smoothPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    if (pts.size == 1) {
        path.lineTo(pts[0].x, pts[0].y)
        return path
    }
    var cx = pts[0].x
    var cy = pts[0].y
    for (i in 1 until pts.size - 1) {
        val p = pts[i]
        val q = pts[i + 1]
        val ex = (p.x + q.x) / 2
        val ey = (p.y + q.y) / 2
        // quadratic curve (cx,cy) -> (ex,ey) with control p, written as a cubic
        path.cubicTo(
            cx + 2f / 3f * (p.x - cx), cy + 2f / 3f * (p.y - cy),
            ex + 2f / 3f * (p.x - ex), ey + 2f / 3f * (p.y - ey),
            ex, ey,
        )
        cx = ex
        cy = ey
    }
    path.lineTo(pts.last().x, pts.last().y)
    return path
}

/** Web: drawS for 'ink' - pen strokes; highlighter strokes are 3x wider and see-through. */
fun DrawScope.drawInk(cam: Camera, f: Fig, a: Float) {
    if (a <= 0f) return
    val c = f.col ?: Color(0xFFFBBF24)
    val w = (if (f.hl) f.inkW * 3 else f.inkW).toFloat().dp.toPx()
    val path = smoothPath(f.p.map { Offset(cam.sx(it.x), cam.sy(it.y)) })
    drawPath(
        path,
        c.copy(alpha = (if (f.hl) 0.35f else 1f) * a),
        style = Stroke(
            w, cap = StrokeCap.Round, join = StrokeJoin.Round,
            pathEffect = when (f.inkStyle) {
                1 -> PathEffect.dashPathEffect(floatArrayOf(w * 3f, w * 2f))
                2 -> PathEffect.dashPathEffect(floatArrayOf(0.1f, w * 2.2f))
                else -> null
            },
        ),
    )
}

/** The stroke while the finger is still down: coloured for pen / highlighter, dotted white for sketch. */
fun DrawScope.drawLiveStroke(pts: List<Offset>, tool: String, col: Color, widthDp: Float) {
    if (pts.size < 2) return
    val path = Path()
    pts.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
    val ink = tool == "pen" || tool == "hl"
    val w = (if (ink) (if (tool == "hl") widthDp * 3 else widthDp) else 3f).dp.toPx()
    val c = if (ink) col.copy(alpha = if (tool == "hl") 0.35f else 1f) else Color.White
    val dash = if (ink) null else PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx()))
    drawPath(path, c, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash))
}

/** The rough sketch fading away as the neat figure pops in. */
fun DrawScope.drawGhost(cam: Camera, pts: List<Pt>, fade: Float) {
    if (pts.size < 2 || fade <= 0f) return
    val path = Path()
    pts.forEachIndexed { i, p ->
        if (i == 0) path.moveTo(cam.sx(p.x), cam.sy(p.y)) else path.lineTo(cam.sx(p.x), cam.sy(p.y))
    }
    val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx()))
    drawPath(path, Color.White.copy(alpha = 0.7f * fade), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash))
}

/** A ring that follows the finger while erasing (its size is the erase reach). */
fun DrawScope.drawEraserRing(at: Offset, radiusDp: Float, pulse: Float) {
    val r = radiusDp.dp.toPx()
    drawCircle(Color.White.copy(alpha = 0.10f), r, at)
    drawCircle(Color.White.copy(alpha = 0.5f + 0.3f * pulse), r, at, style = Stroke(2.dp.toPx()))
}
