package com.aismartmath.board.board

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

private val LayerColor = Color(0xFFD08A4E)

/** Layers: show / hide, lock, reorder and fade groups of figures. New drawings go on the active layer. */
@Composable
fun LayersPanel(state: BoardState) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .glass(shape, LayerColor)
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🗂 Layers", color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Chip("＋", false, LayerColor, onLong = { state.showToast("Add a layer") }) { state.addLayer() }
            Chip("✕", false, LayerColor) { state.layersOpen = false }
        }
        state.layers.forEachIndexed { i, l ->
            val active = state.activeLayer == l.id
            val bd by animateColorAsState(if (active) LayerColor else Color.White.copy(alpha = 0.14f), tween(200), label = "layerBd")
            val rowShape = RoundedCornerShape(10.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(rowShape)
                    .background(if (active) LayerColor.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f))
                    .border(BorderStroke(1.5.dp, bd), rowShape)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LayerBtn(if (l.visible) "👁" else "🚫") { state.setLayerVisible(l, !l.visible) }
                    LayerBtn(if (l.locked) "🔒" else "🔓") { state.setLayerLocked(l, !l.locked) }
                    Text(
                        l.name + (if (active) "  ✓" else ""),
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (l.locked || !l.visible) state.showToast("Show and unlock the layer to draw on it")
                                else state.activeLayer = l.id
                            }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                    LayerBtn("↑") { state.moveLayer(i, -1) }
                    LayerBtn("↓") { state.moveLayer(i, 1) }
                    if (state.layers.size > 1) LayerBtn("✕") { state.deleteLayer(l.id) }
                }
                if (active) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("◐", color = BoardColors.Muted, fontSize = 13.sp, modifier = Modifier.width(24.dp))
                    Slider(
                        value = l.opacity,
                        onValueChange = { l.opacity = (it * 20).roundToInt() / 20f },
                        valueRange = 0.1f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = LayerColor,
                            activeTrackColor = LayerColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                        ),
                    )
                    Text("${(l.opacity * 100).roundToInt()}%", color = BoardColors.Muted, fontSize = 11.sp, modifier = Modifier.width(38.dp))
                }
            }
        }
    }
}

@Composable
private fun LayerBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 14.sp) }
}
