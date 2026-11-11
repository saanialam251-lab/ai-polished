package com.aismartmath.board.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.theme.BoardColors
import kotlin.math.roundToInt

private val TrigColor = Color(0xFFD08A4E)

/** Web: the panel of the Trig triangle and Unit circle tools - quick angles and sliders. */
@Composable
fun TrigPanel(state: BoardState) {
    val shape = RoundedCornerShape(14.dp)
    val unit = state.tool == "unit"
    val trig = state.trigFig()
    Column(
        Modifier
            .fillMaxWidth()
            .glass(shape, TrigColor)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val quick = if (unit) listOf(30, 45, 60, 90, 180, 270) else listOf(30, 45, 60, 90)
        val now = if (unit) state.unitDeg() else (trig?.ga?.roundToInt() ?: 30)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            quick.forEach { d ->
                Chip("$d°", now == d, TrigColor) {
                    if (unit) state.setUnitDeg(d) else trig?.let { state.editField(it, "th", d.toDouble()) }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("θ", color = Color.White, fontSize = 15.sp, modifier = Modifier.width(30.dp))
            Slider(
                value = now.toFloat(),
                onValueChange = {
                    val d = it.roundToInt()
                    if (unit) state.setUnitDeg(d) else trig?.let { f -> state.editField(f, "th", d.toDouble()) }
                },
                valueRange = 0f..(if (unit) 360f else 90f),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = TrigColor,
                    activeTrackColor = TrigColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                ),
            )
            Text("$now°", color = BoardColors.Muted, fontSize = 12.sp, modifier = Modifier.width(44.dp))
        }
        if (!unit && trig != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("hyp", color = Color.White, fontSize = 13.sp, modifier = Modifier.width(30.dp))
                Slider(
                    value = trig.gb.toFloat().coerceIn(1f, 8f),
                    onValueChange = { state.editField(trig, "h", (it * 2).roundToInt() / 2.0) },
                    valueRange = 1f..8f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = TrigColor,
                        activeTrackColor = TrigColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    ),
                )
                Text(fmt(trig.gb, 1), color = BoardColors.Muted, fontSize = 12.sp, modifier = Modifier.width(44.dp))
            }
        }
    }
}
