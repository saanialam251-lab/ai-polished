package com.aismartmath.board.board

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.math.eqMath
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.math.tidyTyping
import com.aismartmath.board.ui.components.MathView
import com.aismartmath.board.math.hasMath
import com.aismartmath.board.math.parseRich
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.theme.BoardColors
import kotlin.math.roundToInt

private val Gold = Color(0xFFE8C38A)

/** Web: the equation panel - typed equation with live maths, Plot, presets, a / b / c sliders, Animate and f' / f''. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GraphPanel(state: BoardState) {
    val shape = RoundedCornerShape(12.dp)
    var tf by remember { mutableStateOf(TextFieldValue(state.eq)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(state.eq) { if (tf.text != state.eq) tf = TextFieldValue(state.eq, TextRange(state.eq.length)) }
    val fieldBorder by animateColorAsState(if (focused) Gold else Color(0xFF6B4630), tween(180), label = "eqBorder")

    Column(
        Modifier
            .fillMaxWidth()
            .glass(shape, Gold)
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.horizontalScroll(rememberScrollState())) { MathView(eqMath(state.eq), 18.sp) }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicTextField(
                value = tf,
                onValueChange = { nv ->
                    val o = nv.text
                    val n = tidyTyping(o)
                    tf = if (n != o) {
                        val q = (nv.selection.start - (o.length - n.length)).coerceIn(0, n.length)
                        TextFieldValue(n, TextRange(q))
                    } else nv
                    state.eq = n
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { state.plot() }),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Gold),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused }
                    .shadow(3.dp, RoundedCornerShape(9.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF160E0A))
                    .border(BorderStroke(1.dp, fieldBorder), RoundedCornerShape(9.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
            PanelButton("Plot", true) { state.plot() }
        }

        PRESETS.withIndex().groupBy { it.value.group }.forEach { (group, items) ->
            Text(group, color = BoardColors.Muted, fontSize = 9.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items.forEach { (i, p) -> Chip(p.name, false, Gold, small = true) { state.preset(i) } }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("a" to state.gpA, "b" to state.gpB, "c" to state.gpC).forEach { (k, v) ->
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(k, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(fmt(v, 1), color = BoardColors.Muted, fontSize = 11.sp)
                    }
                    Slider(
                        value = v.toFloat().coerceIn(-10f, 10f),
                        onValueChange = { state.setParam(k, (it * 10).roundToInt() / 10.0) },
                        valueRange = -10f..10f,
                        colors = SliderDefaults.colors(
                            thumbColor = Gold,
                            activeTrackColor = Gold,
                            inactiveTrackColor = Color(0xFF5C3D28),
                        ),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(if (state.animA) "⏸ a" else "▶ a", state.animA, Gold, onLong = { state.showToast("Animate a: it swings back and forth") }, small = true) {
                state.animA = !state.animA
            }
            Chip("dy/dx", false, Gold, onLong = { state.showToast("Add the slope curve of the last graph — or type: dy/dx of x^3") }, small = true) { state.addDv(1) }
            Chip("d²y/dx²", false, Gold, onLong = { state.showToast("Add the second derivative curve") }, small = true) { state.addDv(2) }
            Chip("?", false, Gold, onLong = { state.showToast("How to use graphs") }, small = true) {
                state.helpTopic = "graph"
                state.helpOpen = true
            }
        }
    }
}

@Composable
private fun PanelButton(label: String, on: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "pbPress",
    )
    val bg by animateColorAsState(if (pressed) Gold else if (on) Gold.copy(alpha = 0.55f) else Color(0xFF3B271B), tween(140), label = "pbBg")
    val bd by animateColorAsState(if (on || pressed) Color(0xFFF1D3A5) else Color(0xFF6B4630), tween(150), label = "pbBd")
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .shadow(if (pressed) 1.dp else 5.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, bd), shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        if (hasMath(label)) {
            val node = remember(label) { parseRich(label) }
            MathView(node, 14.sp, Color.White)
        } else Text(label, color = Color.White, fontSize = 12.sp)
    }
}
