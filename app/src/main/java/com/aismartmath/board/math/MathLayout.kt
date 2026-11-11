package com.aismartmath.board.math

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import kotlin.math.max

/** A laid-out piece of maths: width, height above / below the baseline, and how to draw it at (x, baselineY). */
class MBox(val w: Float, val asc: Float, val desc: Float, val draw: (Canvas, Float, Float) -> Unit) {
    val h: Float get() = asc + desc
}

class MathPaints(argb: Int) {
    /** Smallest letter size nested levels may shrink to. */
    var minS = 0f

    val upright = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.SERIF }
    val italic = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC) }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    init {
        recolor(argb)
    }

    fun recolor(argb: Int) {
        upright.color = argb
        italic.color = argb
        stroke.color = argb
        fill.color = argb
    }
}

private const val AXIS = 0.27f

private fun sm(x: Float, pp: MathPaints): Float = max(x, pp.minS)

private fun empty() = MBox(0f, 0f, 0f) { _, _, _ -> }

private fun text(str: String, s: Float, pp: MathPaints, italic: Boolean, bold: Boolean = false): MBox {
    val p = if (italic) pp.italic else pp.upright
    p.textSize = s
    p.isFakeBoldText = bold
    val trimmed = str.trim()
    if (str.length >= 3 && str.first() == ' ' && str.last() == ' ' && trimmed.isNotEmpty()) {
        val pad = s * 0.2f
        val gw = p.measureText(trimmed)
        return MBox(gw + 2 * pad, s * 0.76f, s * 0.24f) { cv, x, y ->
            p.textSize = s
            p.isFakeBoldText = bold
            cv.drawText(trimmed, x + pad, y, p)
        }
    }
    val extra = if (italic) s * 0.05f else 0f
    val w = p.measureText(str)
    return MBox(w + extra, s * 0.76f, s * 0.24f) { cv, x, y ->
        p.textSize = s
        p.isFakeBoldText = bold
        cv.drawText(str, x, y, p)
    }
}

private fun hbox(items: List<MBox>): MBox {
    if (items.isEmpty()) return empty()
    var w = 0f
    var a = 0f
    var d = 0f
    for (b in items) {
        w += b.w
        a = max(a, b.asc)
        d = max(d, b.desc)
    }
    return MBox(w, a, d) { cv, x, y ->
        var cx = x
        for (b in items) {
            b.draw(cv, cx, y)
            cx += b.w
        }
    }
}

private fun frac(nb: MBox, db: MBox, s: Float, pp: MathPaints, bar: Boolean): MBox {
    val pad = s * 0.12f
    val w = max(nb.w, db.w) + 2 * pad
    val th = max(1f, s * 0.055f)
    val axis = s * AXIS
    val gap = s * 0.14f
    val asc = axis + th / 2 + gap + nb.h
    val desc = max(0f, db.h + gap + th / 2 - axis)
    return MBox(w, asc, desc) { cv, x, y ->
        nb.draw(cv, x + (w - nb.w) / 2f, y - axis - th / 2 - gap - nb.desc)
        db.draw(cv, x + (w - db.w) / 2f, y - axis + th / 2 + gap + db.asc)
        if (bar) {
            val p = pp.stroke
            p.strokeWidth = th
            p.strokeCap = Paint.Cap.BUTT
            cv.drawLine(x + pad * 0.5f, y - axis, x + w - pad * 0.5f, y - axis, p)
            p.strokeCap = Paint.Cap.ROUND
        }
    }
}

private fun scripts(base: MBox, sub: MBox?, sup: MBox?, s: Float): MBox {
    var up = 0f
    var dn = 0f
    if (sup != null) up = max(0.42f * s, base.asc - sup.asc * 0.9f)
    if (sub != null) dn = max(0.18f * s, base.desc - sub.asc * 0.5f)
    val sw = max(sup?.w ?: 0f, sub?.w ?: 0f)
    val lead = s * 0.02f
    val asc = max(base.asc, if (sup != null) up + sup.asc else 0f)
    val desc = max(base.desc, if (sub != null) dn + sub.desc else 0f)
    return MBox(base.w + lead + sw + s * 0.03f, asc, desc) { cv, x, y ->
        base.draw(cv, x, y)
        sup?.draw(cv, x + base.w + lead, y - up)
        sub?.draw(cv, x + base.w + lead, y + dn)
    }
}

private fun sqrtBox(b: MBox, idx: MBox?, s: Float, pp: MathPaints): MBox {
    val th = max(1f, s * 0.06f)
    val gap = s * 0.14f
    var asc = b.asc + gap + th
    val desc = b.desc
    var hh = asc + desc
    if (idx != null) {
        val need = idx.h / 0.72f
        if (hh < need) {
            asc += need - hh
            hh = need
        }
    }
    val rw = s * 0.45f + hh * 0.14f
    val off = if (idx != null) max(0f, idx.w - rw * 0.3f) else 0f
    val w = off + rw + b.w + s * 0.08f
    return MBox(w, asc, desc) { cv, x, y ->
        val top = y - asc
        val bot = y + desc
        val rx = x + off
        val p = pp.stroke
        p.strokeWidth = th
        p.strokeJoin = Paint.Join.MITER
        val path = Path()
        path.moveTo(rx, top + 0.60f * hh)
        path.lineTo(rx + 0.18f * rw, top + 0.52f * hh)
        path.lineTo(rx + 0.45f * rw, bot)
        path.lineTo(rx + rw, top + th / 2)
        path.lineTo(x + w, top + th / 2)
        cv.drawPath(path, p)
        p.strokeJoin = Paint.Join.ROUND
        b.draw(cv, rx + rw, y)
        if (idx != null) idx.draw(cv, x, top + 0.36f * hh + (idx.asc - idx.desc) / 2f)
    }
}

private fun drawBracket(cv: Canvas, kind: String, x: Float, top: Float, bot: Float, w: Float, left: Boolean, pp: MathPaints, s: Float) {
    val p = pp.stroke
    p.strokeWidth = max(1f, s * 0.07f)
    val mid = (top + bot) / 2f
    val hh = bot - top
    fun fx(t: Float): Float = if (left) x + t * w else x + (1f - t) * w
    val path = Path()
    when (kind) {
        "(", ")" -> {
            path.moveTo(fx(0.85f), top)
            path.quadTo(fx(-0.15f), mid, fx(0.85f), bot)
        }
        "[", "]" -> {
            path.moveTo(fx(0.8f), top)
            path.lineTo(fx(0.25f), top)
            path.lineTo(fx(0.25f), bot)
            path.lineTo(fx(0.8f), bot)
        }
        "{", "}" -> {
            path.moveTo(fx(0.85f), top)
            path.quadTo(fx(0.5f), top, fx(0.5f), top + 0.16f * hh)
            path.lineTo(fx(0.5f), mid - 0.1f * hh)
            path.quadTo(fx(0.5f), mid, fx(0.1f), mid)
            path.quadTo(fx(0.5f), mid, fx(0.5f), mid + 0.1f * hh)
            path.lineTo(fx(0.5f), bot - 0.16f * hh)
            path.quadTo(fx(0.5f), bot, fx(0.85f), bot)
        }
        "⌊", "⌋" -> {
            path.moveTo(fx(0.3f), top)
            path.lineTo(fx(0.3f), bot)
            path.lineTo(fx(0.85f), bot)
        }
        "⌈", "⌉" -> {
            path.moveTo(fx(0.3f), bot)
            path.lineTo(fx(0.3f), top)
            path.lineTo(fx(0.85f), top)
        }
        "⟨", "⟩" -> {
            path.moveTo(fx(0.8f), top)
            path.lineTo(fx(0.2f), mid)
            path.lineTo(fx(0.8f), bot)
        }
        "‖" -> {
            path.moveTo(fx(0.3f), top)
            path.lineTo(fx(0.3f), bot)
            path.moveTo(fx(0.7f), top)
            path.lineTo(fx(0.7f), bot)
        }
        else -> {
            path.moveTo(fx(0.5f), top)
            path.lineTo(fx(0.5f), bot)
        }
    }
    cv.drawPath(path, p)
}

/** Brackets that grow with what is inside (a fraction in brackets gets tall brackets). */
private fun brackets(inner: MBox, open: String, close: String, s: Float, pp: MathPaints): MBox {
    if (inner.h <= s * 1.15f) {
        val l = if (open.isEmpty()) null else text(open, s, pp, false)
        val r = if (close.isEmpty()) null else text(close, s, pp, false)
        return hbox(listOfNotNull(l, inner, r))
    }
    val pad = s * 0.08f
    val top = inner.asc + pad
    val bot = inner.desc + pad
    val bw = s * 0.3f
    val lw = if (open.isEmpty()) 0f else bw
    val rw = if (close.isEmpty()) 0f else bw
    val gap = s * 0.04f
    return MBox(lw + inner.w + rw + 2 * gap, top, bot) { cv, x, y ->
        if (open.isNotEmpty()) drawBracket(cv, open, x, y - top, y + bot, bw, true, pp, s)
        inner.draw(cv, x + lw + gap, y)
        if (close.isNotEmpty()) drawBracket(cv, close, x + lw + inner.w + 2 * gap, y - top, y + bot, bw, false, pp, s)
    }
}

private fun bigOp(sym: String, lo: MBox?, hi: MBox?, s: Float, pp: MathPaints): MBox {
    if (sym == "lim") {
        val lb = text("lim", s, pp, false)
        val w = max(lb.w, lo?.w ?: 0f)
        val gap = s * 0.05f
        return MBox(w, lb.asc, lb.desc + (if (lo != null) gap + lo.h else 0f)) { cv, x, y ->
            lb.draw(cv, x + (w - lb.w) / 2f, y)
            lo?.draw(cv, x + (w - lo.w) / 2f, y + lb.desc + gap + lo.asc)
        }
    }
    val glyph = when (sym) {
        "sum" -> "∑"
        "prod" -> "∏"
        "int" -> "∫"
        "iint" -> "∬"
        "oint" -> "∮"
        else -> sym
    }
    val isInt = sym == "int" || sym == "iint" || sym == "oint"
    val ss = if (isInt) s * 1.5f else s * 1.3f
    pp.upright.textSize = ss
    pp.upright.isFakeBoldText = false
    val gw = pp.upright.measureText(glyph)
    val half = ss * (if (isInt) 0.52f else 0.48f)
    val symAsc = AXIS * s + half
    val symDesc = max(0f, half - AXIS * s)
    val symBox = MBox(gw, symAsc, symDesc) { cv, x, y ->
        pp.upright.textSize = ss
        pp.upright.isFakeBoldText = false
        cv.drawText(glyph, x, y + (0.30f * ss - AXIS * s), pp.upright)
    }
    if (!isInt) {
        val gapT = s * 0.08f
        val w = max(gw, max(lo?.w ?: 0f, hi?.w ?: 0f))
        val asc = symAsc + (if (hi != null) gapT + hi.h else 0f)
        val desc = symDesc + (if (lo != null) gapT + lo.h else 0f)
        return MBox(w, asc, desc) { cv, x, y ->
            symBox.draw(cv, x + (w - gw) / 2f, y)
            hi?.draw(cv, x + (w - hi.w) / 2f, y - symAsc - gapT - hi.desc)
            lo?.draw(cv, x + (w - lo.w) / 2f, y + symDesc + gapT + lo.asc)
        }
    }
    val lw = max(lo?.w ?: 0f, hi?.w ?: 0f)
    val hiUp = symAsc * 0.78f
    val loDn = symDesc * 0.85f
    val asc = max(symAsc, hiUp + (hi?.asc ?: 0f))
    val desc = max(symDesc, loDn + (lo?.desc ?: 0f))
    return MBox(gw + s * 0.05f + lw, asc, desc) { cv, x, y ->
        symBox.draw(cv, x, y)
        hi?.draw(cv, x + gw + s * 0.05f, y - hiUp)
        lo?.draw(cv, x + gw + s * 0.05f, y + loDn)
    }
}

private fun accent(b: MBox, kind: String, s: Float, pp: MathPaints): MBox {
    if (kind == "under") {
        return MBox(b.w, b.asc, b.desc + s * 0.18f) { cv, x, y ->
            b.draw(cv, x, y)
            val p = pp.stroke
            p.strokeWidth = max(1f, s * 0.06f)
            cv.drawLine(x, y + b.desc + s * 0.1f, x + b.w, y + b.desc + s * 0.1f, p)
        }
    }
    val lift = s * 0.14f
    return MBox(b.w, b.asc + lift + s * 0.14f, b.desc) { cv, x, y ->
        b.draw(cv, x, y)
        val p = pp.stroke
        p.strokeWidth = max(1f, s * 0.06f)
        val ty = y - b.asc - lift
        val cx = x + b.w / 2f
        when (kind) {
            "hat" -> {
                val pa = Path()
                pa.moveTo(cx - s * 0.2f, ty + s * 0.1f)
                pa.lineTo(cx, ty - s * 0.04f)
                pa.lineTo(cx + s * 0.2f, ty + s * 0.1f)
                cv.drawPath(pa, p)
            }
            "vec" -> {
                cv.drawLine(x + s * 0.05f, ty, x + b.w, ty, p)
                val pa = Path()
                pa.moveTo(x + b.w - s * 0.12f, ty - s * 0.08f)
                pa.lineTo(x + b.w, ty)
                pa.lineTo(x + b.w - s * 0.12f, ty + s * 0.08f)
                cv.drawPath(pa, p)
            }
            "arc" -> {
                val pa = Path()
                pa.moveTo(x + s * 0.05f, ty + s * 0.1f)
                pa.quadTo(cx, ty - s * 0.16f, x + b.w - s * 0.05f, ty + s * 0.1f)
                cv.drawPath(pa, p)
            }
            "dot" -> cv.drawCircle(cx, ty, s * 0.05f, pp.fill)
            "ddot" -> {
                cv.drawCircle(cx - s * 0.1f, ty, s * 0.05f, pp.fill)
                cv.drawCircle(cx + s * 0.1f, ty, s * 0.05f, pp.fill)
            }
            "tilde" -> {
                val pa = Path()
                pa.moveTo(cx - s * 0.2f, ty + s * 0.04f)
                pa.quadTo(cx - s * 0.1f, ty - s * 0.1f, cx, ty + s * 0.03f)
                pa.quadTo(cx + s * 0.1f, ty + s * 0.14f, cx + s * 0.2f, ty - s * 0.02f)
                cv.drawPath(pa, p)
            }
            else -> cv.drawLine(x + s * 0.05f, ty, x + b.w - s * 0.05f, ty, p)
        }
    }
}

private fun matrixBox(rows: List<List<MBox>>, open: String, close: String, s: Float, pp: MathPaints): MBox {
    if (rows.isEmpty()) return empty()
    val nc = rows.maxOf { it.size }
    if (nc == 0) return empty()
    val colW = FloatArray(nc)
    val rowA = FloatArray(rows.size)
    val rowD = FloatArray(rows.size)
    rows.forEachIndexed { r, row ->
        rowA[r] = s * 0.76f
        rowD[r] = s * 0.24f
        row.forEachIndexed { c, b ->
            colW[c] = max(colW[c], b.w)
            rowA[r] = max(rowA[r], b.asc)
            rowD[r] = max(rowD[r], b.desc)
        }
    }
    val gx = s * 0.9f
    val gy = s * 0.3f
    var totalH = gy * (rows.size - 1)
    for (r in rows.indices) totalH += rowA[r] + rowD[r]
    var totalW = gx * (nc - 1)
    for (c in 0 until nc) totalW += colW[c]
    val asc = totalH / 2f + AXIS * s
    val desc = max(0f, totalH / 2f - AXIS * s)
    val inner = MBox(totalW, asc, desc) { cv, x, y ->
        var ty = y - asc
        for (r in rows.indices) {
            val by = ty + rowA[r]
            var cx = x
            for (c in 0 until nc) {
                if (c < rows[r].size) {
                    val b = rows[r][c]
                    b.draw(cv, cx + (colW[c] - b.w) / 2f, by)
                }
                cx += colW[c] + gx
            }
            ty += rowA[r] + rowD[r] + gy
        }
    }
    return brackets(inner, open, close, s, pp)
}

private fun lay(n: MathNode, s: Float, pp: MathPaints): MBox = when (n) {
    is MathNode.Txt -> text(n.s, s, pp, n.s.length == 1 && n.s[0].isLetter())
    is MathNode.Word -> text(n.s, s, pp, false, n.bold)
    is MathNode.Seq -> hbox(n.items.map { lay(it, s, pp) })
    is MathNode.Sup -> scripts(lay(n.base, s, pp), null, lay(n.exp, sm(s * 0.7f, pp), pp), s)
    is MathNode.Sub -> scripts(lay(n.base, s, pp), lay(n.sub, sm(s * 0.7f, pp), pp), null, s)
    is MathNode.SubSup -> scripts(lay(n.base, s, pp), lay(n.sub, sm(s * 0.7f, pp), pp), lay(n.sup, sm(s * 0.7f, pp), pp), s)
    is MathNode.Frac -> frac(lay(n.n, sm(s * 0.9f, pp), pp), lay(n.d, sm(s * 0.9f, pp), pp), s, pp, true)
    is MathNode.Binom -> brackets(frac(lay(n.n, sm(s * 0.9f, pp), pp), lay(n.d, sm(s * 0.9f, pp), pp), s, pp, false), "(", ")", s, pp)
    is MathNode.Sqrt -> sqrtBox(lay(n.n, s, pp), null, s, pp)
    is MathNode.Root -> {
        val body = lay(n.n, s, pp)
        val hh0 = body.h + s * 0.14f + max(1f, s * 0.06f)
        var isz = s * 0.55f
        var idx = lay(n.index, isz, pp)
        var guard = 0
        // a big index (a fraction, a sum …) is made smaller first; if it still does not fit the root grows
        while (idx.h > hh0 * 0.72f && isz > s * 0.3f && guard < 6) {
            isz = max(s * 0.3f, isz * 0.8f)
            idx = lay(n.index, isz, pp)
            guard++
        }
        sqrtBox(body, idx, s, pp)
    }
    is MathNode.Paren -> brackets(lay(n.n, s, pp), n.open, n.close, s, pp)
    is MathNode.BigOp -> bigOp(n.sym, n.lo?.let { lay(it, sm(s * 0.7f, pp), pp) }, n.hi?.let { lay(it, sm(s * 0.7f, pp), pp) }, s, pp)
    is MathNode.Matrix -> matrixBox(n.rows.map { r -> r.map { lay(it, s, pp) } }, n.open, n.close, s, pp)
    is MathNode.Accent -> accent(lay(n.n, s, pp), n.kind, s, pp)
    MathNode.Br -> empty()
}

/** Lays out one expression at font size [s] pixels in colour [argb]. */
fun layoutMath(n: MathNode, s: Float, argb: Int): MBox {
    val pp = MathPaints(argb)
    pp.minS = s * 0.42f
    return lay(n, s, pp)
}

/** A block of text with maths in it: wrapped lines. Draw it with [draw] (scale = target size / layout size). */
class MText(val w: Float, val h: Float, val paints: MathPaints, private val body: (Canvas, Float, Float) -> Unit) {
    fun draw(cv: Canvas, left: Float, top: Float, scale: Float = 1f, argb: Int? = null) {
        if (argb != null) paints.recolor(argb)
        if (scale == 1f) {
            body(cv, left, top)
            return
        }
        cv.save()
        cv.translate(left, top)
        cv.scale(scale, scale)
        body(cv, 0f, 0f)
        cv.restore()
    }
}

fun layoutMathText(root: MathNode, s: Float, maxW: Float, argb: Int): MText {
    val pp = MathPaints(argb)
    pp.minS = s * 0.42f
    val pieces = if (root is MathNode.Seq) root.items else listOf(root)
    val lines = mutableListOf<MutableList<MBox>>()
    var cur = mutableListOf<MBox>()
    var curW = 0f
    for (pc in pieces) {
        if (pc === MathNode.Br) {
            lines.add(cur)
            cur = mutableListOf()
            curW = 0f
            continue
        }
        val b = lay(pc, s, pp)
        val isSpace = pc is MathNode.Word && pc.s.isBlank()
        if (cur.isEmpty() && isSpace) continue
        if (!isSpace && cur.isNotEmpty() && curW + b.w > maxW) {
            lines.add(cur)
            cur = mutableListOf()
            curW = 0f
        }
        cur.add(b)
        curW += b.w
    }
    lines.add(cur)
    val asc = FloatArray(lines.size)
    val desc = FloatArray(lines.size)
    val lw = FloatArray(lines.size)
    val gap = s * 0.22f
    var total = 0f
    var maxLine = 0f
    for (k in lines.indices) {
        asc[k] = s * 0.76f
        desc[k] = s * 0.24f
        for (b in lines[k]) {
            asc[k] = max(asc[k], b.asc)
            desc[k] = max(desc[k], b.desc)
            lw[k] += b.w
        }
        total += asc[k] + desc[k] + (if (k > 0) gap else 0f)
        maxLine = max(maxLine, lw[k])
    }
    return MText(maxLine, total, pp) { cv, left, top ->
        var by = top
        for (k in lines.indices) {
            by += if (k > 0) desc[k - 1] + gap + asc[k] else asc[k]
            var cx = left
            for (b in lines[k]) {
                b.draw(cv, cx, by)
                cx += b.w
            }
        }
    }
}

/** Layouts of board text are made once at a fixed size and drawn scaled, so zooming never re-lays them out. */
object MathCache {
    const val REF = 48f
    private val map = HashMap<String, HashMap<Int, MText>>()
    private var count = 0

    /** [wRatio] = box width divided by letter size. */
    @Synchronized
    fun text(src: String, wRatio: Float): MText {
        val w = Math.round(wRatio * 4f)
        val inner = map[src]
        val hit = inner?.get(w)
        if (hit != null) return hit
        if (count > 300) {
            map.clear()
            count = 0
        }
        val mt = layoutMathText(parseRich(src), REF, wRatio * REF, 0xFFFFFFFF.toInt())
        map.getOrPut(src) { HashMap() }[w] = mt
        count++
        return mt
    }
}
