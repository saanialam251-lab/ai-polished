package com.aismartmath.board.math

/*
 * Typed maths -> MathNode.  Works the way people type, no backslashes needed:
 *   a/b  ->  stacked fraction      x^2, x_1, x^(n+1)  ->  powers and subscripts
 *   sqrt(x), sqr(x), sqrt x, root, cbrt(x)  ->  roots      (a/b)  ->  tall brackets
 *   pi, theta, alpha ...  ->  Greek letters                 a*x  ->  ax
 * and the usual LaTeX still works:  \frac{a}{b}  \sqrt[3]{x}  \sum_{i=1}^{n}  \begin{pmatrix} ... \end{pmatrix}
 */

private enum class K { OPERAND, OP, SLASH, PROSE, SPACE, PUNCT, BR }

private class Item(val node: MathNode, val k: K, val glued: Boolean)

private sealed interface Piece
private class Run(val nodes: MutableList<MathNode>) : Piece
private class One(val node: MathNode, val k: K) : Piece

private val FUNCS = setOf(
    "sin", "cos", "tan", "cot", "sec", "csc", "cosec", "arcsin", "arccos", "arctan", "sinh", "cosh", "tanh",
    "log", "ln", "exp", "lim", "max", "min", "gcd", "det", "dim", "deg", "arg", "mod", "sgn", "lg",
)

private val TWO = setOf(
    "if", "is", "of", "to", "or", "in", "at", "on", "by", "an", "as", "be", "do", "go", "he", "it", "me", "my",
    "no", "so", "up", "we", "us", "am", "ok", "hi",
)

private val GREEK = mapOf(
    "pi" to "π", "theta" to "θ", "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "epsilon" to "ε",
    "lambda" to "λ", "mu" to "μ", "rho" to "ρ", "sigma" to "σ", "tau" to "τ", "phi" to "φ", "omega" to "ω",
    "Delta" to "Δ", "Sigma" to "Σ", "Omega" to "Ω", "Pi" to "Π", "Gamma" to "Γ", "Theta" to "Θ", "Lambda" to "Λ", "Phi" to "Φ",
    "zeta" to "ζ", "eta" to "η", "iota" to "ι", "kappa" to "κ", "nu" to "ν", "xi" to "ξ", "upsilon" to "υ", "chi" to "χ", "psi" to "ψ",
    "Xi" to "Ξ", "Psi" to "Ψ", "Upsilon" to "Υ",
)

private val EXTRA = mapOf(
    "subseteq" to "⊆", "supset" to "⊃", "supseteq" to "⊇", "setminus" to "∖", "propto" to "∝", "leftrightarrow" to "↔",
    "langle" to "⟨", "rangle" to "⟩", "land" to "∧", "lor" to "∨", "neg" to "¬", "lnot" to "¬", "vdots" to "⋮",
    "ddots" to "⋱", "star" to "⋆", "ast" to "∗", "oplus" to "⊕", "otimes" to "⊗", "hbar" to "ℏ", "aleph" to "ℵ",
)

private val OPNAMES = setOf(
    "times", "div", "pm", "mp", "cdot", "leq", "le", "geq", "ge", "neq", "ne", "approx", "equiv", "sim", "cong", "in",
    "notin", "subset", "subseteq", "supset", "supseteq", "cup", "cap", "to", "rightarrow", "leftarrow", "Rightarrow",
    "Leftarrow", "Leftrightarrow", "leftrightarrow", "iff", "implies", "propto", "parallel", "perp", "setminus", "land", "lor",
)

private val TEXTCMDS = setOf("text", "textrm", "mathrm", "operatorname", "mbox", "textnormal", "mathsf", "mathtt", "textsf", "mathfrak", "mathcal", "mathscr")
private val BOLDCMDS = setOf("textbf", "mathbf", "boldsymbol", "bm")
private val SKIPCMDS = setOf("big", "Big", "bigg", "Bigg", "displaystyle", "textstyle", "scriptstyle", "limits", "nolimits", "middle")
private val BIGOPS = setOf("sum", "prod", "int", "iint", "oint", "lim", "limsup", "liminf", "bigcup", "bigcap", "coprod")
private val ACCENTS = mapOf(
    "hat" to "hat", "widehat" to "hat", "bar" to "bar", "overline" to "bar", "vec" to "vec", "overrightarrow" to "vec",
    "dot" to "dot", "ddot" to "ddot", "tilde" to "tilde", "widetilde" to "tilde", "underline" to "under",
)
private val OPCH = setOf(
    '±', '×', '÷', '·', '≤', '≥', '≠', '≈', '∈', '∉', '⊂', '⊆', '⊃', '⊇', '∪', '∩', '→', '←', '⇒', '⇐', '⇔', '≡', '∼', '≅',
    '∝', '∥', '⊥', '∧', '∨', '∖', '↔',
)

private fun isWordCh(ch: Char): Boolean {
    if (ch.isLetter()) return true
    val t = Character.getType(ch)
    return t == Character.NON_SPACING_MARK.toInt() || t == Character.COMBINING_SPACING_MARK.toInt()
}

private class RP(val s: String) {
    var i = 0
    val n = s.length

    fun top(): List<MathNode> = resolve(seq(), true)

    fun group(): MathNode {
        val p = resolve(seq(), false)
        return if (p.size == 1) p[0] else MathNode.Seq(p)
    }

    private fun prev(items: List<Item>): Item? {
        var j = items.size - 1
        while (j >= 0 && items[j].k == K.SPACE) j--
        return if (j >= 0) items[j] else null
    }

    fun seq(): MutableList<Item> {
        val items = mutableListOf<Item>()
        var space = true
        while (i < n) {
            val c = s[i]
            val before = i
            if (c == '\n') {
                items.add(Item(MathNode.Br, K.BR, false))
                i++
                space = true
                continue
            }
            if (c == ' ' || c == '\t' || c == '\r') {
                while (i < n && (s[i] == ' ' || s[i] == '\t' || s[i] == '\r')) i++
                val last = items.lastOrNull()
                if (last != null && last.k != K.SPACE && last.k != K.BR) items.add(Item(MathNode.Word(" "), K.SPACE, false))
                space = true
                continue
            }
            val g = !space
            space = false
            atom(items, g)
            if (i == before) i++
        }
        return items
    }

    private fun operand(items: MutableList<Item>, node: MathNode, glued: Boolean) {
        items.add(Item(withScripts(node), K.OPERAND, glued))
    }

    private fun matching(o: Char, c: Char, from: Int): Int {
        var d = 0
        var j = from
        while (j < n) {
            val ch = s[j]
            if (ch == '\\') {
                j += 2
                continue
            }
            if (ch == o) d++ else if (ch == c) {
                d--
                if (d == 0) return j
            }
            j++
        }
        return -1
    }

    private fun withScripts(base: MathNode): MathNode {
        var sub: MathNode? = null
        var sup: MathNode? = null
        while (i < n) {
            val c = s[i]
            if (c == '^' && sup == null) {
                i++
                sup = script(true)
            } else if (c == '_' && sub == null) {
                i++
                sub = script(false)
            } else if (c == '*' && i + 1 < n && s[i + 1] == '*' && sup == null) {
                i += 2
                sup = script(true)
            } else break
        }
        return when {
            sub != null && sup != null -> MathNode.SubSup(base, sub, sup)
            sup != null -> MathNode.Sup(base, sup)
            sub != null -> MathNode.Sub(base, sub)
            else -> base
        }
    }

    /** The small thing after ^ or _ . For ^ the next ^ chains upward, so a^x^2 is a to the (x to the 2). */
    private fun script(chain: Boolean = false): MathNode {
        while (i < n && s[i] == ' ') i++
        if (i >= n) return MathNode.Txt("")
        val c = s[i]
        if (c == '{' || c == '\\') return arg()
        if (c == '(') {
            val m = matching('(', ')', i)
            if (m > 0) {
                val node = RP(s.substring(i + 1, m)).group()
                i = m + 1
                return node
            }
        }
        var j = i
        if (s[j] == '-' || s[j] == '+') j++
        if (j < n && s[j].isDigit()) {
            while (j < n && s[j].isDigit()) j++
            if (j + 1 < n && s[j] == '.' && s[j + 1].isDigit()) {
                j++
                while (j < n && s[j].isDigit()) j++
            }
        } else if (j < n && s[j].isLetter()) {
            var e = j
            while (e < n && s[e].isLetter() && s[e].code < 128) e++
            val w = s.substring(j, e)
            j = if (w == "inf" || w == "infinity" || GREEK.containsKey(w)) e else j + 1
        } else if (j == i) {
            j = i + 1
        }
        val tok = s.substring(i, j)
        i = j
        val node = RP(tok).group()
        if (chain && i < n && s[i] == '^') {
            i++
            return MathNode.Sup(node, script(true))
        }
        return node
    }

    private fun bigScripts(): Pair<MathNode?, MathNode?> {
        var lo: MathNode? = null
        var hi: MathNode? = null
        var k = 0
        while (k < 2 && i < n) {
            if (s[i] == '_') {
                i++
                lo = script(false)
            } else if (s[i] == '^') {
                i++
                hi = script(true)
            } else break
            k++
        }
        return Pair(lo, hi)
    }

    private fun bigShort(items: MutableList<Item>, w: String, glued: Boolean) {
        val hasScript = i < n && (s[i] == '_' || s[i] == '^')
        if (!hasScript) {
            if (w == "int") items.add(Item(MathNode.BigOp("int", null, null), K.OPERAND, glued))
            else if (w == "lim") operand(items, MathNode.Word("lim"), glued)
            else items.add(Item(MathNode.Word(w), K.PROSE, glued))
            return
        }
        val sc = bigScripts()
        items.add(Item(MathNode.BigOp(w, sc.first, sc.second), K.OPERAND, glued))
    }

    /** One argument of a command: {group}, \command or one character. */
    private fun arg(): MathNode {
        while (i < n && s[i] == ' ') i++
        if (i >= n) return MathNode.Txt("")
        val c = s[i]
        if (c == '{') {
            val m = matching('{', '}', i)
            val end = if (m < 0) n else m
            val node = RP(s.substring(i + 1, end)).group()
            i = if (m < 0) n else m + 1
            return node
        }
        if (c == '\\') {
            val st = i
            i++
            if (i < n && s[i].isLetter()) {
                while (i < n && s[i].isLetter()) i++
            } else if (i < n) {
                i++
            }
            return RP(s.substring(st, i)).group()
        }
        i++
        return RP(c.toString()).group()
    }

    private fun rawArg(): String {
        while (i < n && s[i] == ' ') i++
        if (i >= n) return ""
        if (s[i] == '{') {
            val m = matching('{', '}', i)
            val end = if (m < 0) n else m
            val t = s.substring(i + 1, end)
            i = if (m < 0) n else m + 1
            return t
        }
        val t = s[i].toString()
        i++
        return t
    }

    private fun radArg(): MathNode? {
        val start = i
        var j = i
        while (j < n && s[j] == ' ') j++
        if (j >= n) return null
        val c = s[j]
        if (c == '(') {
            val m = matching('(', ')', j)
            if (m > 0) {
                i = m + 1
                return RP(s.substring(j + 1, m)).group()
            }
            return null
        }
        if (c == '{' || c == '\\') {
            i = j
            return arg()
        }
        if (c.isDigit()) {
            var e = j
            while (e < n && s[e].isDigit()) e++
            if (e + 1 < n && s[e] == '.' && s[e + 1].isDigit()) {
                e++
                while (e < n && s[e].isDigit()) e++
            }
            i = e
            return MathNode.Txt(s.substring(j, e))
        }
        if (c.isLetter() && c.code < 128 && (j + 1 >= n || !s[j + 1].isLetter())) {
            i = j + 1
            return MathNode.Txt(c.toString())
        }
        i = start
        return null
    }

    private fun radical(items: MutableList<Item>, glued: Boolean, idx: Int) {
        val a = radArg()
        val node: MathNode = when {
            a == null -> MathNode.Word(if (idx == 3) "cbrt" else "sqrt")
            idx == 2 -> MathNode.Sqrt(a)
            else -> MathNode.Root(a, MathNode.Txt(idx.toString()))
        }
        operand(items, node, glued)
    }

    private fun letters(items: MutableList<Item>, glued: Boolean) {
        val st = i
        while (i < n && isWordCh(s[i])) i++
        while (i + 1 < n && s[i] == '\'' && s[i + 1].isLetter() && i - st >= 2) {
            i++
            while (i < n && isWordCh(s[i])) i++
        }
        val w = s.substring(st, i)
        if (w.any { it.code > 127 }) {
            if (w.all { it.code in 0x370..0x3FF }) {
                var g = glued
                for (ch in w) {
                    operand(items, MathNode.Txt(ch.toString()), g)
                    g = true
                }
            } else {
                items.add(Item(MathNode.Word(w), K.PROSE, glued))
            }
            return
        }
        val low = w.lowercase()
        when {
            w == "sqrt" || w == "sqr" || w == "root" -> radical(items, glued, 2)
            w == "cbrt" -> radical(items, glued, 3)
            w.length > 3 && w.endsWith("brt") && w != "cbrt" -> {
                val iw = w.dropLast(3)
                val idxNode: MathNode = when {
                    iw == "inf" || iw == "infinity" -> MathNode.Txt("∞")
                    GREEK.containsKey(iw) -> MathNode.Txt(GREEK.getValue(iw))
                    iw.length == 1 -> MathNode.Txt(iw)
                    else -> MathNode.Word(iw)
                }
                val a = radArg()
                if (a == null) operand(items, MathNode.Word(w), glued) else operand(items, MathNode.Root(a, idxNode), glued)
            }
            w == "upon" -> items.add(Item(MathNode.Txt("/"), K.SLASH, glued))
            w == "binom" -> {
                val a = radArg()
                val b = radArg()
                if (a != null && b != null) operand(items, MathNode.Binom(a, b), glued) else operand(items, MathNode.Word(w), glued)
            }
            w == "floor" || w == "ceil" -> {
                val a = radArg()
                if (a == null) operand(items, MathNode.Word(w), glued)
                else operand(items, if (w == "floor") MathNode.Paren(a, "⌊", "⌋") else MathNode.Paren(a, "⌈", "⌉"), glued)
            }
            w == "vec" || w == "hat" || w == "bar" || w == "dot" || w == "ddot" || w == "tilde" -> {
                val a = radArg()
                if (a == null) operand(items, MathNode.Word(w), glued) else operand(items, MathNode.Accent(a, w), glued)
            }
            w == "arc" -> {
                var j = i
                while (j < n && s[j] == ' ') j++
                var e = j
                while (e < n && s[e].isUpperCase() && e - j < 4) e++
                if (e - j >= 2) {
                    val body = s.substring(j, e)
                    i = e
                    operand(items, MathNode.Accent(MathNode.Word(body), "arc"), glued)
                } else operand(items, MathNode.Word(w), glued)
            }
            w == "angle" -> operand(items, MathNode.Txt("∠"), glued)
            w == "triangle" -> operand(items, MathNode.Txt("△"), glued)
            w == "deg" -> operand(items, MathNode.Txt("°"), glued)
            w == "perp" -> items.add(Item(MathNode.Txt(" ⊥ "), K.OP, glued))
            w == "parallel" -> items.add(Item(MathNode.Txt(" ∥ "), K.OP, glued))
            w == "cong" -> items.add(Item(MathNode.Txt(" ≅ "), K.OP, glued))
            w == "lim" || w == "sum" || w == "prod" || w == "int" -> bigShort(items, w, glued)
            w in FUNCS -> operand(items, MathNode.Word(w), glued)
            GREEK.containsKey(w) -> operand(items, MathNode.Txt(GREEK.getValue(w)), glued)
            w == "inf" || w == "infinity" -> operand(items, MathNode.Txt("∞"), glued)
            w.length == 1 -> operand(items, MathNode.Txt(w), glued)
            w.length == 2 && low !in TWO -> {
                items.add(Item(MathNode.Txt(w.substring(0, 1)), K.OPERAND, glued))
                operand(items, MathNode.Txt(w.substring(1, 2)), true)
            }
            else -> items.add(Item(MathNode.Word(w), K.PROSE, glued))
        }
    }

    private fun atom(items: MutableList<Item>, glued: Boolean) {
        val c = s[i]
        when {
            c.isDigit() -> {
                val st = i
                while (i < n && s[i].isDigit()) i++
                if (i + 1 < n && s[i] == '.' && s[i + 1].isDigit()) {
                    i++
                    while (i < n && s[i].isDigit()) i++
                }
                val numStr = s.substring(st, i)
                var done = false
                if (s.startsWith("rt", i) && i + 2 < n && (s[i + 2] == '(' || s[i + 2].isLetter() || s[i + 2].isDigit())) {
                    val back = i
                    i += 2
                    val a = radArg()
                    if (a != null) {
                        operand(items, MathNode.Root(a, MathNode.Txt(numStr)), glued)
                        done = true
                    } else i = back
                }
                if (!done && s.startsWith("brt", i)) {
                    val back = i
                    i += 3
                    val a = radArg()
                    if (a != null) {
                        operand(items, MathNode.Root(a, MathNode.Txt(numStr)), glued)
                        done = true
                    } else i = back
                }
                if (!done) operand(items, MathNode.Txt(numStr), glued)
            }
            isWordCh(c) -> letters(items, glued)
            c == '\\' -> command(items, glued)
            c == '$' -> i++
            c == '(' || c == '[' || c == '{' -> {
                val close = if (c == '(') ')' else if (c == '[') ']' else '}'
                val m = matching(c, close, i)
                if (m > i) {
                    val inner = RP(s.substring(i + 1, m)).group()
                    i = m + 1
                    var rooted = false
                    if (c == '(' && s.startsWith("brt", i)) {
                        val back = i
                        i += 3
                        val a = radArg()
                        if (a != null) {
                            operand(items, MathNode.Root(a, inner), glued)
                            rooted = true
                        } else i = back
                    }
                    if (!rooted) operand(items, MathNode.Paren(inner, c.toString(), close.toString()), glued)
                } else {
                    i++
                    items.add(Item(MathNode.Word(c.toString()), K.PUNCT, glued))
                }
            }
            c == '|' -> {
                val j = s.indexOf('|', i + 1)
                val body = if (j > i) s.substring(i + 1, j) else ""
                if (j > i && body.isNotBlank() && !body.contains('\n')) {
                    i = j + 1
                    operand(items, MathNode.Paren(RP(body).group(), "|", "|"), glued)
                } else {
                    i++
                    items.add(Item(MathNode.Word("|"), K.PUNCT, glued))
                }
            }
            c == '√' || c == '∛' || c == '∜' -> {
                i++
                radical(items, glued, if (c == '∛') 3 else if (c == '∜') 4 else 2)
            }
            c == '+' -> {
                i++
                if (i < n && s[i] == '-') {
                    i++
                    items.add(Item(MathNode.Txt(" ± "), K.OP, glued))
                } else {
                    val p = prev(items)
                    if (p == null || p.k == K.OP || p.k == K.SLASH || p.k == K.PUNCT) items.add(Item(MathNode.Txt("+"), K.OPERAND, glued))
                    else items.add(Item(MathNode.Txt(" + "), K.OP, glued))
                }
            }
            c == '-' || c == '−' -> {
                i++
                if (i < n && s[i] == '>') {
                    i++
                    items.add(Item(MathNode.Txt(" → "), K.OP, glued))
                } else {
                    val p = prev(items)
                    if (p == null || p.k == K.OP || p.k == K.SLASH || p.k == K.PUNCT) items.add(Item(MathNode.Txt("−"), K.OPERAND, glued))
                    else if (p.k == K.PROSE && glued && i < n && s[i].isLetter()) items.add(Item(MathNode.Word("-"), K.PUNCT, glued))
                    else items.add(Item(MathNode.Txt(" − "), K.OP, glued))
                }
            }
            c == '=' -> {
                i++
                if (i < n && s[i] == '>') {
                    i++
                    items.add(Item(MathNode.Txt(" ⇒ "), K.OP, glued))
                } else {
                    if (i < n && s[i] == '=') i++
                    items.add(Item(MathNode.Txt(" = "), K.OP, glued))
                }
            }
            c == '<' -> {
                i++
                val rest = s.substring(i, minOf(n, i + 2))
                when {
                    rest == "=>" -> { i += 2; items.add(Item(MathNode.Txt(" ⇔ "), K.OP, glued)) }
                    rest.startsWith("=") -> { i++; items.add(Item(MathNode.Txt(" ≤ "), K.OP, glued)) }
                    rest.startsWith("-") -> { i++; items.add(Item(MathNode.Txt(" ← "), K.OP, glued)) }
                    rest.startsWith(">") -> { i++; items.add(Item(MathNode.Txt(" ≠ "), K.OP, glued)) }
                    else -> items.add(Item(MathNode.Txt(" < "), K.OP, glued))
                }
            }
            c == '>' -> {
                i++
                if (i < n && s[i] == '=') {
                    i++
                    items.add(Item(MathNode.Txt(" ≥ "), K.OP, glued))
                } else items.add(Item(MathNode.Txt(" > "), K.OP, glued))
            }
            c == '!' -> {
                i++
                if (i < n && s[i] == '=') {
                    i++
                    items.add(Item(MathNode.Txt(" ≠ "), K.OP, glued))
                } else items.add(Item(MathNode.Txt("!"), K.OPERAND, glued))
            }
            c == '*' -> {
                if (i + 1 < n && s[i + 1] == '*') {
                    items.add(Item(MathNode.Word("^"), K.PUNCT, glued))
                    i += 2
                } else {
                    i++
                    while (items.isNotEmpty() && items.last().k == K.SPACE) items.removeAt(items.size - 1)
                    while (i < n && s[i] == ' ') i++
                    val p = prev(items)
                    val numBefore = p != null && p.k == K.OPERAND && p.node is MathNode.Txt && (p.node as MathNode.Txt).s.firstOrNull()?.isDigit() == true
                    val numAfter = i < n && s[i].isDigit()
                    if (numBefore && numAfter) items.add(Item(MathNode.Txt(" × "), K.OP, glued))
                    else if (p == null || p.k != K.OPERAND) items.add(Item(MathNode.Txt(" · "), K.OP, glued))
                    // otherwise: a*x is simply ax
                }
            }
            c == '/' -> {
                i++
                items.add(Item(MathNode.Txt("/"), K.SLASH, glued))
            }
            c == ',' -> {
                i++
                val p = prev(items)
                val digitBefore = p != null && p.node is MathNode.Txt && (p.node as MathNode.Txt).s.lastOrNull()?.isDigit() == true
                if (digitBefore && i < n && s[i].isDigit()) {
                    items.add(Item(MathNode.Txt(","), K.OPERAND, true))
                } else {
                    while (i < n && s[i] == ' ') i++
                    items.add(Item(MathNode.Word(", "), K.PUNCT, glued))
                }
            }
            c == ';' || c == ':' || c == '.' || c == '?' -> {
                i++
                items.add(Item(MathNode.Word(c.toString()), K.PUNCT, glued))
            }
            c == '\'' -> {
                var k = 0
                while (i < n && s[i] == '\'' && k < 3) { i++; k++ }
                operand(items, MathNode.Txt(if (k == 1) "′" else if (k == 2) "″" else "‴"), glued)
            }
            c == '^' || c == '_' -> {
                i++
                items.add(Item(MathNode.Word(c.toString()), K.PUNCT, glued))
            }
            c == '%' -> {
                i++
                operand(items, MathNode.Txt("%"), glued)
            }
            Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(s[i + 1]) -> {
                val e = s.substring(i, i + 2)
                i += 2
                items.add(Item(MathNode.Word(e), K.PROSE, glued))
            }
            c in OPCH -> {
                i++
                items.add(Item(MathNode.Txt(" $c "), K.OP, glued))
            }
            else -> {
                i++
                operand(items, MathNode.Txt(c.toString()), glued)
            }
        }
    }

    private fun command(items: MutableList<Item>, glued: Boolean) {
        i++
        if (i >= n) return
        if (!s[i].isLetter()) {
            val ch = s[i]
            i++
            when (ch) {
                '\\' -> items.add(Item(MathNode.Br, K.BR, false))
                '{' -> {
                    val m = s.indexOf("\\}", i)
                    if (m >= 0) {
                        val node = MathNode.Paren(RP(s.substring(i, m)).group(), "{", "}")
                        i = m + 2
                        operand(items, node, glued)
                    } else operand(items, MathNode.Txt("{"), glued)
                }
                ',', ';', ':', ' ' -> items.add(Item(MathNode.Word(" "), K.SPACE, false))
                '!' -> {}
                else -> operand(items, MathNode.Txt(ch.toString()), glued)
            }
            return
        }
        val st = i
        while (i < n && s[i].isLetter()) i++
        val name = s.substring(st, i)
        when {
            name == "frac" || name == "dfrac" || name == "tfrac" || name == "cfrac" -> {
                val a = arg()
                val b = arg()
                operand(items, MathNode.Frac(a, b), glued)
            }
            name == "binom" || name == "dbinom" || name == "tbinom" -> {
                val a = arg()
                val b = arg()
                operand(items, MathNode.Binom(a, b), glued)
            }
            name == "sqrt" -> {
                var idx: MathNode? = null
                var j = i
                while (j < n && s[j] == ' ') j++
                if (j < n && s[j] == '[') {
                    val e = s.indexOf(']', j)
                    if (e > j) {
                        idx = RP(s.substring(j + 1, e)).group()
                        i = e + 1
                    }
                }
                val a = arg()
                operand(items, if (idx == null) MathNode.Sqrt(a) else MathNode.Root(a, idx), glued)
            }
            name == "left" || name == "right" -> {
                if (i < n && s[i] == '.') i++
            }
            name in SKIPCMDS -> {}
            name in TEXTCMDS -> operand(items, MathNode.Word(rawArg()), glued)
            name in BOLDCMDS -> operand(items, MathNode.Word(rawArg(), true), glued)
            name == "textit" || name == "mathit" || name == "emph" -> operand(items, arg(), glued)
            name == "mathbb" -> {
                val t = rawArg()
                val m = when (t) {
                    "R" -> "ℝ"; "N" -> "ℕ"; "Z" -> "ℤ"; "Q" -> "ℚ"; "C" -> "ℂ"; "P" -> "ℙ"
                    else -> t
                }
                operand(items, MathNode.Word(m), glued)
            }
            ACCENTS.containsKey(name) -> operand(items, MathNode.Accent(arg(), ACCENTS.getValue(name)), glued)
            name in BIGOPS -> {
                val sc = bigScripts()
                val lo = sc.first
                val hi = sc.second
                val sym = when (name) {
                    "limsup", "liminf" -> "lim"
                    "bigcup" -> "⋃"
                    "bigcap" -> "⋂"
                    "coprod" -> "∐"
                    else -> name
                }
                items.add(Item(MathNode.BigOp(sym, lo, hi), K.OPERAND, glued))
            }
            name == "begin" -> {
                val env = rawArg()
                val endTag = "\\end{" + env + "}"
                val e = s.indexOf(endTag, i)
                val body = if (e >= 0) s.substring(i, e) else s.substring(i)
                i = if (e >= 0) e + endTag.length else n
                val rows = body.split("\\\\").filter { it.isNotBlank() }.map { r -> r.split("&").map { cell -> RP(cell).group() } }
                val d = when (env.removeSuffix("*")) {
                    "pmatrix" -> "(" to ")"
                    "bmatrix" -> "[" to "]"
                    "Bmatrix" -> "{" to "}"
                    "vmatrix" -> "|" to "|"
                    "Vmatrix" -> "‖" to "‖"
                    "cases" -> "{" to ""
                    else -> "" to ""
                }
                operand(items, MathNode.Matrix(rows, d.first, d.second), glued)
            }
            name == "end" -> rawArg()
            name == "quad" -> items.add(Item(MathNode.Word("  "), K.SPACE, false))
            name == "qquad" -> items.add(Item(MathNode.Word("    "), K.SPACE, false))
            name == "mid" -> items.add(Item(MathNode.Txt(" ∣ "), K.OP, glued))
            name in OPNAMES -> items.add(Item(MathNode.Txt(" " + (CMD[name] ?: EXTRA[name] ?: name) + " "), K.OP, glued))
            name in FUNCS -> operand(items, MathNode.Word(name), glued)
            else -> {
                val sym = CMD[name] ?: EXTRA[name] ?: GREEK[name]
                if (sym != null) operand(items, MathNode.Txt(sym), glued) else operand(items, MathNode.Word(name), glued)
            }
        }
    }
}

private fun isFn(n: MathNode): Boolean = when (n) {
    is MathNode.Word -> true
    is MathNode.Sub -> n.base is MathNode.Word
    is MathNode.Sup -> n.base is MathNode.Word
    is MathNode.SubSup -> n.base is MathNode.Word
    else -> false
}

/** A function name (sin, log_a …) is followed by a little gap, even inside brackets. */
private fun endsWithFn(p: Piece?): Boolean = p is Run && p.nodes.isNotEmpty() && isFn(p.nodes[p.nodes.size - 1])

private fun runNode(r: Run): MathNode = if (r.nodes.size == 1) r.nodes[0] else MathNode.Seq(r.nodes)

private fun stripP(n: MathNode): MathNode = if (n is MathNode.Paren && n.open == "(" && n.close == ")") n.n else n

private fun resolve(items: List<Item>, keepSpaces: Boolean): List<MathNode> {
    val hasProse = items.any { it.k == K.PROSE }
    val pieces = mutableListOf<Piece>()
    for (item in items) {
        when (item.k) {
            K.OPERAND -> {
                val last = pieces.lastOrNull()
                if (last is Run && item.glued) last.nodes.add(item.node) else pieces.add(Run(mutableListOf(item.node)))
            }
            K.SPACE -> if (keepSpaces || hasProse || endsWithFn(pieces.lastOrNull())) pieces.add(One(item.node, K.SPACE))
            else -> pieces.add(One(item.node, item.k))
        }
    }
    var i = 0
    while (i < pieces.size) {
        val pc = pieces[i]
        if (pc is One && pc.k == K.SLASH) {
            var p = i - 1
            while (p >= 0 && pieces[p].let { it is One && it.k == K.SPACE }) p--
            var q = i + 1
            while (q < pieces.size && pieces[q].let { it is One && it.k == K.SPACE }) q++
            if (p >= 0 && q < pieces.size) {
                val a = pieces[p]
                val b = pieces[q]
                if (a is Run && b is Run) {
                    val fr = MathNode.Frac(stripP(runNode(a)), stripP(runNode(b)))
                    for (k in q downTo p) pieces.removeAt(k)
                    pieces.add(p, Run(mutableListOf<MathNode>(fr)))
                    i = p + 1
                    continue
                }
            }
        }
        i++
    }
    val out = mutableListOf<MathNode>()
    for (pc in pieces) {
        when (pc) {
            is Run -> out.add(runNode(pc))
            is One -> out.add(pc.node)
        }
    }
    return out
}

/** The typed text as a list of pieces (words, spaces, maths). A new line is a [MathNode.Br]. */
fun richPieces(src: String): List<MathNode> =
    try {
        RP(src.replace("\r", "")).top()
    } catch (e: Exception) {
        listOf(MathNode.Word(src))
    }

fun parseRich(src: String): MathNode = MathNode.Seq(richPieces(src))

private val WORDSPLIT = Regex("[^A-Za-z]+")

/** Quick check: does this line contain anything the maths layout would change? */
fun hasMath(s: String): Boolean {
    for (ch in s) {
        if (ch == '^' || ch == '/' || ch == '_' || ch == '\\' || ch == '*' || ch == '√' || ch == '|') return true
    }
    if (s.contains("<=") || s.contains(">=") || s.contains("!=") || s.contains("->")) return true
    return s.split(WORDSPLIT).any { it in GREEK || it == "sqrt" || it == "sqr" || it == "cbrt" || it == "root" || it == "inf" }
}
