package com.aismartmath.board.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.math.MathNode
import com.aismartmath.board.math.layoutMath
import com.aismartmath.board.math.layoutMathText
import com.aismartmath.board.math.parseRich

/** Draws a [MathNode]: stacked fractions, raised powers, real roots, tall brackets, sums, integrals, matrices. */
@Composable
fun MathView(node: MathNode, size: TextUnit = 14.sp, color: Color = Color.White, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val px = with(density) { size.toPx() }
    val argb = color.toArgb()
    val box = remember(node, px, argb) { layoutMath(node, px, argb) }
    val wDp = with(density) { box.w.toDp() } + 1.dp
    val hDp = with(density) { box.h.toDp() } + 1.dp
    Canvas(modifier.size(wDp, hDp)) {
        drawIntoCanvas { box.draw(it.nativeCanvas, 0f, box.asc) }
    }
}

/** A whole typed text (words and maths, wrapped to the width available), e.g. a question. */
@Composable
fun MathTextView(src: String, size: TextUnit = 15.sp, color: Color = Color.White, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val px = with(density) { size.toPx() }
        val maxPx = with(density) { maxWidth.toPx() }
        val argb = color.toArgb()
        val mt = remember(src, px, maxPx, argb) { layoutMathText(parseRich(src), px, maxPx, argb) }
        val hDp = with(density) { mt.h.toDp() } + 2.dp
        Canvas(Modifier.fillMaxWidth().height(hDp)) {
            drawIntoCanvas { mt.draw(it.nativeCanvas, 0f, 0f) }
        }
    }
}
