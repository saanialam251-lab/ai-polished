package com.aismartmath.board.board

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.aismartmath.board.model.Fig

private enum class Mode { Draw, Free, UnitCircle, Rotate, Pan, Handle, Move, Resize, Swallow, Box, GMove, GResize, Crop }

/**
 * Touch handling, ported from the web pointer handlers:
 *  - two fingers always pinch-zoom and pan;
 *  - Point / Triangle / Quadrilateral: a ghost follows the finger, the point is placed when it lifts;
 *  - Line / Rectangle / Right triangle / Circle: drag from corner to corner;
 *  - Pen / Highlighter / Eraser / Sketch: free-hand drawing;
 *  - Select: tap (1-5 taps), drag a handle, or move / resize after 3 / 4 taps;
 *  - hold for 0.8 s: paste / copy / cut / delete menu;
 *  - everything else drags the board.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.boardGestures(state: BoardState): Modifier = pointerInput(state) {
    awaitEachGesture {
        val cam = state.camera
        val down = awaitFirstDown(requireUnconsumed = false)
        // a stylus: its eraser end erases, and a resting palm (any other touch) is ignored while it writes
        val eraserEnd = down.type == PointerType.Eraser
        val stylusDown = down.type == PointerType.Stylus || eraserEnd
        val prevTool = state.tool
        if (eraserEnd && prevTool != "er") state.selectTool("er")
        val tool = state.tool
        val slop = 12.dp.toPx()
        val minStep = 2.dp.toPx()
        val downPos = down.position
        state.msg = ""
        if (state.toolMenu != null) state.toolMenu = null

        var mode = Mode.Pan
        var handle = -1
        var target: Fig? = null
        val start = state.snap(downPos)

        if (state.menu != null) {
            state.hideMenu()
            mode = Mode.Swallow
        } else if (state.drawsWithFinger) {
            mode = Mode.Draw
            if (state.isTapTool) state.cursor = start
            else state.draft = Fig(tool, listOf(start, start), 0L)
        } else if (state.boxMode) {
            val z = state.groupZone(downPos)
            if (z == 2) {
                mode = Mode.GResize
                state.beginGroupResize(downPos)
            } else if (z == 1) {
                mode = Mode.GMove
                state.beginGroupMove(downPos)
            } else {
                mode = Mode.Box
                state.beginBox(downPos)
            }
        } else if (state.isFreeTool) {
            mode = Mode.Free
            state.beginFree(downPos)
        } else if (tool == "unit") {
            mode = Mode.UnitCircle
            state.setUnitAt(downPos)
        } else if (state.isSolidTool) {
            val hs = state.hitSolid(downPos)
            if (hs != null) {
                mode = Mode.Rotate
                target = hs
            }
        } else if (tool == "sel") {
            val mvF = state.mv
            val rsF = state.rs
            val cropH = state.cropHandleAt(downPos)
            val gz = if (state.group.isNotEmpty()) state.groupZone(downPos) else 0
            if (gz == 2) {
                mode = Mode.GResize
                state.beginGroupResize(downPos)
            } else if (gz == 1) {
                mode = Mode.GMove
                state.beginGroupMove(downPos)
            } else if (cropH >= 0) {
                mode = Mode.Crop
                handle = cropH
            } else if (mvF != null && state.shapes.contains(mvF)) {
                mode = Mode.Move
                target = mvF
                state.beginMove(mvF, downPos)
            } else if (rsF != null && state.shapes.contains(rsF)) {
                mode = Mode.Resize
                target = rsF
                state.beginResize(rsF, downPos)
            } else {
                handle = state.findHandle(downPos)
                if (handle >= 0) {
                    mode = Mode.Handle
                } else {
                    val hs = state.hitSolid(downPos)
                    if (hs != null) {
                        mode = Mode.Rotate
                        target = hs
                    }
                }
            }
        }

        if (mode == Mode.Draw || mode == Mode.Pan) state.armHold(downPos)

        var pinching = false
        var moved = false
        var prevSpan = 0f
        var prevCenter = Offset.Zero

        while (true) {
            val ev = awaitPointerEvent()
            if (stylusDown) ev.changes.forEach { if (it.id != down.id) it.consume() }
            val pressed = ev.changes.filter { it.pressed && (!stylusDown || it.id == down.id) }
            if (pressed.isEmpty()) break

            if (pressed.size >= 2) {
                state.cancelHold()
                val a = pressed[0].position
                val b = pressed[1].position
                val c = (a + b) / 2f
                val span = (a - b).getDistance()
                if (!pinching) {
                    pinching = true
                    state.cursor = null
                    state.draft = null
                    state.cancelFree()
                    state.boxA = null
                    state.boxB = null
                } else if (prevSpan > 0f && span > 0f) {
                    cam.zoomAt(c.x.toDouble(), c.y.toDouble(), (span / prevSpan).toDouble())
                    cam.pan(c.x - prevCenter.x, c.y - prevCenter.y)
                }
                prevSpan = span
                prevCenter = c
                ev.changes.forEach { it.consume() }
            } else if (!pinching) {
                val ch = pressed[0]
                if (!moved && (ch.position - downPos).getDistance() > slop) {
                    moved = true
                    state.cancelHold()
                }
                if (!state.holdFired) {
                    when (mode) {
                        Mode.Draw -> {
                            val p = state.snap(ch.position)
                            if (state.isTapTool) state.cursor = p else state.draft = Fig(tool, listOf(start, p), 0L)
                        }
                        Mode.Free -> {
                            // fast writing: use every point the screen reported since the last frame
                            for (hc in ch.historical) state.moveFree(hc.position, minStep)
                            state.moveFree(ch.position, minStep)
                        }
                        Mode.UnitCircle -> state.setUnitAt(ch.position)
                        Mode.Rotate -> {
                            val dd = ch.positionChange()
                            target?.let { state.rotateSolid(it, dd.x, dd.y) }
                        }
                        Mode.Pan -> {
                            val d = ch.positionChange()
                            cam.pan(d.x, d.y)
                        }
                        Mode.Handle -> if (moved) state.dragHandle(handle, ch.position)
                        Mode.Move -> if (moved) target?.let { state.dragMove(it, ch.position) }
                        Mode.Resize -> if (moved) target?.let { state.dragResize(it, ch.position) }
                        Mode.Box -> state.moveBox(ch.position)
                        Mode.GMove -> if (moved) state.dragGroupMove(ch.position)
                        Mode.GResize -> if (moved) state.dragGroupResize(ch.position)
                        Mode.Crop -> if (moved) state.dragCrop(handle, ch.position)
                        Mode.Swallow -> {}
                    }
                }
                ch.consume()
            }
        }

        state.cancelHold()
        if (mode == Mode.GMove) state.endGroupDrag("Moved selection")
        if (mode == Mode.GResize) state.endGroupDrag("Resized selection")
        if (!pinching && !state.holdFired) {
            when (mode) {
                Mode.Draw -> {
                    if (state.isTapTool) {
                        state.cursor?.let { state.placeTapPoint(it) }
                    } else {
                        val d = state.draft
                        if (d != null) state.addFig(tool, d.p)
                    }
                }
                Mode.Free -> state.finishFree()
                Mode.Box -> {
                    state.endBox(moved)
                    if (!moved) state.onBlankTap(downPos)
                }
                Mode.GMove, Mode.GResize -> if (!moved && tool == "sel") state.onSelTap(downPos)
                Mode.Rotate -> if (!moved) {
                    if (tool == "sel") state.onSelTap(downPos) else state.onSolidTap(downPos)
                }
                Mode.Handle -> if (!moved) state.onHandleTap(downPos)
                Mode.Pan, Mode.Move, Mode.Resize -> if (!moved) {
                    if (state.pxOn && state.pxPick(downPos)) {
                        // an angle of the parallel lines was picked
                    } else if (tool == "sel") state.onSelTap(downPos)
                    else if (tool == "pan") state.onBlankTap(downPos)
                    else if (state.isGraphTool) state.onGraphTap(downPos)
                    else if (state.isSolidTool) state.onSolidTap(downPos)
                }
                else -> {}
            }
        }
        state.cursor = null
        state.draft = null
        state.cancelFree()
        state.boxA = null
        state.boxB = null
        if (eraserEnd && prevTool != "er") state.selectTool(prevTool)
    }
}
