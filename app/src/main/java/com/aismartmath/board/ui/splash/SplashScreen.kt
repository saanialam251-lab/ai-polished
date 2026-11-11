package com.aismartmath.board.ui.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.ui.theme.BoardColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

private fun seg(t: Float, a: Float, b: Float): Float = ((t - a) / (b - a)).coerceIn(0f, 1f)

private fun ease(x: Float): Float = 1f - (1f - x) * (1f - x) * (1f - x)

/** Ease with a little overshoot, for things that pop. */
private fun back(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}

private val GLYPHS = listOf("π", "√", "∑", "θ", "∞", "Δ", "∫", "≈", "°", "x²", "sin", "½")

private val MESSAGES = listOf(
    "Sharpening the pencils …",
    "Rolling out the grid …",
    "Measuring the angles …",
    "Checking the maths …",
    "Ready!",
)

/**
 * The opening animation. A board plate pops up, a triangle and its circumcircle draw themselves around it (the circle
 * really passes through the three corners), the corner letters and an angle arc appear, a ring around the logo fills
 * as the app loads, the name rises letter by letter, and at the end a shock-wave ring rolls out as the logo swells -
 * then the app fades and zooms in underneath. Maths symbols drift up behind it all.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val t = remember { Animatable(0f) }
    val fin = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(2900, easing = LinearEasing))
        fin.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        onFinished()
    }

    val inf = rememberInfiniteTransition(label = "splashInf")
    val tw by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "twinkle")
    val drift by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "drift")
    val measurer = rememberTextMeasurer()
    val msg by remember { derivedStateOf { MESSAGES[(t.value * 4.999f).toInt().coerceIn(0, 4)] } }

    Box(Modifier.fillMaxSize()) {
        // ---- background: glow, drifting maths symbols, and the finishing shock-wave ----
        Canvas(Modifier.fillMaxSize()) { runCatching {
            drawRect(
                Brush.radialGradient(
                    listOf(BoardColors.Glow, BoardColors.Bg),
                    center = Offset(size.width * 0.5f, size.height * 0.4f),
                    radius = maxOf(size.width, size.height) * 0.75f,
                ),
            )
            val appear = ease(seg(t.value, 0.05f, 0.4f))
            val style = TextStyle(
                color = BoardColors.Indigo.copy(alpha = 0.11f * appear),
                fontSize = 26.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
            )
            GLYPHS.forEachIndexed { i, g ->
                val xf = (i * 0.618f + 0.07f) % 1f
                val yf = 1.1f - (drift * (1 + i % 3) + i * 0.17f) % 1.2f
                // only while fully on screen, so the text never gets a zero or negative size to lay out in
                if (yf in 0f..0.92f && xf < 0.88f) {
                    runCatching { drawText(measurer, g, Offset(xf * size.width, yf * size.height), style, softWrap = false, maxLines = 1) }
                }
            }
            // shock-wave at the very end
            val f = fin.value
            if (f > 0f) {
                val c = Offset(size.width / 2, size.height * 0.4f)
                val r = f * hypot(size.width, size.height) * 0.6f
                drawCircle(BoardColors.Indigo.copy(alpha = 0.5f * (1f - f)), r, c, style = Stroke(6f * (1f - f) + 1f))
                drawCircle(BoardColors.Teal.copy(alpha = 0.3f * (1f - f)), r * 0.7f, c, style = Stroke(3f))
            }
        } }

        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            // ---- the logo ----
            Canvas(
                Modifier
                    .size(210.dp)
                    .graphicsLayer {
                        val k = 1f + 0.14f * ease(fin.value)
                        scaleX = k
                        scaleY = k
                    },
            ) { runCatching { drawLogo(t.value, tw, measurer) } }

            Spacer(Modifier.height(10.dp))

            // ---- the name, letter by letter ----
            val letters = remember {
                buildList<Pair<Char, Color>> {
                    "Smart".forEach { add(it to Color.White) }
                    add(' ' to Color.White)
                    "Math".forEach { add(it to BoardColors.Indigo) }
                    add(' ' to Color.White)
                    "Board".forEach { add(it to Color.White) }
                }
            }
            Row {
                letters.forEachIndexed { i, (ch, col) ->
                    Text(
                        ch.toString(),
                        color = col,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.graphicsLayer {
                            val p = seg(t.value, 0.26f + i * 0.016f, 0.46f + i * 0.016f)
                            alpha = p
                            translationY = (1f - back(p)) * 22.dp.toPx()
                            val sc = 0.6f + 0.4f * ease(p)
                            scaleX = sc
                            scaleY = sc
                        },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Draw · Measure · Learn",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.graphicsLayer {
                    val p = seg(t.value, 0.5f, 0.7f)
                    alpha = 0.65f * p
                    translationY = (1f - ease(p)) * 10.dp.toPx()
                },
            )
            Spacer(Modifier.height(18.dp))
            // what it is "doing" - changes with the loading ring
            Box(Modifier.height(20.dp)) {
                AnimatedContent(
                    targetState = msg,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "splashMsg",
                ) { m ->
                    Text(
                        m,
                        color = if (m == "Ready!") BoardColors.Indigo else Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        fontWeight = if (m == "Ready!") FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

private fun DrawScope.star(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticBezierTo(c.x, c.y, c.x + r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y + r)
        quadraticBezierTo(c.x, c.y, c.x - r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color, style = Fill)
}

/** The logo, drawn in a 200 x 200 space. [p] is the loading progress 0..1, [tw] the looping twinkle phase. */
private fun DrawScope.drawLogo(p: Float, tw: Float, measurer: androidx.compose.ui.text.TextMeasurer) {
    val s = size.width / 200f
    val mid = Offset(100f * s, 100f * s)
    fun o(x: Float, y: Float) = Offset(x * s, y * s)

    // breathing glow
    val glow = (0.34f + 0.12f * sin(tw * 2f * PI.toFloat())) * ease(seg(p, 0f, 0.2f))
    drawCircle(
        Brush.radialGradient(listOf(BoardColors.Indigo.copy(alpha = glow), Color.Transparent), center = mid, radius = 100f * s),
        radius = 100f * s,
        center = mid,
    )

    // loading ring: a track, the filled part, and a bright head
    val rr = 94f * s
    drawCircle(Color.White.copy(alpha = 0.09f), rr, mid, style = Stroke(3f * s))
    val ringP = seg(p, 0.04f, 0.94f)
    drawArc(
        BoardColors.Indigo, -90f, 360f * ringP, false,
        topLeft = Offset(mid.x - rr, mid.y - rr), size = Size(rr * 2, rr * 2),
        style = Stroke(4f * s, cap = StrokeCap.Round),
    )
    if (ringP in 0.001f..0.999f) {
        val a = (-90f + 360f * ringP) * PI.toFloat() / 180f
        val head = Offset(mid.x + rr * kotlin.math.cos(a), mid.y + rr * sin(a))
        drawCircle(Color.White, 4.5f * s, head)
        drawCircle(BoardColors.Indigo.copy(alpha = 0.4f), 9f * s, head)
    }

    // the board plate pops up
    val pop = back(seg(p, 0f, 0.2f)).coerceAtLeast(0f)
    withTransform({ scale(pop, pop, mid) }) {
        val corner = CornerRadius(26f * s)
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF3A2519), Color(0xFF1B100B)), startY = 40f * s, endY = 160f * s),
            topLeft = o(40f, 40f), size = Size(120f * s, 120f * s), cornerRadius = corner,
        )
        drawRoundRect(
            BoardColors.Indigo.copy(alpha = 0.75f),
            topLeft = o(40f, 40f), size = Size(120f * s, 120f * s), cornerRadius = corner, style = Stroke(2f * s),
        )
        // faint grid fades in
        val ga = 0.16f * ease(seg(p, 0.08f, 0.3f))
        for (i in 1..3) {
            val k = 40f + i * 30f
            drawLine(Color.White.copy(alpha = ga), o(k, 50f), o(k, 150f), 1f * s)
            drawLine(Color.White.copy(alpha = ga), o(50f, k), o(150f, k), 1f * s)
        }

        // triangle ABC and the circle through its three corners
        val a = o(64f, 132f)
        val b = o(138f, 132f)
        val c = o(101f, 64f)
        val tri = Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }
        val d = 2f * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y))
        val a2 = a.x * a.x + a.y * a.y
        val b2 = b.x * b.x + b.y * b.y
        val c2 = c.x * c.x + c.y * c.y
        val cc = Offset(
            (a2 * (b.y - c.y) + b2 * (c.y - a.y) + c2 * (a.y - b.y)) / d,
            (a2 * (c.x - b.x) + b2 * (a.x - c.x) + c2 * (b.x - a.x)) / d,
        )
        val cr = hypot(a.x - cc.x, a.y - cc.y)

        // faint fill once the triangle is whole
        drawPath(tri, BoardColors.Indigo.copy(alpha = 0.14f * seg(p, 0.5f, 0.62f)), style = Fill)

        val triP = ease(seg(p, 0.14f, 0.5f))
        val pm = PathMeasure()
        pm.setPath(tri, false)
        val part = Path()
        pm.getSegment(0f, pm.length * triP, part, true)
        drawPath(part, BoardColors.Indigo.copy(alpha = 0.28f), style = Stroke(10f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(part, BoardColors.Indigo, style = Stroke(4.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val circP = ease(seg(p, 0.32f, 0.64f))
        val arcTL = Offset(cc.x - cr, cc.y - cr)
        val arcSz = Size(cr * 2, cr * 2)
        drawArc(BoardColors.Teal.copy(alpha = 0.28f), -90f, 360f * circP, false, arcTL, arcSz, style = Stroke(9f * s, cap = StrokeCap.Round))
        drawArc(BoardColors.Teal, -90f, 360f * circP, false, arcTL, arcSz, style = Stroke(3.5f * s, cap = StrokeCap.Round))

        // the angle at A sweeps open
        val ang = atan2(c.y - a.y, c.x - a.x) * 180f / PI.toFloat()
        val q = ease(seg(p, 0.55f, 0.72f))
        val ar = 15f * s
        drawArc(Color(0xFFFBBF24).copy(alpha = 0.25f), ang, -ang * q, true, Offset(a.x - ar, a.y - ar), Size(ar * 2, ar * 2), style = Fill)
        drawArc(Color(0xFFFBBF24), ang, -ang * q, false, Offset(a.x - ar, a.y - ar), Size(ar * 2, ar * 2), style = Stroke(2.5f * s, cap = StrokeCap.Round))

        // corner dots pop one after another, then their letters
        val label = TextStyle(color = Color.White, fontSize = 12f.times(s).toSp(), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
        val dots = listOf(a to ("A" to Offset(-13f, 1f)), b to ("B" to Offset(6f, 1f)), c to ("C" to Offset(-4f, -17f)))
        dots.forEachIndexed { i, (pt, lab) ->
            val k = back(seg(p, 0.4f + i * 0.04f, 0.48f + i * 0.04f)).coerceAtLeast(0f)
            drawCircle(Color.White, 5f * s * k, pt)
            drawCircle(BoardColors.Indigo, 5f * s * k, pt, style = Stroke(2f * s))
            val la = seg(p, 0.5f + i * 0.04f, 0.6f + i * 0.04f)
            if (la > 0f) {
                runCatching {
                    drawText(
                        measurer, lab.first, Offset(pt.x + lab.second.x * s, pt.y + lab.second.y * s),
                        label.copy(color = Color.White.copy(alpha = la)), softWrap = false, maxLines = 1,
                    )
                }
            }
        }
    }

    // twinkling stars around the plate
    val stars = listOf(o(26f, 46f) to 0f, o(176f, 38f) to 0.25f, o(178f, 154f) to 0.5f, o(24f, 158f) to 0.75f)
    val sa = ease(seg(p, 0.55f, 0.7f))
    stars.forEachIndexed { i, (pt, ph) ->
        val k = 0.5f + 0.5f * sin((tw + ph) * 2f * PI.toFloat())
        star(pt, (4f + 6f * k) * s * (if (i % 2 == 0) 1f else 0.8f), Color(0xFFF1D3A5).copy(alpha = sa * (0.35f + 0.65f * k)))
    }
}
