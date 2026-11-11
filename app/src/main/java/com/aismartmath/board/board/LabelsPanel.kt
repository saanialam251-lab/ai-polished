package com.aismartmath.board.board

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.math.angDeg
import com.aismartmath.board.math.dist
import com.aismartmath.board.math.pts
import com.aismartmath.board.model.Fig
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.components.slab
import com.aismartmath.board.ui.theme.BoardColors

/**
 * The "eye" box: a small scrolling panel that lists every side and angle of the selected figure.
 * Each row has an eye to show or hide that label on the board, and a number box to change it.
 * The ◎ button next to an angle shows only that angle (same as tapping its corner twice).
 */
@Composable
fun LabelsPanel(state: BoardState, f: Fig, bare: Boolean = false, onClose: () -> Unit = { state.labelsOpen = false }) {
    if (f.t == "seg") {
        SegDetails(state, f, bare, onClose)
        return
    }
    val accent = BoardColors.Indigo
    val shape = RoundedCornerShape(12.dp)
    val P = pts(f)
    val n = P.size
    val names = Nm.of(f.nm)
    fun nameAt(i: Int): String = names[i]
    val sideKeys: List<String?> = when (f.t) {
        "tri" -> listOf("ab", "bc", "ca")
        "quad" -> listOf("ab", "bc", "cd", "da")
        else -> List(n) { null }
    }
    val angleEditable = f.t == "tri" || f.t == "quad" || f.t == "pent" || f.t == "hex"
    val iso = f.only in 0 until n

    Column(
        Modifier.fillMaxWidth().then(if (bare) Modifier else Modifier.glass(shape, accent, 8.dp).padding(6.dp)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PanelTitle(if (bare) figTitle(f) else "\\text{DETAILS}", Modifier.weight(1f))
            Chip("Show", false, accent, onLong = { state.showToast("Show every side, angle and extra again") }, small = true) { state.showAllLabels(f, true) }
            Chip("Hide", false, accent, onLong = { state.showToast("Hide the numbers and switch off extras — the lines stay") }, small = true) { state.showAllLabels(f, false) }
            Chip("ABC", "v" !in f.hide, accent, onLong = { state.showToast("Corner letters") }, small = true) { state.toggleLabel(f, "v") }
            if (!bare) Chip("✕", false, accent, small = true) { onClose() }
        }
        if (iso) {
            Chip("◎ Only ∠${nameAt(f.only)} — show everything again", true, accent, small = true) { state.isolateAngle(f, f.only) }
        }
        Row(
            Modifier.fillMaxWidth().then(if (bare) Modifier else Modifier.heightIn(max = 104.dp).verticalScroll(rememberScrollState())),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SectionTitle("Sides")
                for (i in 0 until n) {
                    val key = sideKeys.getOrNull(i)
                    LabelRow(
                        on = "s$i" !in f.hide && !iso,
                        name = nameAt(i) + nameAt((i + 1) % n),
                        value = dist(P[i], P[(i + 1) % n]),
                        readOnly = key == null,
                        accent = accent,
                        hot = false,
                        onEye = { state.toggleLabel(f, "s$i") },
                        onValue = { v -> if (key != null) state.editField(f, key, v) },
                        onFocus = null,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SectionTitle("Angles")
                for (i in 0 until n) {
                    val an = angDeg(P[i], P[(i + n - 1) % n], P[(i + 1) % n])
                    LabelRow(
                        on = "a$i" !in f.hide && (!iso || f.only == i),
                        name = "∠" + nameAt(i),
                        value = an,
                        readOnly = !angleEditable,
                        accent = accent,
                        hot = f.only == i,
                        onEye = { state.toggleLabel(f, "a$i") },
                        onValue = { v -> state.setAngleAt(f, i, v) },
                        onFocus = { state.isolateAngle(f, i) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = BoardColors.Muted, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
}

@Composable
private fun LabelRow(
    on: Boolean,
    name: String,
    value: Double,
    readOnly: Boolean,
    accent: Color,
    hot: Boolean,
    onEye: () -> Unit,
    onValue: (Double) -> Unit,
    onFocus: (() -> Unit)?,
    nameW: Dp = 26.dp,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        EyeBtn(on, accent, onEye)
        Text(
            name,
            color = if (hot) accent else Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.width(nameW),
        )
        NumField(value, readOnly, accent, Modifier.weight(1f)) { onValue(it) }
        if (onFocus != null) FocusBtn(hot, accent, onFocus)
    }
}

/** The eye: lit when the label is showing, dim when it is hidden. */
@Composable
private fun EyeBtn(on: Boolean, accent: Color, onClick: () -> Unit) {
    val fill by animateColorAsState(if (on) accent.copy(alpha = 0.55f) else Color(0xFF2A1B13), tween(140), label = "eyeFill")
    val edge by animateColorAsState(if (on) Color(0xFFF1D3A5) else Color(0xFF5C3D28), tween(140), label = "eyeEdge")
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier.size(22.dp).slab(shape, fill, edge, 3.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("👁", fontSize = 11.sp, color = Color.White.copy(alpha = if (on) 1f else 0.3f)) }
}

/** ◎ - show only this angle. */
@Composable
private fun FocusBtn(on: Boolean, accent: Color, onClick: () -> Unit) {
    val fill by animateColorAsState(if (on) accent.copy(alpha = 0.7f) else Color(0xFF3B271B), tween(140), label = "focFill")
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier.size(22.dp).slab(shape, fill, Color(0xFF6B4630), 3.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("◎", fontSize = 11.sp, color = Color.White) }
}

/** Change the letters of the selected figure: A B C, P Q R, A′ B′ C′ … Corners are taken in order (one letter, or a letter with primes / digits, each). */
@Composable
fun NamesBar(state: BoardState, f: Fig) {
    val accent = BoardColors.Indigo
    val shape = RoundedCornerShape(12.dp)
    var tf by remember(f) { mutableStateOf(TextFieldValue(f.nm ?: "")) }
    Row(
        Modifier.fillMaxWidth().glass(shape, accent, 8.dp).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(if (f.t == "seg") "Ends" else "Letters", color = BoardColors.Muted, fontSize = 10.sp)
        BasicTextField(
            value = tf,
            onValueChange = { nv ->
                tf = nv
                state.setNames(f, nv.text)
            },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic),
            cursorBrush = SolidColor(accent),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF160E0A))
                .border(BorderStroke(1.dp, Color(0xFF6B4630)), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
        )
        Chip("A′B′", false, accent, onLong = { state.showToast("Add a prime to every letter: A′ B′ C′") }, small = true) {
            state.primeNames(f)
            tf = TextFieldValue(f.nm ?: "")
        }
        Chip("ABC", false, accent, onLong = { state.showToast("Back to plain letters") }, small = true) {
            state.setNames(f, if (f.t == "seg") "AB" else "ABCDEF".take(pts(f).size))
            tf = TextFieldValue(f.nm ?: "")
        }
        Chip("✕", false, accent, small = true) { state.namesOpen = false }
    }
}

/**
 * Details of a line: its letters, length and - where another line is joined to one of its ends - the angle between
 * them (∠BAD). The number boxes change them; the eye of an angle shows its degrees on the board.
 */
@Composable
fun SegDetails(state: BoardState, f: Fig, bare: Boolean = false, onClose: () -> Unit = { state.labelsOpen = false }) {
    val accent = BoardColors.Indigo
    val shape = RoundedCornerShape(12.dp)
    val P = pts(f)
    val nz = Nm.of(f.nm)
    val joints = state.jointsFor(f)
    Column(
        Modifier.fillMaxWidth().then(if (bare) Modifier else Modifier.glass(shape, accent, 8.dp).padding(6.dp)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PanelTitle(if (bare) figTitle(f) else "\\text{DETAILS}", Modifier.weight(1f))
            Chip("ABC", "v" !in f.hide, accent, onLong = { state.showToast("Letters at the ends of the line") }, small = true) { state.toggleLabel(f, "v") }
            if (!bare) Chip("✕", false, accent, small = true) { onClose() }
        }
        Column(
            Modifier.fillMaxWidth().then(if (bare) Modifier else Modifier.heightIn(max = 112.dp).verticalScroll(rememberScrollState())),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            SectionTitle("Line")
            LabelRow(
                on = "len" !in f.hide,
                name = nz[0] + nz[1],
                value = dist(P[0], P[1]),
                readOnly = false,
                accent = accent,
                hot = false,
                onEye = { state.toggleLength(f) },
                onValue = { v -> state.editField(f, "len", v) },
                onFocus = null,
                nameW = 44.dp,
            )
            if (joints.isNotEmpty()) {
                SectionTitle("Angles where lines meet")
                for (j in joints) {
                    LabelRow(
                        on = ("jd" + j.e) in f.ov,
                        name = j.name,
                        value = j.deg,
                        readOnly = false,
                        accent = accent,
                        hot = false,
                        onEye = { state.toggleOverlay(f, "jd" + j.e) },
                        onValue = { v -> state.setJointAngle(f, j, v) },
                        onFocus = null,
                        nameW = 52.dp,
                    )
                }
            }
        }
    }
}
