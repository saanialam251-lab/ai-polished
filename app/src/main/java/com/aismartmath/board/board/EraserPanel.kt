package com.aismartmath.board.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

private val EraserColor = Color(0xFFF1D3A5)

/** Eraser options: rub out just the part under the finger, or the whole stroke; and how big the eraser is. */
@Composable
fun EraserPanel(state: BoardState) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .glass(shape, BoardColors.Indigo)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("⌫", color = Color.White, fontSize = 16.sp, modifier = Modifier.width(28.dp))
            Chip("Part", state.erasePart, EraserColor, onLong = { state.showToast("Rub out only the part you touch") }) { state.erasePart = true }
            Chip("Stroke", !state.erasePart, EraserColor, onLong = { state.showToast("Rub out the whole stroke") }) { state.erasePart = false }
            Chip("✕", false, EraserColor, onLong = { state.showToast("Close this box") }, small = true) { state.toolMenu = null }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("◯", color = Color.White, fontSize = 15.sp, modifier = Modifier.width(28.dp))
            Slider(
                value = state.eraserDp,
                onValueChange = { state.eraserDp = it.roundToInt().toFloat() },
                valueRange = 6f..40f,
                modifier = Modifier.weight(1f).height(22.dp),
                colors = SliderDefaults.colors(
                    thumbColor = EraserColor,
                    activeTrackColor = EraserColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                ),
            )
            Text("${state.eraserDp.roundToInt()}", color = BoardColors.Muted, fontSize = 12.sp, modifier = Modifier.width(30.dp))
        }
    }
}
