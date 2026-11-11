package com.aismartmath.board.board

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.model.Tool
import com.aismartmath.board.model.Tools
import com.aismartmath.board.model.Topic
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.theme.BoardColors
import kotlinx.coroutines.delay

private class Grp(val id: String, val icon: String, val name: String, val hint: String, val tools: List<String>)

private val GROUPS = listOf(
    Grp("write", "🖊️", "Write", "Pen, highlighter, eraser — colours, widths and line styles", listOf("pen", "hl", "er")),
    Grp("lines", "╱", "Lines", "Straight lines, curves and points", listOf("seg", "curve", "pt")),
    Grp("flat", "△", "Shapes", "Triangles, quadrilaterals, circles and more", listOf("tri", "rtri", "quad", "rect", "pent", "hex", "circ")),
    Grp("solid", "🧊", "3D", "Cube, cuboid, pyramid, prism, cylinder, cone, sphere", listOf("cube", "cuboid", "pyr", "prism", "cyl", "cone", "sph")),
    Grp("graph", "ƒ", "Graph", "Graphs, derivatives and trigonometry", listOf("fx", "dv", "trig", "unit")),
)

private val DIRECT = listOf("sel", "pan", "sk", "q")

/**
 * The tool bar: a few icons, never scrolling. Pen / lines / shapes / 3D / graph open a small box with their tools
 * (and, for the pen, its colours, width and line style); Select, Move, Content and Answer are one tap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolBar(state: BoardState, topic: Topic) {
    val accent = BoardColors.Indigo
    val shape = RoundedCornerShape(12.dp)
    val menu = state.toolMenu
    val pickTool: (String) -> Unit = { id ->
        if (id == "curve") {
            state.selectTool("pen")
            state.penStyle = 0
            state.showToast("〰 Curve — draw a curved line with the pen")
        } else {
            state.selectTool(id)
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        Row(
            Modifier.glass(shape, accent).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var i = 0
            GROUPS.forEach { g ->
                val ids = g.tools.filter { it == "curve" || it in topic.toolIds }
                if (ids.isNotEmpty()) {
                    val on = menu == g.id || state.tool in g.tools
                    ToolIcon(Tool(g.id, g.icon, g.name, g.hint), on, i++, accent, { state.showToast(g.icon + "  " + g.name + " — " + g.hint) }, null) {
                        val open = state.toolMenu == g.id
                        state.toolMenu = if (open) null else g.id
                    }
                }
            }
            DIRECT.forEach { id ->
                if (id in topic.toolIds) {
                    val tool = Tools.byId.getValue(id)
                    ToolIcon(tool, state.tool == id, i++, accent, { state.showToast(tool.icon + "  " + tool.name + " — " + tool.hint) }, null) {
                        state.selectTool(id)
                    }
                }
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = menu != null,
            enter = expandVertically(spring(dampingRatio = 0.7f, stiffness = 300f)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(160)) + fadeOut(tween(120)),
        ) {
            val g = GROUPS.firstOrNull { it.id == menu } ?: GROUPS.first()
            Column(
                Modifier.padding(top = 4.dp).glass(shape, accent).padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    g.tools.filter { it == "curve" || it in topic.toolIds }.forEachIndexed { i, id ->
                        ToolCell(Tools.byId.getValue(id), state.tool == id, i, accent, null) {
                            pickTool(id)
                            state.toolMenu = if (g.id == "write") "write" else null
                        }
                    }
                }
                if (g.id == "write") {
                    if (state.isPenTool) PenPanel(state) else if (state.isEraserTool) EraserPanel(state)
                }
            }
        }
    }
}

@Composable
private fun ToolIcon(tool: Tool, on: Boolean, index: Int, accent: Color, onLong: () -> Unit, onDouble: (() -> Unit)?, onClick: () -> Unit) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(60L + index * 40L)
        enter.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 240f))
    }
    val haptic = LocalHapticFeedback.current
    val clickNow by rememberUpdatedState(onClick)
    val longNow by rememberUpdatedState(onLong)
    val doubleNow by rememberUpdatedState(onDouble)
    var pressed by remember { mutableStateOf(false) }
    val bounce by animateFloatAsState(if (pressed) 0.88f else if (on) 1.08f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "toolBounce")
    val bg by animateColorAsState(if (pressed) accent else if (on) accent.copy(alpha = 0.55f) else Color(0xFF3B271B), tween(140), label = "toolBg")
    val bd by animateColorAsState(if (on || pressed) Color(0xFFF1D3A5) else Color(0xFF6B4630), tween(140), label = "toolBd")
    val shape = RoundedCornerShape(9.dp)
    Box(
        Modifier
            .size(28.dp)
            .graphicsLayer {
                val e = enter.value
                alpha = e.coerceIn(0f, 1f)
                translationX = (1f - e) * -40f
                scaleX = bounce
                scaleY = bounce
            }
            .shadow(if (on && !pressed) 7.dp else if (pressed) 1.dp else 4.dp, shape, clip = false, ambientColor = if (on) accent else Color.Black, spotColor = if (on) accent else Color.Black)
            .clip(shape)
            .background(bg)
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
                        longNow()
                    },
                    onDoubleTap = if (onDouble == null) null else fun(_: Offset) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        doubleNow?.invoke()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) { Text(tool.icon, fontSize = 14.sp, color = Color.White) }
}

@Composable
private fun MoreButton(open: Boolean, accent: Color, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (open) 180f else 0f, spring(dampingRatio = 0.6f), label = "moreRot")
    val shape = RoundedCornerShape(9.dp)
    val haptic = LocalHapticFeedback.current
    val clickNow by rememberUpdatedState(onClick)
    val fill by animateColorAsState(if (open) accent.copy(alpha = 0.7f) else Color(0xFF4A3022), tween(160), label = "moreFill")
    Box(
        Modifier
            .size(28.dp)
            .shadow(if (open) 1.dp else 4.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, if (open) Color(0xFFF1D3A5) else Color(0xFF6B4630)), shape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    clickNow()
                })
            },
        contentAlignment = Alignment.Center,
    ) { Text("▾", fontSize = 13.sp, color = Color.White, modifier = Modifier.graphicsLayer { rotationZ = rot }) }
}

/** One tool in the "more" grid: its icon with a tiny name under it. */
@Composable
private fun ToolCell(tool: Tool, on: Boolean, index: Int, accent: Color, onDouble: (() -> Unit)?, onClick: () -> Unit) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 25L)
        enter.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 260f))
    }
    val haptic = LocalHapticFeedback.current
    val clickNow by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (pressed) accent else if (on) accent.copy(alpha = 0.55f) else Color(0xFF3B271B), tween(140), label = "cellBg")
    val shape = RoundedCornerShape(9.dp)
    Column(
        Modifier
            .width(42.dp)
            .graphicsLayer {
                val e = enter.value
                alpha = e.coerceIn(0f, 1f)
                translationY = (1f - e) * -24f
                val s = if (pressed) 0.92f else 1f
                scaleX = s
                scaleY = s
            }
            .shadow(if (pressed) 1.dp else 4.dp, shape, clip = false, ambientColor = if (on) accent else Color.Black, spotColor = if (on) accent else Color.Black)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, if (on || pressed) Color(0xFFF1D3A5) else Color(0xFF6B4630)), shape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                }, onDoubleTap = if (onDouble == null) null else fun(_: Offset) { onDouble() }, onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    clickNow()
                })
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(tool.icon, fontSize = 13.sp, color = Color.White)
        Text(tool.name, fontSize = 7.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
    }
}

/** A small glass tag in the corner of the board: the tool you are holding. */
@Composable
fun ToolBadge(toolId: String) {
    val tool = Tools.byId[toolId] ?: return
    AnimatedContent(
        targetState = tool,
        transitionSpec = {
            (slideInVertically(tween(220)) { -it } + fadeIn(tween(220))) togetherWith
                (slideOutVertically(tween(160)) { it } + fadeOut(tween(120)))
        },
        label = "badge",
    ) { t ->
        Row(
            Modifier.glass(RoundedCornerShape(9.dp), BoardColors.Indigo, 6.dp).padding(horizontal = 7.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(t.icon, fontSize = 10.sp, color = Color.White)
            Text(t.name, fontSize = 9.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
        }
    }
}

/** Soft coloured light drifting behind the board - blurred on Android 12 and newer, a soft gradient before that. */
@Composable
fun AmbientGlow(color: Color) {
    val c by animateColorAsState(color, tween(600), label = "ambColor")
    val t = rememberInfiniteTransition(label = "amb")
    val drift by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift",
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (-40).dp)
                .size(240.dp)
                .graphicsLayer { translationX = drift * 90f; translationY = drift * 40f }
                .blur(60.dp)
                .background(Brush.radialGradient(listOf(c.copy(alpha = 0.40f), Color.Transparent))),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 70.dp, y = 60.dp)
                .size(260.dp)
                .graphicsLayer { translationX = -drift * 80f }
                .blur(70.dp)
                .background(Brush.radialGradient(listOf(Color(0xFFB45309).copy(alpha = 0.22f), Color.Transparent))),
        )
    }
}
