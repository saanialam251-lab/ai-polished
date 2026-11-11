package com.aismartmath.board.math

private val SUP = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
    '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾', 'n' to 'ⁿ', 'i' to 'ⁱ',
)

private val SUB = mapOf(
    '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄', '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
    '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎', 'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ', 'x' to 'ₓ', 'n' to 'ₙ', 'i' to 'ᵢ',
)

internal val CMD = mapOf(
    "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "epsilon" to "ε", "varepsilon" to "ε", "zeta" to "ζ",
    "eta" to "η", "theta" to "θ", "lambda" to "λ", "mu" to "μ", "pi" to "π", "rho" to "ρ", "sigma" to "σ", "tau" to "τ",
    "phi" to "φ", "varphi" to "φ", "omega" to "ω", "Delta" to "Δ", "Gamma" to "Γ", "Theta" to "Θ", "Lambda" to "Λ",
    "Pi" to "Π", "Sigma" to "Σ", "Phi" to "Φ", "Omega" to "Ω",
    "times" to "×", "cdot" to "·", "div" to "÷", "pm" to "±", "mp" to "∓", "leq" to "≤", "le" to "≤", "geq" to "≥",
    "ge" to "≥", "neq" to "≠", "ne" to "≠", "approx" to "≈", "equiv" to "≡", "sim" to "∼", "cong" to "≅",
    "infty" to "∞", "to" to "→", "rightarrow" to "→", "Rightarrow" to "⇒", "leftarrow" to "←", "Leftarrow" to "⇐",
    "Leftrightarrow" to "⇔", "iff" to "⇔", "implies" to "⇒", "sum" to "∑", "prod" to "∏", "int" to "∫",
    "angle" to "∠", "triangle" to "△", "circ" to "°", "degree" to "°", "parallel" to "∥", "perp" to "⊥",
    "cdots" to "⋯", "ldots" to "…", "dots" to "…", "therefore" to "∴", "because" to "∵", "in" to "∈", "notin" to "∉",
    "subset" to "⊂", "cup" to "∪", "cap" to "∩", "emptyset" to "∅", "forall" to "∀", "exists" to "∃",
    "partial" to "∂", "nabla" to "∇", "prime" to "′", "quad" to " ", "qquad" to "  ", "mid" to "∣",
    "lt" to "<", "gt" to ">", "ell" to "ℓ",
)

private val SKIP = setOf("left", "right", "big", "Big", "bigg", "Bigg", "displaystyle", "textstyle", "limits", "nolimits")
private val WRAP = setOf("text", "mathrm", "mathbf", "mathit", "mathbb", "mathcal", "operatorname", "textbf", "textit", "boldsymbol", "overline", "vec", "hat")

/** Reads one argument: {...}, a \command, or a single character. Returns the text and the index after it. */
private fun arg(s: String, from: Int): Pair<String, Int> {
    var i = from
    while (i < s.length && s[i] == ' ') i++
    if (i >= s.length) return "" to i
    if (s[i] == '{') {
        var depth = 0
        var j = i
        while (j < s.length) {
            if (s[j] == '{') depth++
            if (s[j] == '}') {
                depth--
                if (depth == 0) return s.substring(i + 1, j) to (j + 1)
            }
            j++
        }
        return s.substring(i + 1) to s.length
    }
    if (s[i] == '\\') {
        var j = i + 1
        while (j < s.length && s[j].isLetter()) j++
        if (j == i + 1 && j < s.length) j++
        return s.substring(i, j) to j
    }
    return s[i].toString() to (i + 1)
}

/** Reads the script after ^ or _ : a braced group, a command, a number (with sign) or one letter. */
private fun script(s: String, from: Int): Pair<String, Int> {
    var i = from
    if (i < s.length && (s[i] == '{' || s[i] == '\\')) return arg(s, i)
    if (i >= s.length) return "" to i
    val st = i
    if ((s[i] == '-' || s[i] == '+') && i + 1 < s.length && s[i + 1].isDigit()) i++
    if (s[i].isDigit()) {
        while (i < s.length && s[i].isDigit()) i++
        return s.substring(st, i) to i
    }
    return s.substring(st, st + 1) to (st + 1)
}

private fun wrap(t: String): String = if (t.length <= 1 || t.all { it.isLetterOrDigit() }) t else "($t)"

private fun sup(t: String): String {
    if (t.isEmpty()) return ""
    if (t == "°" || t == "′") return t
    val m = t.map { SUP[it] }
    if (m.all { it != null }) return m.joinToString("")
    return if (t.length == 1) "^$t" else "^($t)"
}

private fun sub(t: String): String {
    if (t.isEmpty()) return ""
    val m = t.map { SUB[it] }
    if (m.all { it != null }) return m.joinToString("")
    return if (t.length == 1) "_$t" else "_($t)"
}

private fun conv(s: String): String {
    val out = StringBuilder()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            c == '$' -> i++
            c == '\\' -> {
                var j = i + 1
                if (j >= s.length) {
                    i = j
                } else if (!s[j].isLetter()) {
                    when (s[j]) {
                        ',', ';', ':', '!', ' ' -> {}
                        '(', ')', '[', ']' -> {}
                        else -> out.append(s[j])
                    }
                    i = j + 1
                } else {
                    while (j < s.length && s[j].isLetter()) j++
                    val name = s.substring(i + 1, j)
                    i = j
                    when {
                        name == "frac" || name == "dfrac" || name == "tfrac" -> {
                            val (a, n1) = arg(s, i)
                            val (b, n2) = arg(s, n1)
                            i = n2
                            out.append(wrap(conv(a))).append('/').append(wrap(conv(b)))
                        }
                        name == "sqrt" -> {
                            var root = ""
                            var k = i
                            while (k < s.length && s[k] == ' ') k++
                            if (k < s.length && s[k] == '[') {
                                val e = s.indexOf(']', k)
                                if (e > k) {
                                    root = s.substring(k + 1, e)
                                    i = e + 1
                                }
                            }
                            val (a, n1) = arg(s, i)
                            i = n1
                            val sign = when (root) {
                                "" , "2" -> "√"
                                "3" -> "∛"
                                "4" -> "∜"
                                else -> sup(root) + "√"
                            }
                            out.append(sign).append(wrap(conv(a)))
                        }
                        name in WRAP -> {
                            val (a, n1) = arg(s, i)
                            i = n1
                            out.append(conv(a))
                        }
                        name == "begin" || name == "end" -> {
                            val (_, n1) = arg(s, i)
                            i = n1
                        }
                        name in SKIP -> {}
                        else -> out.append(CMD[name] ?: name)
                    }
                }
            }
            c == '^' -> {
                val (g, n1) = script(s, i + 1)
                i = n1
                out.append(sup(conv(g)))
            }
            c == '_' -> {
                val (g, n1) = script(s, i + 1)
                i = n1
                out.append(sub(conv(g)))
            }
            c == '{' || c == '}' -> i++
            else -> {
                out.append(c)
                i++
            }
        }
    }
    return out.toString()
}

/**
 * Turns maths written the way an AI writes it (LaTeX such as x^2, \frac{a}{b}, \sqrt{x}, \alpha, \le, or plain x^2 - 1 = 1)
 * into neat readable symbols: x² − 1 = 1, a/b, √x, α, ≤ ...
 */
fun texToUnicode(src: String): String {
    var s = src
    s = s.replace("<=", "≤").replace(">=", "≥").replace("!=", "≠").replace("+-", "±").replace("->", "→")
    s = s.replace("sqrt(", "√(")
    return conv(s).replace("  ", " ").trim()
}
