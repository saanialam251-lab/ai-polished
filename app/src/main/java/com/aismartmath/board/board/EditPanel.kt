package com.aismartmath.board.board

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.model.Fig
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.components.RichLine
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.theme.BoardColors
import kotlin.math.roundToInt

private val POLYGONS = setOf("tri", "quad", "rect", "rtri", "pent", "hex")

/**
 * The panel of the selected figure. Three icon tabs (measure, style, info) and a row of icon buttons,
 * so everything fits without scrolling. Long-press an icon to read what it does.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPanel(state: BoardState, f: Fig) {
    val accent = BoardColors.Indigo
    val border by animateColorAsState(accent, tween(300), label = "panelBorder")
    val shape = RoundedCornerShape(12.dp)
    var tab by remember(f) { mutableIntStateOf(if (state.detailsOn) 2 else 0) }
    LaunchedEffect(state.detailsOn) { if (state.detailsOn) tab = 2 }
    val tip: (String) -> Unit = { state.showToast(it) }

    Column(
        Modifier.fillMaxWidth().animateContentSize().glass(shape, border, 8.dp).padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                figName(f),
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TabIcon("📏", tab == 0, border) { tab = 0; state.detailsOn = false }
            TabIcon("🎨", tab == 1, border) { tab = 1; state.detailsOn = false }
            TabIcon("ℹ️", tab == 2, border) { tab = 2; state.detailsOn = true }
            TabIcon("✕", false, border) { state.closeEdit() }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (f.t in POLYGONS || f.t == "seg") {
                ActionBtn("👁", "Details — show or hide sides, angles and extras", border, tip, on = state.labelsOpen) {
                    state.labelsOpen = !state.labelsOpen
                }
            }
            if (f.t == "seg" || f.t in POLYGONS) {
                ActionBtn("🔤", "Change the letters (A B C … or A′ B′ C′)", border, tip, on = state.namesOpen) {
                    state.namesOpen = !state.namesOpen
                }
            }
            if (f.t == "circ") {
                ActionBtn("👁", "Show or hide the radius", border, tip, on = "rad" in f.ov) { state.toggleOverlay(f, "rad") }
            }
            if (f.t == "txt") {
                ActionBtn("K", "Make maths neat: x^2 becomes x²", border, tip) { state.applyMath() }
            }
            if (f.t == "img") {
                ActionBtn("▣", "Crop the picture — drag its corners", border, tip, on = state.cropping === f) {
                    if (state.cropping === f) state.endCrop() else state.startCrop(f)
                }
                ActionBtn("↺", "Bring back the cropped-off parts", border, tip) { state.resetCrop(f) }
            }
            ActionBtn("📋", "Copy", border, tip) { state.copySel(f, false) }
            ActionBtn("✂️", "Cut", border, tip) { state.copySel(f, true) }
            ActionBtn("🗑️", "Delete", border, tip) {
                state.delete(f)
                state.showToast("Deleted 🗑️ — undo (↶) brings it back")
            }
            if (f.t != "fx") {
                ActionBtn("✥", "Move — then drag the figure", border, tip) {
                    state.mv = f
                    state.rs = null
                    state.closeEdit(clearSel = false)
                    state.sel = f
                    state.showToast("Move ✥ — drag the figure, tap empty space when done")
                }
            }
            if (f.t != "pt" && f.t != "fx") {
                ActionBtn("⤢", "Resize — then drag", border, tip) {
                    state.rs = f
                    state.mv = null
                    state.closeEdit(clearSel = false)
                    state.sel = f
                    state.showToast("Resize ⤢ — drag outward or inward, tap empty space when done")
                }
            }
            if (f.t in setOf("tri", "quad", "pent", "hex")) {
                ActionBtn("✦", "Make regular", border, tip) { state.makeRegular(f) }
            }
            if (f.t in POLYGONS) {
                ActionBtn("🧱", "3D prism on this base", border, tip) { state.makeSolid(f, "ext") }
                ActionBtn("🔺", "3D pyramid on this base", border, tip) { state.makeSolid(f, "apx") }
            }
            if (state.layers.size > 1) {
                val li = state.layers.indexOfFirst { it.id == f.layer }
                ActionBtn("🗂", "Move to the next layer (now layer ${li + 1})", border, tip) {
                    val next = state.layers[(li + 1).mod(state.layers.size)]
                    state.edit(f, "Moved to " + next.name) { f.layer = next.id }
                    state.showToast("Moved to " + next.name)
                }
            }
        }

        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "editTab",
        ) { t ->
            when (t) {
                0 -> MeasureTab(state, f, border)
                1 -> StyleTab(state, f, border, tip)
                else -> InfoTab(state, f)
            }
        }
    }
}

@Composable
private fun MeasureTab(state: BoardState, f: Fig, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (f.t == "txt") {
            var text by remember(f) { mutableStateOf(f.txt) }
            LaunchedEffect(f.txt) { if (f.txt != text && f.txt.length != text.length) text = f.txt }
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    state.setText(f, it)
                },
                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                cursorBrush = SolidColor(accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 70.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF160E0A))
                    .border(BorderStroke(1.dp, Color(0xFF6B4630)), RoundedCornerShape(7.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip("A−", false, accent, small = true) { state.setFontSize(f, f.fsz * 0.85) }
                Chip("A＋", false, accent, small = true) { state.setFontSize(f, f.fsz * 1.18) }
                Text("letter size · drag the corner to change the width", color = BoardColors.Muted, fontSize = 9.sp)
            }
        }
        fieldsOf(f).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                pair.forEach { fld ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            fld.label,
                            color = BoardColors.Muted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        NumField(fld.value, fld.ro, accent, Modifier.width(56.dp)) { state.editField(f, fld.key, it) }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (f.t == "fx" && f.gd?.dv == 0) {
            listOf("a" to f.ga, "b" to f.gb, "c" to f.gc).forEach { (k, v) ->
                SliderRow(k, v.toFloat().coerceIn(-10f, 10f), -10f..10f, fmt(v, 1), accent) {
                    state.editField(f, k, (it * 10).roundToInt() / 10.0)
                }
            }
        }
        if (f.t == "trig") {
            SliderRow("θ", f.ga.toFloat().coerceIn(0f, 90f), 0f..90f, "${f.ga.roundToInt()}°", accent) {
                state.editField(f, "th", it.roundToInt().toDouble())
            }
            SliderRow("hyp", f.gb.toFloat().coerceIn(1f, 12f), 1f..12f, fmt(f.gb, 1), accent) {
                state.editField(f, "h", (it * 2).roundToInt() / 2.0)
            }
        }
        if (f.t != "pt" && f.t != "fx" && f.t != "trig" && f.t != "sol" && f.t != "ink" && f.t != "txt" && f.t != "img") {
            SliderRow("⤢", (f.sz * 100).toFloat().coerceIn(20f, 300f), 20f..300f, "${(f.sz * 100).roundToInt()}%", accent) {
                state.editField(f, "size", it.roundToInt().toDouble())
            }
        }
        if (f.t != "pt" && f.t != "circ" && f.t != "fx" && f.t != "trig" && f.t != "sol" && f.t != "ink" && f.t != "txt" && f.t != "img") {
            SliderRow("↻", f.rot.toFloat().coerceIn(-180f, 180f), -180f..180f, "${f.rot.roundToInt()}°", accent) {
                state.editField(f, "rot", it.roundToInt().toDouble())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleTab(state: BoardState, f: Fig, accent: Color, tip: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (f.t != "img") FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Swatches.forEach { c ->
                Swatch(c, (f.col ?: figColor(f.t)) == c) { state.edit(f, "Colour") { f.col = c } }
            }
        }
        // switches: (key, short label, long name)
        val toggles: List<Triple<String, String, String>> = when (f.t) {
            "tri", "rtri" -> listOf(
                Triple("med", "Med", "Medians"), Triple("alt", "Alt", "Altitudes"), Triple("bis", "Bis", "Angle bisectors"),
                Triple("cen", "G", "Centroid G"), Triple("orth", "H", "Orthocentre H"), Triple("inc", "In○", "Incircle"),
                Triple("circ", "Out○", "Circumcircle"), Triple("subt", "∠O", "Angle at the centre"), Triple("mid", "Mid", "Midpoints"),
            )
            "quad", "rect", "pent", "hex" -> listOf(
                Triple("diag", "Diag", "Diagonals"), Triple("mid", "Mid", "Midpoints"),
                Triple("cen", "G", "Centroid G"), Triple("cir", "Out○", "Circumcircle"),
            )
            "circ" -> listOf(Triple("rad", "r", "Radius line and length"), Triple("dia", "Dia", "Diameter"))
            else -> emptyList()
        }
        if (toggles.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                toggles.forEach { (k, short, long) ->
                    Chip(short, k in f.ov, accent, onLong = { tip(long) }, small = true) {
                        state.edit(f, "Toggled " + long) { f.ov = if (k in f.ov) f.ov - k else f.ov + k }
                    }
                }
            }
        }
        if (f.t == "sol") {
            val flags = listOf(
                Triple("spin", "↻", "Spin"), Triple("wire", "▦", "Wire frame"), Triple("lab", "ABC", "Corner letters"),
                Triple("diag", "d", "Diagonal"), Triple("hgt", "h", "Height"), Triple("axes", "xyz", "Axes"),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                flags.forEach { (k, short, long) ->
                    val key = if (k == "lab") "nolab" else k
                    val on = if (k == "lab") key !in f.sflags else key in f.sflags
                    Chip(short, on, accent, onLong = { tip(long) }, small = true) {
                        state.edit(f, "Toggled " + long) {
                            f.sflags = if (key in f.sflags) f.sflags - key else f.sflags + key
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTab(state: BoardState, f: Fig) {
    Column(
        Modifier.heightIn(max = 170.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        propsOf(f, state.parGeo()).forEachIndexed { i, line ->
            com.aismartmath.board.ui.components.PopIn(i, fromBelow = true) { RichLine(line, katex = true) }
        }
    }
}

@Composable
private fun TabIcon(icon: String, on: Boolean, accent: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) accent.copy(alpha = 0.6f) else Color(0xFF3B271B), tween(160), label = "tabBg")
    val lift by animateFloatAsState(if (on) 1.06f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "tabLift")
    Box(
        Modifier
            .graphicsLayer { scaleX = lift; scaleY = lift }
            .size(22.dp)
            .shadow(if (on) 5.dp else 3.dp, RoundedCornerShape(8.dp), clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(BorderStroke(1.dp, if (on) Color(0xFFF1D3A5) else Color(0xFF6B4630)), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(icon, fontSize = 10.sp, color = Color.White) }
}

/** An icon-only action. Long-press it to read what it does. */
@Composable
private fun ActionBtn(icon: String, label: String, accent: Color, tip: (String) -> Unit, on: Boolean = false, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val clickNow by rememberUpdatedState(onClick)
    val tipNow by rememberUpdatedState(tip)
    val sc by animateFloatAsState(
        if (pressed) 0.86f else 1f,
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "actPress",
    )
    val bd by animateColorAsState(if (pressed || on) Color(0xFFF1D3A5) else Color(0xFF6B4630), tween(120), label = "actBorder")
    val fill by animateColorAsState(if (pressed) accent else if (on) accent.copy(alpha = 0.6f) else Color(0xFF3B271B), tween(120), label = "actFill")
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .size(24.dp)
            .shadow(if (pressed) 1.dp else 4.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, bd), shape)
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
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        tipNow(label)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) { Text(icon, fontSize = 11.sp, color = Color.White) }
}

@Composable
private fun Swatch(c: Color, on: Boolean, onClick: () -> Unit) {
    val ring by animateFloatAsState(if (on) 1f else 0f, tween(200), label = "ring")
    val lift by animateFloatAsState(if (on) 1.18f else 1f, spring(dampingRatio = 0.45f), label = "swLift")
    Box(
        Modifier
            .graphicsLayer { scaleX = lift; scaleY = lift }
            .size(17.dp)
            .shadow(3.dp, CircleShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CircleShape)
            .background(c)
            .border(BorderStroke(2.dp, Color.White.copy(alpha = ring)), CircleShape)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, shown: String, accent: Color, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 11.sp, modifier = Modifier.width(24.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f).height(26.dp),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color(0xFF5C3D28),
            ),
        )
        Text(shown, color = BoardColors.Muted, fontSize = 10.sp, modifier = Modifier.width(40.dp))
    }
}

@Composable
internal fun NumField(value: Double, readOnly: Boolean, accent: Color, modifier: Modifier, onValue: (Double) -> Unit) {
    var text by remember { mutableStateOf(fmt(value)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!focused) text = fmt(value) }
    val bd by animateColorAsState(if (focused) accent else Color(0xFF6B4630), tween(180), label = "fieldBd")
    BasicTextField(
        value = text,
        onValueChange = {
            text = it
            it.toDoubleOrNull()?.let(onValue)
        },
        readOnly = readOnly,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TextStyle(color = if (readOnly) BoardColors.Muted else Color.White, fontSize = 11.sp, letterSpacing = 0.3.sp),
        cursorBrush = SolidColor(accent),
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF160E0A))
            .border(BorderStroke(1.dp, bd), RoundedCornerShape(7.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}
