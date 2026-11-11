package com.aismartmath.board.board

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.math.parseExpr
import com.aismartmath.board.math.parseRich
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.components.MathTextView
import com.aismartmath.board.ui.components.MathView
import com.aismartmath.board.ui.components.PopIn
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.theme.BoardColors
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The live calculator. What you type is kept as typed maths (a/b, sqrt(x), x^2, pi …) and drawn by the same
 * KaTeX-style engine as the rest of the board; the answer under it updates after every key.
 */
class CalcState {
    var expr by mutableStateOf("")
    var deg by mutableStateOf(true)
    var mini by mutableStateOf(false)
    var big by mutableStateOf(false)

    // window place and size in dp; w = 0 until the first time it is shown (then it is fitted to the board)
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)
    var w by mutableFloatStateOf(0f)
    var h by mutableFloatStateOf(0f)
    private var saved: FloatArray? = null

    /** Toggle the "oversize" state: nearly the whole board, and back to where it was. */
    fun toggleBig(bw: Float, bh: Float) {
        val s = saved
        if (!big) {
            saved = floatArrayOf(x, y, w, h)
            x = 6f
            y = 6f
            w = bw - 12f
            h = bh - 12f
            big = true
        } else {
            if (s != null) {
                x = s[0]
                y = s[1]
                w = s[2]
                h = s[3]
            }
            big = false
        }
    }

    fun press(key: String) {
        when (key) {
            "C" -> expr = ""
            "⌫" -> expr = backspace(expr)
            "=" -> {
                val v = calcValue(expr, deg)
                if (v != null) expr = calcText(v)
            }
            else -> expr = insert(expr, key)
        }
    }
}

private val TOKENS = listOf("\\times ", "sqrt(", "cbrt(", "sin(", "cos(", "tan(", "log(", "ln(", "rt(", "pi")

private fun backspace(e: String): String {
    if (e.isEmpty()) return e
    for (t in TOKENS) if (e.endsWith(t)) return e.dropLast(t.length)
    return e.dropLast(1)
}

/** Put a key's text at the end. A letter-word right after a letter would glue into one word (pie, pisin), so a * goes between. */
private fun insert(e: String, tok: String): String {
    val glue = e.isNotEmpty() && e.last().isLetter() && tok.first().isLetter()
    var t = tok
    if (t == "rt(" && (e.isEmpty() || !e.last().isDigit())) t = "3rt("
    return e + (if (glue) "*" else "") + t
}

private fun closeParens(s: String): String {
    var open = 0
    for (c in s) {
        if (c == '(') open++ else if (c == ')' && open > 0) open--
    }
    return s + ")".repeat(open)
}

/** sin( cos( tan( work in degrees: sin(30) becomes sin((30)*pi/180). */
private fun degWrap(s: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < s.length) {
        var hit: String? = null
        for (fn in arrayOf("sin", "cos", "tan")) {
            if (s.startsWith("$fn(", i) && (i == 0 || !s[i - 1].isLetter())) {
                hit = fn
                break
            }
        }
        if (hit == null) {
            sb.append(s[i])
            i++
            continue
        }
        val open = i + hit.length
        var d = 0
        var j = open
        while (j < s.length) {
            if (s[j] == '(') d++ else if (s[j] == ')') {
                d--
                if (d == 0) break
            }
            j++
        }
        if (j >= s.length) {
            sb.append(s.substring(i))
            break
        }
        sb.append(hit).append("((").append(degWrap(s.substring(open + 1, j))).append(")*pi/180)")
        i = j + 1
    }
    return sb.toString()
}

/** The value of what is typed, or null when it is empty, unfinished or not a number. */
fun calcValue(src: String, deg: Boolean): Double? {
    var s = src.replace("\\times ", "*").trim()
    if (s.isEmpty()) return null
    s = closeParens(s).trimEnd('.')
    if (deg) s = degWrap(s)
    val fn = parseExpr(s) ?: return null
    return try {
        val v = fn(0.0, 0.0, 0.0, 0.0, 0.0)
        if (v.isFinite()) v else null
    } catch (e: Exception) {
        null
    }
}

/** A number as typed maths: 10 digits, no trailing zeros, big and tiny ones as m × 10^n. */
fun calcText(v: Double): String {
    if (v == 0.0 || abs(v) < 1e-13) return "0"
    val bd = BigDecimal(v).round(MathContext(10)).stripTrailingZeros()
    val a = abs(v)
    if (a < 1e12 && a >= 1e-6) return bd.toPlainString()
    val digits = bd.unscaledValue().abs().toString()
    val exp = bd.precision() - bd.scale() - 1
    val mant = if (digits.length > 1) digits.substring(0, 1) + "." + digits.substring(1) else digits
    return (if (v < 0) "-" else "") + mant + "\\times 10^" + exp
}

private enum class KeyKind { NUM, OP, FN, CLEAR, EQ }

private class CalcKey(val label: String, val ins: String, val kind: KeyKind, val tex: Boolean = false)

private val ROWS: List<List<CalcKey>> = listOf(
    listOf(CalcKey("C", "C", KeyKind.CLEAR), CalcKey("⌫", "⌫", KeyKind.CLEAR), CalcKey("(", "(", KeyKind.OP), CalcKey(")", ")", KeyKind.OP), CalcKey("÷", "/", KeyKind.OP)),
    listOf(CalcKey("sqrt(x)", "sqrt(", KeyKind.FN, true), CalcKey("cbrt(x)", "cbrt(", KeyKind.FN, true), CalcKey("x^2", "^2", KeyKind.FN, true), CalcKey("x^y", "^(", KeyKind.FN, true), CalcKey("×", "\\times ", KeyKind.OP)),
    listOf(CalcKey("7", "7", KeyKind.NUM), CalcKey("8", "8", KeyKind.NUM), CalcKey("9", "9", KeyKind.NUM), CalcKey("sin", "sin(", KeyKind.FN), CalcKey("−", "-", KeyKind.OP)),
    listOf(CalcKey("4", "4", KeyKind.NUM), CalcKey("5", "5", KeyKind.NUM), CalcKey("6", "6", KeyKind.NUM), CalcKey("cos", "cos(", KeyKind.FN), CalcKey("+", "+", KeyKind.OP)),
    listOf(CalcKey("1", "1", KeyKind.NUM), CalcKey("2", "2", KeyKind.NUM), CalcKey("3", "3", KeyKind.NUM), CalcKey("tan", "tan(", KeyKind.FN), CalcKey("=", "=", KeyKind.EQ)),
    listOf(CalcKey("0", "0", KeyKind.NUM), CalcKey(".", ".", KeyKind.NUM), CalcKey("π", "pi", KeyKind.FN), CalcKey("e", "e", KeyKind.FN, true), CalcKey("log", "log(", KeyKind.FN)),
    listOf(CalcKey("ln", "ln(", KeyKind.FN), CalcKey("nbrt(x)", "rt(", KeyKind.FN, true), CalcKey("1/x", "1/", KeyKind.FN, true), CalcKey("10^x", "10^", KeyKind.FN, true), CalcKey("e^x", "e^", KeyKind.FN, true)),
)

private val HEADER = 38.dp

/** The floating calculator: drag it by its title, shrink it to the title, resize it from the corner, oversize it, close it. */
@Composable
fun CalculatorWindow(calc: CalcState, visible: Boolean, onClose: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bw = maxWidth.value
        val bh = maxHeight.value
        if (calc.w <= 0f) {
            calc.w = minOf(318f, bw - 16f).coerceAtLeast(240f)
            calc.h = minOf(440f, bh - 16f).coerceAtLeast(300f)
            calc.x = (bw - calc.w - 46f).coerceAtLeast(6f)
            calc.y = 8f
        }
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = 380f), initialScale = 0.4f, transformOrigin = TransformOrigin(1f, 0f)) + fadeIn(tween(160)),
            exit = scaleOut(tween(150), targetScale = 0.6f, transformOrigin = TransformOrigin(1f, 0f)) + fadeOut(tween(130)),
        ) {
            CalcBody(calc, bw, bh, onClose)
        }
    }
}

@Composable
private fun CalcBody(calc: CalcState, bw: Float, bh: Float, onClose: () -> Unit) {
    val density = LocalDensity.current.density
    val accent = BoardColors.Indigo
    val shape = RoundedCornerShape(16.dp)
    val bwNow by rememberUpdatedState(bw)
    val bhNow by rememberUpdatedState(bh)
    val boxH by animateFloatAsState(if (calc.mini) HEADER.value else calc.h, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium), label = "calcH")

    // keep the window inside the board when the board changes size
    val w = calc.w.coerceIn(220f, bwNow)
    val cx = calc.x.coerceIn(0f, (bwNow - w).coerceAtLeast(0f))
    val cy = calc.y.coerceIn(0f, (bhNow - HEADER.value).coerceAtLeast(0f))

    Box(
        Modifier
            .offset { IntOffset((cx * density).roundToInt(), (cy * density).roundToInt()) }
            .size(w.dp, boxH.dp)
            .glass(shape, accent, 12.dp)
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(Modifier.fillMaxSize()) {
            // ---- title bar: drag here ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(HEADER)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            calc.x = (calc.x + drag.x / density).coerceIn(0f, (bwNow - calc.w).coerceAtLeast(0f))
                            calc.y = (calc.y + drag.y / density).coerceIn(0f, (bhNow - HEADER.value).coerceAtLeast(0f))
                        }
                    }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("⠿", color = BoardColors.Muted, fontSize = 14.sp)
                PanelTitle("\\text{CALCULATOR}", Modifier.weight(1f))
                Chip(if (calc.deg) "DEG" else "RAD", true, accent, small = true) { calc.deg = !calc.deg }
                Chip(if (calc.mini) "▭" else "▁", false, accent, small = true) { calc.mini = !calc.mini }
                Chip(if (calc.big) "⤡" else "⤢", calc.big, accent, small = true) {
                    calc.mini = false
                    calc.toggleBig(bwNow, bhNow)
                }
                Chip("✕", false, accent, small = true) { onClose() }
            }
            if (!calc.mini) {
                Display(calc, Modifier.fillMaxWidth().weight(0.27f).padding(horizontal = 8.dp))
                Column(
                    Modifier.fillMaxWidth().weight(0.73f).padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    var n = 0
                    for (row in ROWS) {
                        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (k in row) {
                                val idx = n++
                                PopIn(idx / 2, Modifier.weight(1f).fillMaxSize()) {
                                    Key(k, accent) { calc.press(k.ins) }
                                }
                            }
                        }
                    }
                }
            }
        }
        // ---- corner: drag to resize ----
        if (!calc.mini && !calc.big) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            calc.w = (calc.w + drag.x / density).coerceIn(230f, (bwNow - calc.x).coerceAtLeast(230f))
                            calc.h = (calc.h + drag.y / density).coerceIn(300f, (bhNow - calc.y).coerceAtLeast(300f))
                        }
                    },
                contentAlignment = Alignment.BottomEnd,
            ) {
                Text("◢", color = accent, fontSize = 13.sp, modifier = Modifier.padding(end = 4.dp, bottom = 1.dp))
            }
        }
    }
}

/** What you typed (as maths) and, under it, the answer. */
@Composable
private fun Display(calc: CalcState, modifier: Modifier) {
    val accent = BoardColors.Indigo
    val shown = closeParens(calc.expr)
    val value = calcValue(calc.expr, calc.deg)
    val answer = if (calc.expr.isBlank()) "0" else if (value != null) calcText(value) else ""
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF160E0A))
            .border(BorderStroke(1.dp, Color(0xFF6B4630)), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState(), reverseScrolling = true)) {
            if (shown.isEmpty()) Text("type a sum …", color = BoardColors.Muted, fontSize = 13.sp)
            else MathTextView(shown, 22.sp)
        }
        AnimatedContent(
            targetState = answer,
            transitionSpec = {
                (slideInVertically(tween(160)) { it / 2 } + fadeIn(tween(160))) togetherWith
                    (slideOutVertically(tween(120)) { -it / 2 } + fadeOut(tween(100)))
            },
            label = "calcAns",
            modifier = Modifier.align(Alignment.End),
        ) { a ->
            if (a.isEmpty()) {
                Text("…", color = BoardColors.Muted, fontSize = 16.sp)
            } else {
                val node = remember(a) { parseRich("= $a") }
                MathView(node, 22.sp, accent)
            }
        }
    }
}

@Composable
private fun Key(k: CalcKey, accent: Color, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val clickNow by rememberUpdatedState(onClick)
    val sc by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "keySc")
    val base = when (k.kind) {
        KeyKind.NUM -> Color(0xFF3B271B)
        KeyKind.OP -> accent.copy(alpha = 0.32f)
        KeyKind.FN -> Color(0xFF4A3022)
        KeyKind.CLEAR -> Color(0xFF5A2A22)
        KeyKind.EQ -> accent
    }
    val fill by animateColorAsState(if (pressed) accent.copy(alpha = 0.85f) else base, tween(110), label = "keyFill")
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .shadow(if (pressed) 1.dp else 3.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, if (pressed) Color(0xFFF1D3A5) else Color(0xFF6B4630)), shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        clickNow()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (k.tex) {
            val node = remember(k.label) { parseRich(k.label) }
            MathView(node, 14.sp, Color.White)
        } else {
            Text(k.label, color = Color.White, fontSize = 16.sp, fontWeight = if (k.kind == KeyKind.NUM) FontWeight.Medium else FontWeight.Normal)
        }
    }
}
