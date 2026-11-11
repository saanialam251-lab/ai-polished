package com.aismartmath.board.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.components.glass

private val ParBlue = Color(0xFFD08A4E)

/** Web: pxPanel - choose which family of angles to highlight on the parallel lines. Short labels; long-press for the name. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParPanel(state: BoardState) {
    val shape = RoundedCornerShape(16.dp)
    FlowRow(
        Modifier.fillMaxWidth().glass(shape, ParBlue).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        listOf(
            Triple("all", "All", "All eight angles"),
            Triple("cor", "Corr", "Corresponding angles"),
            Triple("alt", "Alt in", "Alternate interior angles"),
            Triple("ext", "Alt out", "Alternate exterior angles"),
            Triple("co", "Co-in", "Co-interior angles (add to 180°)"),
            Triple("vert", "Vert", "Vertically opposite angles"),
        ).forEach { (key, short, long) ->
            Chip(short, state.pxMode == key && state.pxHl == 0, ParBlue, onLong = { state.showToast(long) }, small = true) {
                state.pxMode = key
                state.pxHl = 0
            }
        }
        if (state.parAll().size > 1) {
            Chip("Set ${state.parIdx + 1} of ${state.parAll().size} ▸", false, ParBlue, onLong = { state.showToast("Next pair of parallel lines") }, small = true) { state.nextParSet() }
        }
        if (state.parAll().size > 1) {
            Chip("All sets", state.pxAllSets, ParBlue, onLong = { state.showToast("Show the angles of every transversal together") }, small = true) { state.pxAllSets = !state.pxAllSets }
        }
        Chip("∠½", state.pxBis, ParBlue, onLong = { state.showToast("Angle bisectors") }, small = true) { state.pxBis = !state.pxBis }
        Chip("✕", false, ParBlue, onLong = { state.showToast("Hide the angles") }, small = true) { state.pxOn = false }
    }
}
