package com.aismartmath.board.board

import androidx.compose.ui.graphics.Color
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Pt

/** Everything about a figure that an edit can change - used to undo and redo edits. */
data class FigSnap(
    val t: String,
    val p: List<Pt>,
    val nm: String?,
    val col: Color?,
    val sz: Double,
    val rot: Double,
    val base: List<Pt>?,
    val ov: Set<String>,
    val ga: Double,
    val gb: Double,
    val gc: Double,
    val sd: Map<String, Double>,
    val rx: Double,
    val ry: Double,
    val sf: Set<String>,
    val layer: Int,
    val hide: Set<String> = emptySet(),
    val only: Int = -1,
    val txt: String = "",
    val fsz: Double = 0.6,
    val crop: List<Float> = listOf(0f, 0f, 1f, 1f),
    val stash: Set<String> = emptySet(),
)

fun Fig.snapshot(): FigSnap = FigSnap(t, p, nm, col, sz, rot, base, ov, ga, gb, gc, sd, rx, ry, sflags, layer, hide, only, txt, fsz, crop, stash)

fun Fig.restore(s: FigSnap) {
    t = s.t
    p = s.p
    nm = s.nm
    col = s.col
    sz = s.sz
    rot = s.rot
    base = s.base
    ov = s.ov
    ga = s.ga
    gb = s.gb
    gc = s.gc
    sd = s.sd
    rx = s.rx
    ry = s.ry
    sflags = s.sf
    layer = s.layer
    hide = s.hide
    only = s.only
    txt = s.txt
    fsz = s.fsz
    crop = s.crop
    stash = s.stash
}

/** One step of history. Every action has a readable label, shown when it is undone or redone. */
sealed interface Act {
    val label: String
}

class AddAct(val fs: List<Fig>, override val label: String) : Act

class RemoveAct(val fs: List<Fig>, val idx: List<Int>, override val label: String) : Act

class EditAct(
    val f: Fig,
    val before: FigSnap,
    var after: FigSnap,
    var time: Long,
    override val label: String,
) : Act

/** Some figures replaced by others (lines joined into a shape, part of a drawing erased). */
class ReplaceAct(
    val removed: List<Fig>,
    val removedIdx: List<Int>,
    val added: List<Fig>,
    val addedIdx: List<Int>,
    val instant: Boolean,
    override val label: String,
) : Act

/** Several figures changed together (a selected box moved or resized). */
class MultiEditAct(
    val fs: List<Fig>,
    val before: List<FigSnap>,
    val after: List<FigSnap>,
    override val label: String,
) : Act
