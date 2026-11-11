package com.aismartmath.board.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import com.aismartmath.board.math.parseRich
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Gold = Color(0xFFFBBF24)
private val Pink = Color(0xFFF472B6)
private val Green = Color(0xFF34D399)
private val Cream = Color(0xFFF1D3A5)
private val Copper = Color(0xFFD08A4E)
private val GridC = Color(0x44F1D3A5)

private class HelpTopic(val id: String, val title: String, val lines: List<String>, val pic: @Composable () -> Unit)

private fun DrawScope.axes() {
    val w = size.width
    val h = size.height
    drawLine(GridC, Offset(0f, h * 0.7f), Offset(w, h * 0.7f), 2f)
    drawLine(GridC, Offset(w / 2, 0f), Offset(w / 2, h), 2f)
}

private fun DrawScope.plotFn(col: Color, x0: Float, x1: Float, width: Float = 5f, f: (Float) -> Float) {
    val w = size.width
    val h = size.height
    val p = Path()
    var first = true
    var x = x0
    while (x <= x1) {
        val px = w / 2 + x * w / 7f
        val py = h * 0.7f - f(x) * h / 5f
        if (py > -h && py < 2 * h) {
            if (first) {
                p.moveTo(px, py)
                first = false
            } else p.lineTo(px, py)
        }
        x += 0.05f
    }
    drawPath(p, col, style = Stroke(width, cap = StrokeCap.Round))
}

@Composable
private fun Pic(draw: DrawScope.(TextMeasurer) -> Unit) {
    val m = rememberTextMeasurer()
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF160E0A))
            .border(BorderStroke(1.dp, Color(0xFF6B4630)), RoundedCornerShape(12.dp)),
    ) { draw(m) }
}

private fun DrawScope.label(m: TextMeasurer, s: String, x: Float, y: Float, c: Color = Cream, size: Float = 13f) {
    drawText(m, s, Offset(x, y), TextStyle(color = c, fontSize = size.sp, fontWeight = FontWeight.Bold))
}

private val TOPICS: List<HelpTopic> = listOf(
    HelpTopic(
        "graph", "ƒ  Graphs",
        listOf(
            "Open the ƒ Graph tool (toolbar → Graph) and type an equation, then press Plot.",
            "Try:  x^2 - 3x + 1   ·   sin(x)   ·   1/x   ·   sqrt(x)   ·   abs(x).",
            "a, b and c are sliders: a*x^2 + b*x + c lets you change the numbers and watch the curve move. ▶ a animates it.",
            "Tap the curve to see a point on it and its slope. The info bar lists intercepts and turning points.",
            "Use ^ for powers, * for times, / for divide, and | | or abs( ) for absolute value.",
        ),
    ) {
        Pic { m ->
            axes()
            plotFn(Gold, -2.6f, 2.6f) { it * it * 0.5f - 0.5f }
            val tx = 1.2f
            val ty = tx * tx * 0.5f - 0.5f
            val cx = size.width / 2 + tx * size.width / 7f
            val cy = size.height * 0.7f - ty * size.height / 5f
            drawLine(Color(0xFFF87171), Offset(cx - 60f, cy + 60f * 1.2f * 0.7f), Offset(cx + 60f, cy - 60f * 1.2f * 0.7f), 4f, StrokeCap.Round)
            drawCircle(Color.White, 8f, Offset(cx, cy))
            label(m, "y = x²/2 − ½", 12f, 8f)
        }
    },
    HelpTopic(
        "derivative", "dy/dx  Derivatives (slope curves)",
        listOf(
            "Type  dy/dx of x^3 - 3x  and press Plot. You get two curves: your function, and in pink its slope curve dy/dx.",
            "Type  d2y/dx2 of x^3 - 3x  for the second derivative (green).",
            "Other ways that work:  d/dx(sin(x))  ·  derivative of x^2  ·  diff(x^2).",
            "Already plotted a graph? Press the  dy/dx  or  d²y/dx²  button in the graph panel to add its slope curve.",
            "Where the pink curve touches zero, the original curve has a top or a bottom (a turning point).",
            "If you type  dy/dx = 2x  the app just draws 2x, because that is the slope you wrote.",
        ),
    ) {
        Pic { m ->
            axes()
            plotFn(Gold, -2.7f, 2.7f) { it * it * it / 4f - 0.75f * it }
            plotFn(Pink, -2.7f, 2.7f) { 0.75f * it * it - 0.75f }
            label(m, "yellow: y", 12f, 8f, Gold)
            label(m, "pink: dy/dx", 12f, 28f, Pink)
        }
    },
    HelpTopic(
        "shapes", "○ ▭ △  Circles, squares and more by equation",
        listOf(
            "Any equation with x and y draws its curve. Circle:  x^2 + y^2 = 9  (radius 3).   Ellipse:  x^2/9 + y^2/4 = 1.",
            "Square:  max(|x|, |y|) = 3.   Rectangle:  max(|x|/4, |y|/2) = 1.   Rhombus:  |x|/4 + |y|/2 = 1.",
            "Vertical line:  x = 3.   Hyperbola:  x^2/4 - y^2/4 = 1.   Parabola sideways:  y^2 = 4x.",
            "Triangle: use the Triangle chip under “Shapes by equation”, then change a (half-width) and b (half-height) with the sliders.",
            "Tip: put a, b, c in your equation (x^2 + y^2 = a) and the sliders resize the shape.",
        ),
    ) {
        Pic { m ->
            val h = size.height
            drawCircle(Gold, h * 0.3f, Offset(size.width * 0.2f, h / 2), style = Stroke(5f))
            drawRect(Pink, Offset(size.width * 0.42f, h * 0.2f), Size(h * 0.6f, h * 0.6f), style = Stroke(5f))
            val p = Path().apply {
                moveTo(size.width * 0.8f, h * 0.2f)
                lineTo(size.width * 0.93f, h * 0.8f)
                lineTo(size.width * 0.67f, h * 0.8f)
                close()
            }
            drawPath(p, Green, style = Stroke(5f))
            label(m, "x²+y²=9", size.width * 0.1f, h - 24f, Gold, 11f)
            label(m, "max(|x|,|y|)=3", size.width * 0.38f, h - 24f, Pink, 11f)
        }
    },
    HelpTopic(
        "curves", "✿ ♥  Curves: parametric and polar",
        listOf(
            "Parametric: write both parts with t, in brackets:  (3cos(t), 3sin(t))  draws a circle.",
            "Polar: write r with theta:  r = 4 sin(3 theta)  draws a rose.   r = 0.3 theta  draws a spiral.",
            "The Curves chips have a Heart and a Lissajous figure — press one, then play with the a, b, c sliders.",
            "Tap a curve to select it, like any other figure.",
        ),
    ) {
        Pic { _ ->
            val w = size.width
            val h = size.height
            val rose = Path()
            var first = true
            var t = 0f
            while (t <= PI.toFloat() * 2) {
                val r = sin(3 * t) * h * 0.4f
                val x = w * 0.28f + r * cos(t)
                val y = h / 2 + r * sin(t)
                if (first) {
                    rose.moveTo(x, y)
                    first = false
                } else rose.lineTo(x, y)
                t += 0.03f
            }
            drawPath(rose, Pink, style = Stroke(4f, cap = StrokeCap.Round))
            val heart = Path()
            first = true
            t = 0f
            while (t <= PI.toFloat() * 2) {
                val x = w * 0.72f + 16f * sin(t).let { it * it * it } * (h * 0.0165f)
                val y = h * 0.48f - (13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)) * (h * 0.0165f)
                if (first) {
                    heart.moveTo(x, y)
                    first = false
                } else heart.lineTo(x, y)
                t += 0.03f
            }
            drawPath(heart, Gold, style = Stroke(4f, cap = StrokeCap.Round))
        }
    },
    HelpTopic(
        "content", "⬚  Content: select, move, resize, colour",
        listOf(
            "Pick the ⬚ Content tool and drag a box round writing, text, pictures or figures.",
            "Tap ✓ to keep what the box caught, or ✗ to forget it.",
            "Drag inside the box to move everything together; drag the small square at the corner to resize.",
            "The bar below gives: copy, cut, delete, paste, crop (one picture), K (neat maths), thicker or thinner pen lines, colours, a size slider and arrows to nudge.",
            "In the Select tool you can also choose ⬚ Content to box several figures — they are then checked for parallel lines.",
            "The 👁 in the Content bar shows everything about the selected figures: every angle where two lines cross (up to four, with degrees), every line length, side and corner angle. Without it the board stays clean.",
            "Every new line gets its own letters (A B, then C D …, after Z it goes on with A′ B′ …). Select a line or shape and tap 🔤 to change them: type P Q R, or A′B′C′, or press A′B′ to add a prime to all.",
        ),
    ) {
        Pic { m ->
            val w = size.width
            val h = size.height
            val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
            val wave = Path().apply {
                moveTo(w * 0.12f, h * 0.5f)
                cubicTo(w * 0.2f, h * 0.2f, w * 0.28f, h * 0.8f, w * 0.36f, h * 0.45f)
            }
            drawPath(wave, Gold, style = Stroke(5f, cap = StrokeCap.Round))
            drawRect(Pink, Offset(w * 0.45f, h * 0.35f), Size(w * 0.18f, h * 0.3f), style = Stroke(4f))
            drawRect(Cream, Offset(w * 0.08f, h * 0.12f), Size(w * 0.6f, h * 0.76f), style = Stroke(3f, pathEffect = dash))
            drawRect(Cream, Offset(w * 0.68f - 9f, h * 0.88f - 9f), Size(18f, 18f))
            drawCircle(Color(0xFF6FA66B), 22f, Offset(w * 0.82f, h * 0.35f))
            label(m, "✓", w * 0.82f - 8f, h * 0.35f - 12f, Color.White, 18f)
            drawCircle(Color(0xFFC0583F), 22f, Offset(w * 0.92f, h * 0.35f))
            label(m, "✗", w * 0.92f - 8f, h * 0.35f - 12f, Color.White, 18f)
        }
    },
    HelpTopic(
        "answer", "🧮  Answer: draw a figure from its sizes",
        listOf(
            "Open 🧮 Answer, pick a figure: triangle, rectangle, quadrilateral, circle, cube, cylinder, cone, graph …",
            "Type the sizes (for a triangle: the three sides) and press Draw ✓.",
            "The figure appears at exactly those sizes with its labels. Write on it with the pen or highlighter, add text, or select it to change colours.",
            "Pick Answer again for the next figure. Undo (↶) removes the last one.",
        ),
    ) {
        Pic { m ->
            val w = size.width
            val h = size.height
            val p = Path().apply {
                moveTo(w * 0.3f, h * 0.8f)
                lineTo(w * 0.7f, h * 0.8f)
                lineTo(w * 0.3f, h * 0.2f)
                close()
            }
            drawPath(p, Gold, style = Stroke(5f))
            label(m, "3", w * 0.22f, h * 0.45f, Cream)
            label(m, "4", w * 0.48f, h * 0.82f, Cream)
            label(m, "5", w * 0.52f, h * 0.42f, Cream)
            label(m, "Triangle 3 · 4 · 5  →  Draw ✓", w * 0.1f, 6f, Copper, 11f)
        }
    },
    HelpTopic(
        "parallel", "∥  Parallel lines and a transversal",
        listOf(
            "Draw two parallel lines and a line that cuts through both. The eight angles ∠1–∠8 appear with their sizes.",
            "Tap an angle to see what it equals. Use the chips for corresponding, alternate and co-interior angles.",
            "A side that only touches the lines at a corner is not a transversal — it has to cut through both lines.",
            "If there is more than one transversal, only one is shown. Double-tap another one to switch to it.",
            "Box several figures with Select → ⬚ Content and tap ✓: the parallel lines inside them are found for you.",
        ),
    ) {
        Pic { m ->
            val w = size.width
            val h = size.height
            drawLine(Gold, Offset(w * 0.1f, h * 0.35f), Offset(w * 0.9f, h * 0.35f), 5f)
            drawLine(Gold, Offset(w * 0.1f, h * 0.7f), Offset(w * 0.9f, h * 0.7f), 5f)
            drawLine(Pink, Offset(w * 0.3f, h * 0.9f), Offset(w * 0.6f, h * 0.1f), 5f)
            label(m, "1", w * 0.5f, h * 0.17f, Cream, 11f)
            label(m, "2", w * 0.57f, h * 0.17f, Cream, 11f)
            label(m, "5", w * 0.4f, h * 0.52f, Cream, 11f)
            label(m, "6", w * 0.47f, h * 0.52f, Cream, 11f)
        }
    },
    HelpTopic(
        "katex", "Σ  KaTeX: neat maths in any text",
        listOf(
            "Tap Aa to add text (or paste some). Whatever you type is turned into real maths on the board, and the box shows a live preview before you press Add. Below is a catalogue of every kind of thing you can type — each drawn as it will look.",
            "Fractions: just write a/b, x/y or (a+b)/(c+d). It becomes a stacked fraction — no backslash needed. 2x/3y makes (2x) over (3y).",
            "Roots: write sqrt(x), sqr(x+1) or sqrt x. Cube root: cbrt(x). The old way \\sqrt[3]{x} works too.",
            "Powers and small numbers: x^2, x^(n+1), x_1. Towers work: a^x^2 or a^(x^2) both give a to the power (x to the power 2). a*x becomes ax. pi, theta, alpha … (all Greek names) become π, θ, α.",
            "Brackets grow by themselves: (a/b) gets tall brackets round the fraction. In (a+b)/(c+d) the brackets are dropped because the bar already groups them. Write upon instead of / if you like: a upon b. Absolute value: |x|.",
            "Everything else from LaTeX still works: \\sum, \\int, \\lim, \\binom, matrices, \\text{ }, \\mathbb{R}, \\hat, \\vec … see the cheat sheet below.",
            "The same neat maths is used for the answers, details and graph info bars, so fractions, roots and powers are always drawn properly.",
            "K still works: select the text and press K to re-fit its box. Words like “and/or” or “km/h” are left alone when they are clearly words.",
        ),
    ) {
        Pic { m ->
            val w = size.width
            label(m, "x^2 + a/b = sqrt(x)", w * 0.06f, 14f, Cream, 15f)
            label(m, "↓", w * 0.06f, 52f, Copper, 18f)
            label(m, "x² + a/b = √x", w * 0.06f, 86f, Gold, 17f)
            label(m, "(a+b)/(c+d)", w * 0.58f, 14f, Cream, 13f)
            label(m, "pi  theta  alpha", w * 0.58f, 52f, Cream, 13f)
            label(m, "→  π  θ  α", w * 0.58f, 86f, Gold, 15f)
        }
    },
    HelpTopic(
        "drag", "✥  Dragging, moving and zooming",
        listOf(
            "Move tool: drag empty board to slide the whole board around. Pinch with two fingers to zoom; the board is endless.",
            "Drag any figure, text or picture to move it. Drag a corner dot of a shape to change its size and shape; the dimensions update live.",
            "Select tool: tap a figure to select it. The selected one shows its dimensions; others stay quiet so overlapping shapes are not cluttered.",
            "Double-tap a figure for its menu: Details (shown in neat maths), colours, delete, and more.",
            "Double-tap empty board (Move or Select tool) → Paste. Triple-tap opens your pictures.",
            "Hold a handle to see its hover text. Undo (↶) and redo (↷) undo every move.",
        ),
    ) {
        Pic { m ->
            val w = size.width
            val h = size.height
            val tri = Path().apply {
                moveTo(w * 0.2f, h * 0.78f)
                lineTo(w * 0.5f, h * 0.78f)
                lineTo(w * 0.2f, h * 0.25f)
                close()
            }
            drawPath(tri, Gold, style = Stroke(5f))
            drawCircle(Color.White, 9f, Offset(w * 0.5f, h * 0.78f))
            drawLine(Copper, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.66f, h * 0.78f), 4f, StrokeCap.Round)
            drawLine(Copper, Offset(w * 0.66f, h * 0.78f), Offset(w * 0.62f, h * 0.7f), 4f, StrokeCap.Round)
            drawLine(Copper, Offset(w * 0.66f, h * 0.78f), Offset(w * 0.62f, h * 0.86f), 4f, StrokeCap.Round)
            drawCircle(Pink, 14f, Offset(w * 0.82f, h * 0.4f), style = Stroke(4f))
            drawCircle(Pink, 14f, Offset(w * 0.9f, h * 0.48f), style = Stroke(4f))
            label(m, "pinch = zoom", w * 0.66f, h * 0.7f, Cream, 11f)
            label(m, "drag a corner", w * 0.5f, 8f, Copper, 11f)
        }
    },
    HelpTopic(
        "sketch", "✏  Sketch: draw roughly, get neat shapes",
        listOf(
            "Open the Sketch tool. The ✏ Shape chip is for drawing: draw a rough line, triangle, rectangle, quadrilateral or circle with your finger or stylus.",
            "Lift your finger and the rough drawing becomes a neat figure with its labels and adjustable dimensions.",
            "Each shape gets its own vertex names, not always ABC, so several shapes on one board do not repeat the same letters.",
            "The ⬚ Content chip in the same tool is for selecting: drag a box round what you want, tap ✓, then move, resize, colour or copy it.",
            "Angles are shaded automatically and each kind of angle has its own colour.",
            "Want a 3D solid from your own 2D drawing? Sketch the shape first, then use it as the base in the 3D tool.",
        ),
    ) {
        Pic { m ->
            val w = size.width
            val h = size.height
            val rough = Path().apply {
                moveTo(w * 0.1f, h * 0.8f)
                cubicTo(w * 0.2f, h * 0.76f, w * 0.3f, h * 0.84f, w * 0.4f, h * 0.8f)
                cubicTo(w * 0.34f, h * 0.6f, w * 0.26f, h * 0.35f, w * 0.24f, h * 0.2f)
                cubicTo(w * 0.2f, h * 0.4f, w * 0.12f, h * 0.65f, w * 0.1f, h * 0.8f)
            }
            drawPath(rough, Cream, style = Stroke(4f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))))
            label(m, "→", w * 0.46f, h * 0.42f, Copper, 26f)
            val tri = Path().apply {
                moveTo(w * 0.6f, h * 0.8f)
                lineTo(w * 0.9f, h * 0.8f)
                lineTo(w * 0.6f, h * 0.2f)
                close()
            }
            drawPath(tri, Gold, style = Stroke(5f))
            label(m, "A", w * 0.55f, h * 0.84f - 8f, Cream, 12f)
            label(m, "B", w * 0.9f, h * 0.84f - 8f, Cream, 12f)
            label(m, "C", w * 0.55f, h * 0.12f, Cream, 12f)
        }
    },
    HelpTopic(
        "pen", "🖊️  Pen, highlighter and stylus",
        listOf(
            "Tap 🖊️ Write: pick Pen, Highlighter or Eraser, then colours, width and (for the pen) solid, dashed or dotted lines.",
            "Lines holds the straight line and points; Shapes, 3D and Graph hold everything else.",
            "A stylus works too: resting your palm while writing with a stylus is ignored, the stylus' eraser end erases, and fast strokes stay smooth.",
            "If a home-made soft-tip pen is not noticed at all, the phone itself is not reporting it as a touch — try it in another app first.",
        ),
    ) {
        Pic { m ->
            val w = size.width
            val h = size.height
            drawLine(Gold, Offset(w * 0.1f, h * 0.3f), Offset(w * 0.9f, h * 0.3f), 8f, StrokeCap.Round)
            drawLine(Pink, Offset(w * 0.1f, h * 0.55f), Offset(w * 0.9f, h * 0.55f), 8f, StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f)))
            drawLine(Green, Offset(w * 0.1f, h * 0.8f), Offset(w * 0.9f, h * 0.8f), 8f, StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 20f)))
            label(m, "solid", w * 0.75f, h * 0.12f, Cream, 11f)
        }
    },
)

private class Meta(
    val icon: String,
    val blurb: String,
    val accent: Color,
    val tryIt: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val cheat: List<Cat> = emptyList(),
)

private val META: Map<String, Meta> = mapOf(
    "graph" to Meta(
        "ƒ", "Plot any equation, see slopes, intercepts and turning points.", Gold,
        tryIt = listOf("x^2 - 3x + 1", "sin(x)/x", "1/x", "sqrt(x)", "cbrt(x)", "4brt(x)", "(1/3)brt(x)", "abs(x)", "a*x^2 + b*x + c", "x^2/9 + y^2/4 = 1"),
        tips = listOf("Every function works: sin, cos, tan, cot, sec, csc (cosec), log, ln, exp, abs, sqrt, cbrt. Any root: 4brt(x) is the fourth root, (1/3)brt(x) a root with index 1/3 — drawn as real maths in the equation bar.", "The info bar under the graph shows fractions, roots and powers as proper maths."),
    ),
    "derivative" to Meta(
        "d/dx", "Slope curves: first and second derivative, drawn next to your graph.", Pink,
        tryIt = listOf("dy/dx of x^3 - 3x", "d2y/dx2 of x^3 - 3x", "d/dx(sin(x))", "diff(x^2)"),
        tips = listOf("Where the pink curve crosses zero, your curve has a top or a bottom.", "Use the dy/dx button in the graph panel to add the slope curve to a graph you already have."),
    ),
    "pen" to Meta(
        "🖊️", "Pen, highlighter and eraser with solid, dashed or dotted lines.", Green,
        tips = listOf("Palm resting is ignored when you write with a stylus.", "Highlighter keeps your writing visible underneath."),
    ),
    "drag" to Meta(
        "✥", "Move the board, zoom, drag figures and corners.", Copper,
        tips = listOf("Overlapping shapes: only the selected one shows its dimensions.", "Double-tap a figure for Details written in neat maths."),
    ),
    "content" to Meta(
        "⬚", "Box things together, then move, resize, copy or colour them.", Pink,
        tips = listOf("✓ keeps the box, ✗ forgets it.", "Press K on selected text to re-fit it."),
    ),
    "sketch" to Meta(
        "✏", "Rough drawing in, neat figure out.", Gold,
        tips = listOf("Draw in one stroke where you can: triangles and rectangles are recognised best that way.", "A circle should be closed: end near where you started."),
    ),
    "katex" to Meta(
        "Σ", "Type maths the easy way — a/b becomes a real fraction.", Gold,
        tryIt = listOf("a/b", "x/y + z/w", "(a+b)/(c+d)", "x^2/y^2", "sqrt(x^2 + 1)", "(a/b)", "2x/3y = 4", "pi r^2", "sin(x)/x", "dy/dx"),
        tips = listOf("A space ends a fraction part: 1/2 x is a half times x, 1/2x is one over 2x.", "Use \\text{ } for words inside a LaTeX formula."),
        cheat = KATEX_CATS,
    ),
    "shapes" to Meta("○", "Circles, squares, ellipses and more, from equations.", Green, tryIt = listOf("x^2 + y^2 = 9", "x^2/9 + y^2/4 = 1", "max(|x|, |y|) = 3", "|x|/4 + |y|/2 = 1", "x^2/4 - y^2/4 = 1")),
    "curves" to Meta("✿", "Parametric and polar curves, hearts and roses.", Pink, tryIt = listOf("(3cos(t), 3sin(t))", "r = 4 sin(3 theta)", "r = 0.3 theta")),
    "answer" to Meta("🧮", "Draw a figure from its sizes: triangle, circle, cube …", Gold),
    "parallel" to Meta("∥", "Parallel lines, a transversal and the eight angles.", Green),
)

private val ORDER = listOf("graph", "derivative", "pen", "drag", "content", "sketch", "katex", "shapes", "curves", "answer", "parallel")

private fun metaOf(id: String) = META[id] ?: Meta("?", "", Gold)

private fun normTopic(id: String): String? = when (id) {
    "text" -> "katex"
    else -> if (TOPICS.any { it.id == id }) id else null
}

/** Fades and slides its content in, a little later for each [index] - gives lists a gentle cascade. */
@Composable
private fun FadeIn(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(minOf(index, 10) * 45L)
        shown = true
    }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(320), label = "fadeIn")
    Box(
        Modifier.graphicsLayer {
            alpha = a
            translationY = (1f - a) * 36f
            scaleX = 0.96f + 0.04f * a
            scaleY = 0.96f + 0.04f * a
        },
    ) { content() }
}

@Composable
private fun TopicCard(t: HelpTopic, m: Meta, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2A1B12))
            .border(BorderStroke(1.dp, m.accent.copy(alpha = 0.6f)), RoundedCornerShape(16.dp))
            .clickable { onClick() },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(78.dp)
                .background(Brush.verticalGradient(listOf(m.accent.copy(alpha = 0.45f), Color(0xFF2A1B12)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(m.icon, color = Color.White, fontSize = 32.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(t.title.substringAfter("  ").ifBlank { t.title }, color = Cream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            HelpText(m.blurb, 11.sp, Color(0xFFB59A83))
            Text("${t.lines.size} steps  ›", color = m.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CodeRow(code: String, accent: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF160E0A))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(code, color = Copper, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        Text("→", color = Color(0xFF9C8470), fontSize = 14.sp)
        Box(Modifier.weight(1f)) { MathView(parseRich(code), 16.sp, accent) }
    }
}

/** One example: the maths drawn big on top, what to type underneath. */
@Composable
private fun ExampleCard(code: String, accent: Color) {
    val node = remember(code) { parseRich(code) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF160E0A))
            .border(BorderStroke(1.dp, Color(0xFF4A3022)), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { MathView(node, 22.sp, accent) }
        Text(code, color = Copper, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

/** Help text: maths in it (x^2, a/b, sqrt(x) …) is drawn as real maths. Lines that show LaTeX code stay plain. */
@Composable
private fun HelpText(s: String, size: androidx.compose.ui.unit.TextUnit, color: Color, modifier: Modifier = Modifier) {
    if (s.contains('\\')) Text(s, color = color, fontSize = size, lineHeight = size * 1.4f, modifier = modifier)
    else MathTextView(s, size, color, modifier)
}

@Composable
private fun SectionTitle(s: String, c: Color) {
    Text(s, color = c, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun TopicDetail(t: HelpTopic, m: Meta, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹ All topics",
                color = Copper,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onBack() }.padding(horizontal = 8.dp, vertical = 6.dp),
            )
            Text(t.title, color = m.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 6.dp))
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { t.pic() }
            if (m.blurb.isNotBlank()) item { HelpText(m.blurb, 14.sp, Cream) }
            item { SectionTitle("Steps", m.accent) }
            itemsIndexed(t.lines) { i, line ->
                FadeIn(i) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A1B12))
                        .border(BorderStroke(1.dp, Color(0xFF4A3022)), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(m.accent.copy(alpha = 0.85f)).padding(horizontal = 9.dp, vertical = 3.dp),
                    ) { Text("${i + 1}", color = Color(0xFF1A110C), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    HelpText(line, 13.sp, Color(0xFFE9D8C4), Modifier.weight(1f))
                }
                }
            }
            if (m.tryIt.isNotEmpty()) {
                item { SectionTitle("Try typing  →  how it looks", m.accent) }
                items(m.tryIt.size) { i -> CodeRow(m.tryIt[i], Gold) }
            }
            if (m.cheat.isNotEmpty()) {
                item {
                    Text(
                        "${m.cheat.sumOf { it.items.size }} examples in ${m.cheat.size} groups — each one is drawn live by the same engine the board uses. Type what you see in the small code line.",
                        color = Cream, fontSize = 13.sp, lineHeight = 18.sp,
                    )
                }
            }
            m.cheat.forEach { cat ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SectionTitle(cat.title, m.accent)
                        HelpText(cat.note, 12.sp, Color(0xFFB59A83))
                    }
                }
                items(cat.items.size) { i -> ExampleCard(cat.items[i], Gold) }
            }
            if (m.tips.isNotEmpty()) {
                item { SectionTitle("Good to know", m.accent) }
                items(m.tips.size) { i ->
                    HelpText("•  " + m.tips[i], 12.sp, Color(0xFFD8C3AB))
                }
            }
            item { Box(Modifier.height(24.dp)) }
        }
    }
}

/** The “?” page: a grid of features; tap one for a picture, the steps and examples. */
@Composable
fun HelpDialog(startTopic: String, onClose: () -> Unit) {
    var open by remember { mutableStateOf(normTopic(startTopic)) }
    val topics = remember { ORDER.mapNotNull { id -> TOPICS.firstOrNull { it.id == id } } }
    Dialog(
        onDismissRequest = { if (open != null) open = null else onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF1A110C))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "How to use",
                    color = Cream,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconBtn("✕", compact = true) { onClose() }
            }
            AnimatedContent(targetState = open, modifier = Modifier.weight(1f), label = "helpPage") { id ->
                val topic = topics.firstOrNull { it.id == id }
                if (topic == null) {
                    LazyVerticalGrid(
                        GridCells.Adaptive(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        gridItemsIndexed(topics) { idx, t -> FadeIn(idx) { TopicCard(t, metaOf(t.id)) { open = t.id } } }
                    }
                } else {
                    TopicDetail(topic, metaOf(topic.id)) { open = null }
                }
            }
        }
    }
}
