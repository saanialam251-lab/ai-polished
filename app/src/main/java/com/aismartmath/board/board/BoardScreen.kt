package com.aismartmath.board.board

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.data.BoardCodec
import com.aismartmath.board.data.BoardStore
import com.aismartmath.board.data.ImageStore
import com.aismartmath.board.math.dist
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Topic
import com.aismartmath.board.model.Tools
import com.aismartmath.board.ui.components.IconBtn
import com.aismartmath.board.ui.components.PopIn
import com.aismartmath.board.ui.components.RenameDialog
import com.aismartmath.board.ui.components.RichLine
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.components.slab
import com.aismartmath.board.ui.theme.BoardColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BoardScreen(
    topic: Topic,
    boardId: String,
    store: BoardStore,
    bg: Color,
    bgName: String,
    onCycleBg: () -> Unit,
    onBack: () -> Unit,
) {
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val lastSaved = remember(boardId) { arrayOf("") }
    val baseUndo = remember(boardId) { intArrayOf(0) }
    val state = remember(boardId) {
        BoardState(topic, scope).also {
            it.camera.density = density.toDouble()
            val meta = store.meta(boardId)
            val saved = if (meta != null) store.load(boardId) else null
            if (saved != null) BoardCodec.decode(it, saved) else it.camera.oy = it.initialOy()
            it.boardName = meta?.name ?: (topic.name + " · " + SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date()))
            it.start()
            baseUndo[0] = it.undoStack.size
            if (saved != null) lastSaved[0] = BoardCodec.encode(it, false)
        }
    }
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val b = ImageStore.decode(ctx, uri)
            if (b != null) state.addImage(b) else state.showToast("Could not open that picture")
        }
    }
    state.pickImage = { picker.launch("image/*") }
    state.readClipboard = { readPhoneClip(ctx) }
    state.clipStamp = { clipStampOf(ctx) }
    var renaming by remember { mutableStateOf(false) }
    var extras by rememberSaveable { mutableStateOf(false) }
    var sideOpen by rememberSaveable { mutableStateOf(false) }
    var calcOpen by rememberSaveable { mutableStateOf(false) }
    val calc = remember { CalcState() }

    // boards save by themselves: every couple of seconds if something changed, and once more when leaving
    LaunchedEffect(boardId) {
        while (true) {
            delay(2500)
            saveBoard(state, store, boardId, lastSaved, baseUndo[0], false)
        }
    }
    DisposableEffect(boardId) {
        onDispose { saveBoard(state, store, boardId, lastSaved, baseUndo[0], false) }
    }
    if (renaming) {
        RenameDialog(
            initial = state.boardName,
            onDismiss = { renaming = false },
            onSave = { n ->
                state.boardName = n
                store.rename(boardId, n)
                renaming = false
            },
        )
    }
    val cam = state.camera
    var full by rememberSaveable { mutableStateOf(false) }
    var minimised by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
    AmbientGlow(BoardColors.Indigo)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        AnimatedVisibility(
            visible = !full,
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(250)) + fadeOut(tween(200)),
        ) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .glass(RoundedCornerShape(14.dp), BoardColors.Indigo)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    IconBtn("←", compact = true, onLongClick = { state.showToast("Back to the topics — this board is saved") }, onClick = onBack)
                    Text(
                        "${topic.icon}  ${state.boardName}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { renaming = true }
                            .padding(vertical = 4.dp),
                    )
                    IconBtn("↶", enabled = state.canUndo, compact = true, onLongClick = { state.showToast("Undo: " + (state.undoStack.lastOrNull()?.label ?: "nothing yet")) }) { state.undo() }
                    IconBtn("↷", enabled = state.canRedo, compact = true, onLongClick = { state.showToast("Redo: " + (state.redoStack.lastOrNull()?.label ?: "nothing yet")) }) { state.redo() }
                    IconBtn("🔗", dim = !state.autoJoin, compact = true, onLongClick = { state.showToast("Auto-join: lines that close up become a shape") }) {
                        state.autoJoin = !state.autoJoin
                        state.showToast(if (state.autoJoin) "Auto-join ON — lines that close up become a shape" else "Auto-join OFF")
                    }
                    IconBtn("🗑", compact = true, onLongClick = { state.showToast("Clear the board — undo brings it back") }) { state.clear() }
                }
                ToolBar(state, topic)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(bg.copy(alpha = 0.9f))) {
            BoardCanvas(state, topic.axes)
            Box(Modifier.align(Alignment.TopStart).padding(8.dp)) { ToolBadge(state.tool) }

            // The side bar: + - ⌂ and Aa are always there. Tap ⌂ and the rest ripples out below it; tap ⌂ again and it folds away.
            // (hold ⌂ to go back to the start view). It scrolls if the screen is short, so nothing is ever squeezed together.
            Column(
                Modifier.align(Alignment.TopEnd).padding(8.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End,
            ) {
                IconBtn("＋", floating = true, onLongClick = { state.showToast("Zoom in") }) { scope.launch { cam.animateZoomAt(cam.w / 2, cam.h / 2, 1.3) } }
                IconBtn("－", floating = true, onLongClick = { state.showToast("Zoom out") }) { scope.launch { cam.animateZoomAt(cam.w / 2, cam.h / 2, 0.77) } }
                IconBtn(
                    if (sideOpen) "⌂▴" else "⌂",
                    floating = true,
                    onLongClick = {
                        scope.launch { cam.animateReset(state.initialOy()) }
                        state.showToast("Back to the start view")
                    },
                ) {
                    sideOpen = !sideOpen
                    if (!sideOpen) extras = false
                }
                IconBtn("Aa", floating = true, onLongClick = { state.showToast("Add your own text, such as a question") }) { state.textDialog = true }
                AnimatedVisibility(
                    visible = sideOpen,
                    enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(160)),
                    exit = shrinkVertically(tween(160)) + fadeOut(tween(120)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                        PopIn(0) {
                            IconBtn("🧮", floating = true, dim = !calcOpen, onLongClick = { state.showToast("Calculator — type with KaTeX maths, drag, shrink or resize it") }) {
                                calcOpen = !calcOpen
                            }
                        }
                        PopIn(1) {
                            IconBtn("K", floating = true, onLongClick = { state.showToast("K: turn maths in selected text into neat symbols (x^2 → x²)") }) { state.applyMath() }
                        }
                        PopIn(2) {
                            IconBtn("∥", floating = true, dim = !state.pxOn || state.parGeo() == null, onLongClick = { state.showToast("Parallel lines: angles made by a line crossing two parallel lines") }) {
                                if (state.parGeo() == null) {
                                    state.showToast("∥ Draw two parallel lines and a line across them — or select figures with Content")
                                } else {
                                    state.pxOn = !state.pxOn
                                }
                            }
                        }
                        PopIn(3) {
                            IconBtn("☰", floating = true, dim = !extras, onLongClick = { state.showToast("More: save, paste, picture, layers, background") }) { extras = !extras }
                        }
                        AnimatedVisibility(
                            visible = extras,
                            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                            exit = shrinkVertically(tween(150)) + fadeOut(tween(150)),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                                IconBtn("💾", floating = true, onLongClick = { state.showToast("Save this board now") }) {
                                    val ok = saveBoard(state, store, boardId, lastSaved, baseUndo[0], true)
                                    state.showToast(if (ok) "Saved ✓ — find it under 📂 My boards" else "Nothing to save yet")
                                }
                                IconBtn("📥", floating = true, onLongClick = { state.showToast("Paste text or a picture you copied") }) { state.pasteHere(null) }
                                IconBtn("🖼️", floating = true, onLongClick = { state.showToast("Add a picture from your phone") }) { state.pickImage() }
                                IconBtn("🗂", floating = true, dim = !state.layersOpen, onLongClick = { state.showToast("Layers: show, hide, lock, reorder") }) {
                                    state.layersOpen = !state.layersOpen
                                }
                                IconBtn("🎨", floating = true, onLongClick = { state.showToast("Background: $bgName — tap to change") }) {
                                    onCycleBg()
                                }
                            }
                        }
                        PopIn(4) {
                            IconBtn("?", floating = true, onLongClick = { state.showToast("How to use the features, with pictures") }) {
                                state.helpTopic = if (state.isGraphTool) "graph" else "content"
                                state.helpOpen = true
                            }
                        }
                        PopIn(5) {
                            IconBtn("🎯", floating = true, onLongClick = { state.showToast("Back to the start view") }) { scope.launch { cam.animateReset(state.initialOy()) } }
                        }
                        PopIn(6) {
                            IconBtn(if (full) "✕" else "⛶", floating = true, onLongClick = { state.showToast("Full screen: hide the panels") }) { full = !full }
                        }
                        PopIn(7) { ZoomBadge(cam.z) }
                    }
                }
            }

            // bottom panel (tool hint or edit panel) with its minimise button at the top-left
            androidx.compose.animation.AnimatedVisibility(
                visible = !full,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(150)),
            ) {
                Column(
                    Modifier.fillMaxWidth().widthIn(max = 700.dp).padding(8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MinButton(minimised) { minimised = !minimised }
                        if (minimised && state.ai.busy) {
                            Text(
                                "🧠 " + state.ai.ticker.take(70),
                                color = Color(0xFFD9B88F),
                                fontSize = 10.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(bottom = 4.dp).weight(1f, fill = false)
                                    .glass(RoundedCornerShape(8.dp), BoardColors.Indigo, 4.dp).padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !minimised,
                        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                        exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
                    ) {
                        val ed = state.editing?.takeIf { state.shapes.contains(it) }
                        AnimatedContent(
                            targetState = ed,
                            transitionSpec = {
                                (fadeIn(tween(220)) togetherWith fadeOut(tween(150))).using(SizeTransform(clip = false))
                            },
                            label = "dock",
                        ) { e ->
                            if (e != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AnimatedVisibility(
                                        visible = state.labelsOpen,
                                        enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                                        exit = shrinkVertically(tween(160)) + fadeOut(tween(120)),
                                    ) { LabelsPanel(state, e) }
                                    AnimatedVisibility(
                                        visible = state.namesOpen && (e.t == "seg" || e.t == "tri" || e.t == "quad" || e.t == "rect" || e.t == "rtri" || e.t == "pent" || e.t == "hex"),
                                        enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                                        exit = shrinkVertically(tween(150)) + fadeOut(tween(110)),
                                    ) { NamesBar(state, e) }
                                    EditPanel(state, e)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AnimatedVisibility(
                                        visible = state.isGraphTool,
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) { GraphPanel(state) }
                                    AnimatedVisibility(
                                        visible = state.pending.isNotEmpty(),
                                        enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                                        exit = shrinkVertically(tween(160)) + fadeOut(tween(120)),
                                    ) { ConfirmBar(state) }
                                    AnimatedVisibility(
                                        visible = state.group.isNotEmpty() && state.grpDetailsOpen,
                                        enter = expandVertically(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
                                        exit = shrinkVertically(tween(160)) + fadeOut(tween(120)),
                                    ) { GroupDetailsPanel(state) }
                                    AnimatedVisibility(
                                        visible = state.group.isNotEmpty(),
                                        enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                                        exit = shrinkVertically(tween(160)) + fadeOut(tween(120)),
                                    ) { ContentBar(state) }
                                    AnimatedVisibility(
                                        visible = state.tool == "sk",
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) { SketchPanel(state) }
                                    AnimatedVisibility(
                                        visible = state.tool == "sel",
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) { SelectPanel(state) }
                                    AnimatedVisibility(
                                        visible = state.tool != "q" && state.group.isEmpty() && (state.ai.busy || state.ai.hasOutput),
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) {
                                        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(12.dp), BoardColors.Indigo, 8.dp).padding(6.dp)) { AiLive(state) }
                                    }
                                    AnimatedVisibility(
                                        visible = state.tool == "q",
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) { AiPanel(state) }
                                    AnimatedVisibility(
                                        visible = state.pxOn && state.parGeo() != null,
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) { ParPanel(state) }
                                    AnimatedVisibility(
                                        visible = state.tool == "trig" || state.tool == "unit",
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) { TrigPanel(state) }
                                    AnimatedVisibility(
                                        visible = state.layersOpen,
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) { LayersPanel(state) }
                                    InfoCard(infoOf(state), BoardColors.Indigo)
                                }
                            }
                        }
                    }
                }
            }

            // the calculator floats above the board; its state is kept while it is closed
            CalculatorWindow(calc, calcOpen) { calcOpen = false }

            if (state.textDialog) AddTextDialog(state)
            if (state.helpOpen) com.aismartmath.board.ui.components.HelpDialog(state.helpTopic) { state.helpOpen = false }
            ToastPill(state.toastMsg, Modifier.align(Alignment.TopCenter).padding(top = 10.dp, end = 56.dp))
            MenuOverlay(state)
        }
    }
    }
}

/** Info bar content. Equality is by [key] so the bar only animates when the words really change. */
private class Info(val key: String, val parts: List<Part>, val katex: Boolean = false) {
    override fun equals(other: Any?) = other is Info && other.key == key
    override fun hashCode() = key.hashCode()
}

private fun infoOf(state: BoardState): Info {
    fun txt(s: String) = Info(s, listOf(Part.T(s)))
    state.msg.takeIf { it.isNotEmpty() }?.let { return txt(it) }
    if (state.pxOn) {
        val g = state.parGeo()
        if (g != null) {
            val parts = pxText(g, state.pxMode, state.pxHl)
            return Info("px:" + partsKey(parts), parts)
        }
    }
    state.mv?.let { return txt("Move mode ✥ — drag the figure, tap empty space when done") }
    state.rs?.let { return txt("Resize ⤢ — drag outward or inward, tap empty space when done") }
    state.draft?.let { d ->
        val a = d.p[0]
        val b = d.p[1]
        return txt(
            when (d.t) {
                "seg" -> "Length ${fmt(dist(a, b))}"
                "circ" -> "Radius ${fmt(dist(a, b))}"
                else -> "${fmt(abs(b.x - a.x))} × ${fmt(abs(b.y - a.y))}"
            },
        )
    }
    state.cursor?.let {
        val k = state.pend.size + 1
        return txt(
            if (state.need > 1) "Point $k of ${state.need} · (${fmt(it.x)}, ${fmt(it.y)})"
            else "Point (${fmt(it.x)}, ${fmt(it.y)})",
        )
    }
    if (state.pend.isNotEmpty()) return txt("Tap point ${state.pend.size + 1} of ${state.need}")

    // web: the info bar describes the active figure (a graph while the Graph / Derivative tools are open)
    val act = state.activeFig()
    if (act != null) {
        val parts: List<Part> = if (act.t == "fx") {
            graphParts(act) + (state.trace?.takeIf { it.f === act }?.let { traceParts(act, it.x) } ?: emptyList())
        } else {
            val out = mutableListOf<Part>()
            propsOf(act, state.parGeo()).forEachIndexed { i, line ->
                if (i > 0) out.add(Part.T(" · "))
                out.addAll(line)
            }
            out
        }
        return Info(partsKey(parts), parts, katex = act.t != "fx")
    }
    val tool = Tools.byId.getValue(state.tool)
    val phase = toolPhase(tool.id)
    return txt(
        if (phase <= 7) "${tool.icon}  ${tool.name} — ${tool.hint}"
        else "${tool.icon}  ${tool.name} — ${tool.hint}  ·  arrives in Phase $phase",
    )
}

@Composable
private fun BoardCanvas(state: BoardState, axes: Boolean) {
    val cam = state.camera
    val measurer = rememberTextMeasurer(cacheSize = 256)

    // tick the clock while anything is popping in or fading out
    LaunchedEffect(state.animTick) {
        val start = System.nanoTime()
        while (true) {
            val t = withFrameNanos { it }
            state.clock = t
            if (t - start > 700_000_000L) break
        }
    }

    // web: "Animate a" - the a slider swings back and forth
    LaunchedEffect(state.animA) {
        if (state.animA) {
            while (true) {
                val t = withFrameNanos { it }
                val v = Math.round(5 * sin(t / 1e6 / 900.0) * 100) / 100.0
                state.lastFx()?.let { it.ga = v }
                state.gpA = v
            }
        }
    }

    // web: "Spin" - solids with spin switched on turn by themselves
    val spinning = state.shapes.any { it.t == "sol" && it.dieAt == 0L && "spin" in it.sflags }
    LaunchedEffect(spinning) {
        if (spinning) {
            var last = withFrameNanos { it }
            while (true) {
                val t = withFrameNanos { it }
                val dt = (t - last) / 1e9
                last = t
                for (s in state.shapes.toList()) {
                    if (s.t == "sol" && s.dieAt == 0L && "spin" in s.sflags) s.ry += 0.72 * dt
                }
            }
        }
    }

    val inf = rememberInfiniteTransition(label = "sel")
    val dashPhase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "dash",
    )
    val pulse by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Canvas(
        Modifier
            .fillMaxSize()
            .onSizeChanged {
                cam.w = it.width.toDouble()
                cam.h = it.height.toDouble()
            }
            .boardGestures(state),
    ) {
        drawGrid(cam, axes || state.forceAxes, measurer)
        val clock = state.clock
        val layerList = state.layers.toList()
        val shapes = state.shapes.toList().filter { f -> layerList.firstOrNull { it.id == f.layer }?.visible != false }
        val quiet = quietSet(shapes, state.editing ?: state.sel)
        val grpSet: Set<Fig> = state.group.toSet()
        val plan = labelPlan(cam, shapes)
        val ordered = layerList.filter { it.visible }.flatMap { l -> shapes.filter { it.layer == l.id } } +
            shapes.filter { f -> layerList.none { it.id == f.layer } }

        for (s in ordered) {
            val la = layerList.firstOrNull { it.id == s.layer }?.opacity ?: 1f
            if (s.t == "sol") {
                val aSol = if (s.dieAt != 0L) {
                    1f - ((clock - s.dieAt) / 240_000_000.0).toFloat().coerceIn(0f, 1f)
                } else {
                    minOf(1f, ((clock - s.born) / 500_000_000.0).toFloat().coerceIn(0f, 1f) * 2.5f)
                }
                drawSolid(cam, measurer, s, aSol * la)
                continue
            }
            if (s.t == "unit") {
                val dk = if (s.dieAt != 0L) ((clock - s.dieAt) / 240_000_000.0).toFloat().coerceIn(0f, 1f) else 0f
                drawUnit(cam, measurer, s, (1f - dk) * la)
                continue
            }
            if (s.t == "img" || s.t == "txt") {
                val dk = if (s.dieAt != 0L) ((clock - s.dieAt) / 240_000_000.0).toFloat().coerceIn(0f, 1f) else 0f
                val ak = (1f - dk) * la
                if (s.t == "img") drawPicture(cam, s, ak) else drawTextFig(cam, measurer, s, ak)
                continue
            }
            if (s.t == "ink") {
                val dk = if (s.dieAt != 0L) ((clock - s.dieAt) / 240_000_000.0).toFloat().coerceIn(0f, 1f) else 0f
                drawInk(cam, s, (1f - dk) * la)
                continue
            }
            if (s.t == "fx") {
                if (s.dieAt != 0L) {
                    val dk = ((clock - s.dieAt) / 240_000_000.0).toFloat().coerceIn(0f, 1f)
                    drawGraph(cam, s, 1f, (1f - dk) * la)
                } else {
                    val k = ((clock - s.born) / 500_000_000.0).toFloat().coerceIn(0f, 1f)
                    drawGraph(cam, s, k, la)
                }
                continue
            }
            if (s.dieAt != 0L) {
                val dk = ((clock - s.dieAt) / 240_000_000.0).toFloat().coerceIn(0f, 1f)
                drawFig(cam, measurer, s, 1f - 0.3f * dk, (1f - dk) * la, s in quiet, axes, false, plan.skip[s] ?: emptySet(), plan.lenT[s] ?: 0.5f)
            } else {
                val k = ((clock - s.born) / 500_000_000.0).toFloat().coerceIn(0f, 1f)
                drawFig(cam, measurer, s, easeOutBack(k), minOf(1f, k * 2.5f) * la, s in quiet, axes, false, plan.skip[s] ?: emptySet(), plan.lenT[s] ?: 0.5f)
            }
        }
        // figures whose eye is on keep showing the degrees at their crossings, selected or not
        val degSet: Set<Fig> = shapes.filter { "eye" in it.ov }.toSet()
        drawCrossings(cam, measurer, shapes, quiet, degSet)
        drawJoints(cam, measurer, shapes, quiet, degSet)

        val sel = state.sel
        if (sel != null && sel.t != "fx" && sel.dieAt == 0L && shapes.contains(sel)) {
            if (sel.t == "sol") drawSolidSelection(cam, sel, dashPhase, pulse, state.mv === sel || state.rs === sel)
            else if (sel.t == "img" || sel.t == "txt") drawBoxSelection(cam, sel, dashPhase, pulse)
            else drawSelection(cam, sel, dashPhase, pulse, state.mv === sel || state.rs === sel)
        }
        val ed = state.editing
        if (ed != null && ed.t != "fx" && ed.dieAt == 0L && shapes.contains(ed) && state.cropping !== ed) drawHandles(cam, ed, pulse)
        state.cropping?.let { if (shapes.contains(it)) drawCropFrame(cam, it) }
        if (state.group.isNotEmpty()) state.groupBounds()?.let { drawGroupBox(cam, it, dashPhase) }
        state.pendRect?.let { r ->
            drawLiveBox(Offset(cam.sx(r[0]), cam.sy(r[3])), Offset(cam.sx(r[2]), cam.sy(r[1])))
        }
        val ba = state.boxA
        val bb = state.boxB
        if (ba != null && bb != null) drawLiveBox(ba, bb)
        if (state.pxOn) {
            val cur = state.parGeo()
            if (state.pxAllSets) state.parAll().forEach { if (it !== cur) drawParallel(cam, measurer, it, "all", 0, false, true) }
            cur?.let { drawParallel(cam, measurer, it, state.pxMode, state.pxHl, state.pxBis) }
        }
        state.trace?.let { tr ->
            if (tr.f.dieAt == 0L && shapes.contains(tr.f)) drawTrace(cam, measurer, tr.f, tr.x, pulse)
        }

        if (state.pend.isNotEmpty() || state.cursor != null) {
            if (state.isTapTool && state.tool != "pt") drawPending(cam, measurer, state.pend.toList(), state.cursor, state.tool)
            else state.cursor?.let { drawPending(cam, measurer, emptyList(), it, state.tool) }
        }
        state.draft?.let { drawFig(cam, measurer, it, 1f, 0.7f, false, axes) }

        // free-hand: the stroke in progress, the eraser ring and the fading sketch
        if (state.skP.size > 1 && state.tool != "er") {
            drawLiveStroke(state.skP.toList(), state.tool, state.penCol, state.penW)
        }
        if (state.tool == "er") state.penAt?.let { drawEraserRing(it, state.eraserDp, pulse) }
        if (state.ghost.isNotEmpty()) {
            val fade = 1f - ((clock - state.ghostAt) / 450_000_000.0).toFloat().coerceIn(0f, 1f)
            drawGhost(cam, state.ghost, fade)
        }
    }
}

@Composable
private fun ToastPill(text: String?, modifier: Modifier) {
    val last = remember { arrayOf("") }
    if (text != null) last[0] = text
    val shape = RoundedCornerShape(20.dp)
    AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.6f, stiffness = 300f)) { -it * 2 } + fadeIn(tween(150)),
        exit = slideOutVertically(tween(200)) { -it * 2 } + fadeOut(tween(200)),
    ) {
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .glass(shape, BoardColors.Indigo)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(last[0], color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MenuOverlay(state: BoardState) {
    val menu = state.menu
    val last = remember { arrayOf<MenuState?>(null) }
    if (menu != null) last[0] = menu
    val d = LocalDensity.current.density
    val accent = BoardColors.Indigo
    val border by animateColorAsState(accent, tween(250), label = "menuBorder")
    AnimatedVisibility(
        visible = menu != null,
        enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 350f), initialScale = 0.5f) + fadeIn(tween(120)),
        exit = scaleOut(tween(120), targetScale = 0.8f) + fadeOut(tween(120)),
    ) {
        val m = last[0]
        if (m != null) {
            val wPx = (m.items.size * 52 + 10) * d
            val left = (m.x - wPx / 2).coerceIn(6f * d, (state.camera.w.toFloat() - wPx - 6f * d).coerceAtLeast(6f * d))
            val top = (m.y - 84f * d).coerceIn(6f * d, (state.camera.h.toFloat() - 90f * d).coerceAtLeast(6f * d))
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .glass(shape, border)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                m.items.forEach { it0 ->
                    Column(
                        Modifier
                            .width(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { state.menuAction(it0.key) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(it0.icon, fontSize = 16.sp, color = Color.White)
                        Text(it0.label, fontSize = 9.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomBadge(z: Double) {
    val pct = z * 100
    val text = if (pct >= 100) "${pct.toLong()}%" else "${fmt(pct, 1)}%"
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE62A1B13))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(text, color = BoardColors.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MinButton(minimised: Boolean, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (minimised) 180f else 0f, tween(250), label = "minRot")
    Row(
        Modifier
            .padding(bottom = 4.dp)
            .slab(RoundedCornerShape(8.dp), Color(0xFF4A3022), Color(0xFF6B4630), 4.dp)
            .clickable(onClick = onClick)
            .height(20.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("▾", color = Color.White, fontSize = 11.sp, modifier = Modifier.graphicsLayer { rotationZ = rot })
        Text(if (minimised) "Show" else "Hide", color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun InfoCard(info: Info, accent: Color) {
    val border by animateColorAsState(accent.copy(alpha = 0.55f), tween(300), label = "infoBorder")
    val shape = RoundedCornerShape(12.dp)
    AnimatedContent(
        targetState = info,
        transitionSpec = {
            (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(220))) togetherWith
                (slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(120)))
        },
        label = "info",
    ) { t ->
        Box(
            Modifier
                .fillMaxWidth()
                .glass(shape, accent)
                .padding(horizontal = 9.dp, vertical = 5.dp),
        ) {
            RichLine(t.parts, katex = t.katex)
        }
    }
}

@Composable
private fun ToolChip(icon: String, name: String, on: Boolean, index: Int, onLong: () -> Unit, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val clickNow by rememberUpdatedState(onClick)
    val longNow by rememberUpdatedState(onLong)
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(80L + index * 50L)
        enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 200f))
    }
    val border by animateColorAsState(if (on) Color(0xFFF1D3A5) else Color(0xFF6B4630), tween(200), label = "border")
    val bg by animateColorAsState(
        if (on) BoardColors.Indigo.copy(alpha = 0.5f) else Color(0xFF3B271B),
        tween(200),
        label = "bg",
    )
    val lift by animateFloatAsState(
        if (on) 1.08f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "lift",
    )
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .graphicsLayer {
                val e = enter.value
                alpha = e.coerceIn(0f, 1f)
                scaleX = (0.5f + 0.5f * e) * lift
                scaleY = (0.5f + 0.5f * e) * lift
            }
            .shadow(if (on) 6.dp else 3.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, border), shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        clickNow()
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        longNow()
                    },
                )
            }
            .padding(horizontal = 9.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 16.sp, color = Color.White)
        Text(name, fontSize = 10.sp, color = Color.White)
    }
}

/** Save the board if it has something worth keeping and has changed. Returns true when a file was written. */
private fun saveBoard(
    state: BoardState,
    store: BoardStore,
    id: String,
    last: Array<String>,
    baseUndo: Int,
    force: Boolean,
): Boolean {
    val alive = state.shapes.count { it.dieAt == 0L }
    val exists = store.meta(id) != null
    if (!exists) {
        if (alive == 0) return false
        // a board that still only holds what the topic put there by itself is not worth saving
        if (!force && state.undoStack.size <= baseUndo && state.redoStack.isEmpty()) return false
    }
    val key = BoardCodec.encode(state, false)
    if (!force && exists && key == last[0]) return false
    store.save(id, state.boardName, state.topic.id, alive, BoardCodec.encode(state))
    last[0] = key
    return true
}

/** What is on the phone's clipboard: a picture (copied from the gallery or a keyboard) or some words. */
private fun readPhoneClip(ctx: Context): ClipItem? {
    return try {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val item = clip.getItemAt(0)
        val uri = item.uri
        if (uri != null) {
            val type = ctx.contentResolver.getType(uri) ?: ""
            if (type.startsWith("image/")) {
                val b = ImageStore.decode(ctx, uri)
                if (b != null) return ClipItem.Picture(b)
            }
        }
        val txt = item.text?.toString()
        if (!txt.isNullOrBlank()) ClipItem.Words(txt) else null
    } catch (e: Exception) {
        null
    }
}

/** When the clipboard last changed (needs Android 8); 0 on older phones. */
private fun clipStampOf(ctx: Context): Long {
    if (Build.VERSION.SDK_INT < 26) return 0L
    return try {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.primaryClipDescription?.timestamp ?: 0L
    } catch (e: Exception) {
        0L
    }
}
