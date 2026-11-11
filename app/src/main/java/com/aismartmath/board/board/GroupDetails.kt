package com.aismartmath.board.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.math.parseRich
import com.aismartmath.board.math.pts
import com.aismartmath.board.model.Fig
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.components.MathView
import com.aismartmath.board.ui.components.PopIn
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.theme.BoardColors

/** A panel heading drawn as maths (KaTeX style): pass typed maths such as `\text{DETAILS}`. */
@Composable
internal fun PanelTitle(tex: String, modifier: Modifier = Modifier) {
    val node = remember(tex) { parseRich(tex) }
    Row(modifier) { MathView(node, 13.sp, Color.White) }
}

/** `TRIANGLE ABC`, `LINE AB`, `CIRCLE` as typed maths. Letters are joined with * so each one stays an italic variable. */
internal fun figTitle(f: Fig): String {
    val nz = Nm.of(f.nm)
    val name = "\\text{" + figName(f).uppercase().replace("{", "").replace("}", "") + "}"
    val n = when (f.t) {
        "seg" -> 2
        "circ", "pt", "fx", "sol", "ink", "txt", "img", "unit", "trig" -> 0
        else -> pts(f).size
    }
    if (n == 0) return name
    return name + " " + (0 until n).joinToString("*") { nz[it] }
}

/**
 * The Details box of a Content selection - the same rows as the Details box of a single figure (sides, angles,
 * letters, an eye and a number box for each), one block per selected figure.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupDetailsPanel(state: BoardState) {
    val accent = BoardColors.Indigo
    val figs = state.grpDetailFigs()
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(12.dp), accent, 8.dp).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PanelTitle("\\text{DETAILS}", Modifier.weight(1f))
            Chip("Show", false, accent, onLong = { state.showToast("Show every side, angle and letter again") }, small = true) {
                figs.forEach { state.showAllLabels(it, true) }
            }
            Chip("Hide", false, accent, onLong = { state.showToast("Hide the numbers — the lines stay") }, small = true) {
                figs.forEach { state.showAllLabels(it, false) }
            }
            Chip("✕", false, accent, onLong = { state.showToast("Close this box — the details stay on the board") }, small = true) {
                state.grpDetailsOpen = false
            }
        }
        Column(
            Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (figs.isEmpty()) {
                Text("The selected things have no sides or angles to list.", color = BoardColors.Muted, fontSize = 11.sp)
            }
            figs.forEachIndexed { i, f ->
                PopIn(i, Modifier.fillMaxWidth()) {
                    if (f.t == "circ") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PanelTitle("\\text{CIRCLE}")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Chip("r", "rad" in f.ov, accent, onLong = { state.showToast("Radius line and length") }, small = true) {
                                    state.toggleOverlay(f, "rad")
                                }
                                Chip("d", "dia" in f.ov, accent, onLong = { state.showToast("Diameter") }, small = true) {
                                    state.toggleOverlay(f, "dia")
                                }
                            }
                        }
                    } else {
                        LabelsPanel(state, f, bare = true)
                    }
                }
            }
        }
    }
}
