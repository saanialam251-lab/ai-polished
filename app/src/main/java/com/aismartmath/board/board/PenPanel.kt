package com.aismartmath.board.board

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.theme.BoardColors
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** Web: penPanel - colour swatches and a width slider, plus a live preview of the pen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PenPanel(state: BoardState) {
    val accent by animateColorAsState(state.penCol, tween(250), label = "penAccent")
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .glass(shape, BoardColors.Indigo)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Swatches.forEach { c -> PenSwatch(c, state.penCol == c) { state.penCol = c } }
            }
            Chip("✕", false, BoardColors.Indigo, onLong = { state.showToast("Close this box") }, small = true) { state.toolMenu = null }
        }
        if (state.tool == "pen") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Line", color = BoardColors.Muted, fontSize = 9.sp, modifier = Modifier.width(20.dp))
                Chip("━ Solid", state.penStyle == 0, BoardColors.Indigo, small = true) { state.penStyle = 0 }
                Chip("╌ Dashed", state.penStyle == 1, BoardColors.Indigo, small = true) { state.penStyle = 1 }
                Chip("··· Dotted", state.penStyle == 2, BoardColors.Indigo, small = true) { state.penStyle = 2 }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (state.tool == "hl") "🖍" else "✎", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(20.dp))
            Slider(
                value = state.penW,
                onValueChange = { state.penW = it.roundToInt().toFloat() },
                valueRange = 1f..24f,
                modifier = Modifier.weight(1f).height(22.dp),
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = Color(0xFF5C3D28),
                ),
            )
            Text("${state.penW.roundToInt()}", color = BoardColors.Muted, fontSize = 10.sp, modifier = Modifier.width(22.dp))
        }
        val hl = state.tool == "hl"
        Canvas(Modifier.fillMaxWidth().height(22.dp)) {
            val path = Path()
            val n = 40
            for (i in 0..n) {
                val x = size.width * (0.04f + 0.92f * i / n)
                val y = size.height / 2 + sin(i / n.toFloat() * 2f * PI.toFloat()) * size.height * 0.28f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val w = (if (hl) state.penW * 3 else state.penW).dp.toPx()
            drawPath(
                path,
                accent.copy(alpha = if (hl) 0.35f else 1f),
                style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
private fun PenSwatch(c: Color, on: Boolean, onClick: () -> Unit) {
    val ring by animateFloatAsState(if (on) 1f else 0f, tween(200), label = "penRing")
    val lift by animateFloatAsState(if (on) 1.18f else 1f, spring(dampingRatio = 0.5f), label = "penLift")
    Box(
        Modifier
            .graphicsLayer { scaleX = lift; scaleY = lift }
            .size(20.dp)
            .clip(CircleShape)
            .background(c)
            .border(BorderStroke(2.dp, Color.White.copy(alpha = ring)), CircleShape)
            .clickable(onClick = onClick),
    )
}
