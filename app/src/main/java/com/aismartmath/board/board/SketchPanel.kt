package com.aismartmath.board.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.theme.BoardColors

/** Content tool: drag a box round writing, drawings, text or pictures. Shape: draw roughly and get a neat figure. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SketchPanel(state: BoardState) {
    val acc = BoardColors.Indigo
    FlowRow(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(12.dp), acc, 8.dp).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Chip("⬚ Content", state.skDrag, acc, onLong = { state.showToast("Drag a box round things, then tap ✓") }, small = true) {
            state.skDrag = true
            state.showToast("Drag a box round what you want to select")
        }
        Chip("✏ Shape", !state.skDrag, acc, onLong = { state.showToast("Draw roughly — it becomes a neat figure") }, small = true) {
            state.skDrag = false
            state.clearGroup()
        }
    }
}

/** Select tool: pick one figure (default) or draw a box round several (content selection). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectPanel(state: BoardState) {
    val acc = BoardColors.Indigo
    FlowRow(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(12.dp), acc, 8.dp).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Chip("👆 Pick", !state.selContent, acc, onLong = { state.showToast("Tap a figure to edit it") }, small = true) {
            state.selContent = false
            state.cancelPending()
        }
        Chip("⬚ Content", state.selContent, acc, onLong = { state.showToast("Draw a box round figures — then tap ✓") }, small = true) {
            state.selContent = true
            state.showToast("Drag a box round the figures you want")
        }
    }
}

/** The tick and the cross after a box was drawn. */
@Composable
fun ConfirmBar(state: BoardState) {
    val acc = BoardColors.Indigo
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(12.dp), acc, 8.dp).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${state.pending.size} inside the box — use them?", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Chip("✓", true, Color(0xFF6FA66B), onLong = { state.showToast("Yes, select these") }, small = true) { state.confirmPending() }
        Chip("✗", false, Color(0xFFC0583F), onLong = { state.showToast("No, forget the box") }, small = true) { state.cancelPending() }
    }
}

@Composable
private fun Dot(c: Color, onClick: () -> Unit) {
    Box0(c, onClick)
}

@Composable
private fun Box0(c: Color, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(c)
            .border(BorderStroke(1.dp, Color(0xFFF1D3A5).copy(alpha = 0.6f)), CircleShape)
            .clickable { onClick() },
    )
}

/** The bar under a selection: copy / cut / delete / paste, colour, size, nudge, crop, and K for neat maths. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentBar(state: BoardState) {
    val acc = BoardColors.Indigo
    val n = state.group.size
    val hasInk = state.group.any { it.t == "ink" }
    val hasText = state.group.any { it.t == "txt" }
    var pct by remember(state.group) { mutableFloatStateOf(100f) }
    var active by remember(state.group) { mutableStateOf(false) }
    var aiOpen by remember { mutableStateOf(false) }
    val step = 24f * state.camera.density.toFloat()
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(12.dp), acc, 8.dp).padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("⬚ $n selected", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(end = 4.dp))
            Chip("📋", false, acc, onLong = { state.showToast("Copy") }, small = true) { state.copyGroup(false) }
            Chip("✂️", false, acc, onLong = { state.showToast("Cut") }, small = true) { state.copyGroup(true) }
            Chip("🗑️", false, acc, onLong = { state.showToast("Delete") }, small = true) { state.deleteGroup() }
            Chip("📥", false, acc, onLong = { state.showToast("Paste text, a picture, or what you copied") }, small = true) { state.pasteHere(null) }
            Chip("👁", state.grpEye, acc, onLong = { state.showToast("Details: opens a box to choose what to show — what you switch on stays on the board") }, small = true) { state.toggleGroupEye() }
            Chip("▣", false, acc, onLong = { state.showToast("Crop a picture") }, small = true) { state.cropGroup() }
            if (hasText) Chip("K", false, acc, onLong = { state.showToast("Make maths in the text neat: x^2 becomes x²") }, small = true) { state.applyMath() }
            if (hasInk) {
                Chip("━−", false, acc, onLong = { state.showToast("Thinner pen lines") }, small = true) { state.inkThicker(0.8) }
                Chip("━＋", false, acc, onLong = { state.showToast("Thicker pen lines") }, small = true) { state.inkThicker(1.25) }
            }
            Chip("✨ Gemini", aiOpen, acc, onLong = { state.showToast("Ask the AI about what you selected — a picture, a figure, text or a question") }, small = true) { aiOpen = !aiOpen }
            Chip("✕", false, acc, onLong = { state.showToast("Un-select") }, small = true) { state.clearGroup() }
        }
        if (aiOpen) AiDoubtRow(state)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Swatches.forEach { c -> Dot(c) { state.setGroupColor(c) } }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Size", color = BoardColors.Muted, fontSize = 9.sp, modifier = Modifier.width(24.dp))
            Slider(
                value = pct,
                onValueChange = {
                    if (!active) {
                        state.beginGroupScale()
                        active = true
                    }
                    pct = it
                    state.scaleGroupTo(it / 100.0)
                },
                onValueChangeFinished = {
                    state.endGroupDrag("Resized selection")
                    active = false
                    pct = 100f
                },
                valueRange = 30f..250f,
                modifier = Modifier.weight(1f).height(22.dp),
                colors = SliderDefaults.colors(
                    thumbColor = acc,
                    activeTrackColor = acc,
                    inactiveTrackColor = Color(0xFF5C3D28),
                ),
            )
            Chip("◀", false, acc, small = true) { state.nudgeGroup(-step, 0f) }
            Chip("▲", false, acc, small = true) { state.nudgeGroup(0f, -step) }
            Chip("▼", false, acc, small = true) { state.nudgeGroup(0f, step) }
            Chip("▶", false, acc, small = true) { state.nudgeGroup(step, 0f) }
        }
    }
}
