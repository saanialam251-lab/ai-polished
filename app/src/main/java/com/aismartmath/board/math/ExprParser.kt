package com.aismartmath.board.math

import com.aismartmath.board.model.Fn
import com.aismartmath.board.model.ParDef
import kotlin.math.atan2
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * Port of the web parse / parseEq / toTex. Instead of JavaScript eval it uses a small safe parser.
 * Supports: + - * / ^ ** ² ³ ( ) x y a b c e pi π, implicit multiplication (2x, a(x+1), 3sin(x)) and
 * sqrt cosec csc sec cot asin acos atan sinh cosh tanh sin cos tan abs exp floor ceil sign log ln.
 */
private val TOKEN = Regex("\\d*\\.?\\d+|nrt|sqrt|cbrt|asinh|acosh|atanh|gamma|round|frac|hypot|logb|log2|pow|erf|cosec|csc|sec|cot|asin|acos|atan2|atan|sinh|cosh|tanh|sin|cos|tan|abs|exp|floor|ceil|sign|log|ln|max|min|mod|pi|[xyabcet]|\\*\\*|[-+*/(),]")
private val FUNCS = setOf("nrt", "asinh", "acosh", "atanh", "gamma", "round", "frac", "hypot", "logb", "log2", "pow", "erf", "sqrt", "cbrt", "cosec", "csc", "sec", "cot", "asin", "acos", "atan2", "atan", "sinh", "cosh", "tanh", "sin", "cos", "tan", "abs", "exp", "floor", "ceil", "sign", "log", "ln", "max", "min", "mod")
private val TWO = setOf("nrt", "pow", "hypot", "logb", "max", "min", "mod", "atan2")
private val NUMBER = Regex("^(\\d|\\.)")

private fun isF(t: String) = t in FUNCS
private fun isV(t: String) = NUMBER.containsMatchIn(t) || t == "pi" || (t.length == 1 && t[0] in "xyabcet")
private fun startsAtom(t: String) = isV(t) || isF(t) || t == "("

/** |x| written with bars becomes abs(x); bars may be nested: ||x|-1|. */
private fun fixBars(s: String): String {
    if ('|' !in s) return s
    val sb = StringBuilder()
    for (ch in s) {
        if (ch == '|') {
            val prev = sb.lastOrNull { !it.isWhitespace() }
            val opener = prev == null || prev in "+-*/^(,="
            if (opener) sb.append("abs(") else sb.append(')')
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

private fun normalize(src: String, stripY: Boolean): String {
    var s = fixBars(src.lowercase()).replace("theta", "t").replace("θ", "t").replace("[", "(").replace("]", ")")
    if (stripY) s = s.replace(Regex("^\\s*y\\s*="), "")
    return nthRoots(s.replace(Regex("\\s"), "").replace("π", "pi").replace("^", "**").replace("²", "**2").replace("³", "**3"))
}

/** sqr(x) and root(x) mean sqrt(x); 4rt(x) and 4brt(x) and (4/5)brt(6/8) are n-th roots, written nrt(n, value). */
private fun nthRoots(s0: String): String {
    var s = s0.replace("sqr(", "sqrt(").replace("root(", "sqrt(")
    s = Regex("(?<![a-z])(\\d+(?:\\.\\d+)?)rt\\(").replace(s, "nrt(\$1,")
    var guard = 0
    while (guard++ < 20) {
        var k = s.indexOf("brt")
        while (k > 0 && s[k - 1] == 'c') k = s.indexOf("brt", k + 1)
        if (k < 0) break
        var ls = k
        if (k > 0 && s[k - 1] == ')') {
            var d = 0
            var j = k - 1
            while (j >= 0) {
                if (s[j] == ')') d++ else if (s[j] == '(') {
                    d--
                    if (d == 0) break
                }
                j--
            }
            ls = if (j >= 0) j else 0
        } else {
            var j = k
            while (j > 0 && (s[j - 1].isLetterOrDigit() || s[j - 1] == '.')) j--
            ls = j
        }
        var re = k + 3
        if (re < s.length && s[re] == '(') {
            re = closeParen(s, re)
        } else {
            var j = re
            while (j < s.length && (s[j].isLetterOrDigit() || s[j] == '.')) j++
            re = if (j > re && j < s.length && s[j] == '(') closeParen(s, j) else j
        }
        val left = s.substring(ls, k)
        val right = s.substring(k + 3, re)
        if (left.isEmpty() || right.isEmpty()) {
            s = s.substring(0, k) + s.substring(k + 3)
            continue
        }
        s = s.substring(0, ls) + "nrt(" + unwrap(left) + "," + unwrap(right) + ")" + s.substring(re)
    }
    return s
}

private fun closeParen(s: String, open: Int): Int {
    var d = 0
    var j = open
    while (j < s.length) {
        if (s[j] == '(') d++ else if (s[j] == ')') {
            d--
            if (d == 0) return j + 1
        }
        j++
    }
    return s.length
}

private fun unwrap(t: String): String = if (t.length > 2 && t[0] == '(' && closeParen(t, 0) == t.length) t.substring(1, t.length - 1) else t

// ---------------- evaluation tree ----------------
private sealed interface Node { fun ev(x: Double, y: Double, a: Double, b: Double, c: Double): Double }
private class Num(val v: Double) : Node { override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double) = v }
private class Var(val k: Char) : Node {
    override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double) = when (k) { 'x', 't' -> x; 'y' -> y; 'a' -> a; 'b' -> b; else -> c }
}
private class Neg(val n: Node) : Node { override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double) = -n.ev(x, y, a, b, c) }
private class Bin(val op: Char, val l: Node, val r: Node) : Node {
    override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double): Double {
        val p = l.ev(x, y, a, b, c)
        val q = r.ev(x, y, a, b, c)
        return when (op) { '+' -> p + q; '-' -> p - q; '*' -> p * q; else -> p / q }
    }
}
private class Pow(val l: Node, val r: Node) : Node {
    override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double) = l.ev(x, y, a, b, c).pow(r.ev(x, y, a, b, c))
}
private class Call(val name: String, val arg: Node) : Node {
    override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double): Double {
        val v = arg.ev(x, y, a, b, c)
        return when (name) {
            "sqrt" -> sqrt(v); "sin" -> sin(v); "cos" -> cos(v); "tan" -> tan(v); "abs" -> abs(v); "exp" -> exp(v)
            "log" -> log10(v); "ln" -> ln(v); "cbrt" -> Math.cbrt(v)
            "asinh" -> ln(v + sqrt(v * v + 1)); "acosh" -> ln(v + sqrt(v * v - 1)); "atanh" -> 0.5 * ln((1 + v) / (1 - v))
            "log2" -> ln(v) / ln(2.0); "gamma" -> gammaFn(v); "erf" -> erfFn(v)
            "round" -> floor(v + 0.5); "frac" -> v - floor(v)
            "csc", "cosec" -> 1 / sin(v); "sec" -> 1 / cos(v); "cot" -> 1 / tan(v)
            "asin" -> asin(v); "acos" -> acos(v); "atan" -> atan(v)
            "sinh" -> sinh(v); "cosh" -> cosh(v); "tanh" -> tanh(v)
            "floor" -> floor(v); "ceil" -> ceil(v); "sign" -> sign(v)
            else -> Double.NaN
        }
    }
}

/** Gamma function (Lanczos): gamma(5) = 24. */
private fun gammaFn(x: Double): Double {
    if (x < 0.5) return PI / (sin(PI * x) * gammaFn(1 - x))
    val c = doubleArrayOf(
        0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313, -176.61502916214059,
        12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7,
    )
    val xx = x - 1
    var a = c[0]
    val t = xx + 7.5
    for (i in 1 until 9) a += c[i] / (xx + i)
    return sqrt(2 * PI) * t.pow(xx + 0.5) * exp(-t) * a
}

/** Error function (Abramowitz-Stegun 7.1.26), accurate to about 1e-7. */
private fun erfFn(x: Double): Double {
    val sg = if (x < 0) -1.0 else 1.0
    val ax = abs(x)
    val t = 1.0 / (1.0 + 0.3275911 * ax)
    val y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * exp(-ax * ax)
    return sg * y
}

private class Call2(val name: String, val l: Node, val r: Node) : Node {
    override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double): Double {
        val p = l.ev(x, y, a, b, c)
        val q = r.ev(x, y, a, b, c)
        return when (name) {
            "max" -> if (p.isNaN() || q.isNaN()) Double.NaN else maxOf(p, q)
            "min" -> if (p.isNaN() || q.isNaN()) Double.NaN else minOf(p, q)
            "mod" -> p - q * floor(p / q)
            "pow" -> p.pow(q)
            "hypot" -> Math.hypot(p, q)
            "logb" -> ln(q) / ln(p)
            "nrt" -> when {
                p == 0.0 -> Double.NaN
                q >= 0 -> q.pow(1.0 / p)
                p == Math.rint(p) && p.toLong() % 2L != 0L -> -((-q).pow(1.0 / p))
                else -> Double.NaN
            }
            else -> atan2(p, q)
        }
    }
}

private class Parser(val t: List<String>) {
    var i = 0
    fun pk(): String? = t.getOrNull(i)
    fun nx(): String? = t.getOrNull(i++)

    fun expr(): Node? {
        var l = term() ?: return null
        while (pk() == "+" || pk() == "-") {
            val op = nx()!![0]
            val r = term() ?: return null
            l = Bin(op, l, r)
        }
        return l
    }

    fun term(): Node? {
        var l = unary() ?: return null
        while (true) {
            val tk = pk() ?: break
            if (tk == "*" || tk == "/") {
                nx()
                val r = unary() ?: return null
                l = Bin(tk[0], l, r)
            } else if (startsAtom(tk)) {
                val r = unary() ?: return null
                l = Bin('*', l, r)
            } else break
        }
        return l
    }

    fun unary(): Node? {
        if (pk() == "-") { nx(); val u = unary() ?: return null; return Neg(u) }
        if (pk() == "+") { nx(); return unary() }
        return power()
    }

    fun power(): Node? {
        val b = atom() ?: return null
        if (pk() == "**") { nx(); val e = expo() ?: return null; return Pow(b, e) }
        return b
    }

    fun expo(): Node? {
        if (pk() == "-") { nx(); val e = expo() ?: return null; return Neg(e) }
        if (pk() == "+") { nx(); return expo() }
        return power()
    }

    fun atom(): Node? {
        val tk = nx() ?: return null
        if (tk == "(") {
            val e = expr() ?: return null
            if (nx() != ")") return null
            return e
        }
        if (isF(tk)) {
            if (nx() != "(") return null
            val e = expr() ?: return null
            if (tk in TWO) {
                if (nx() != ",") return null
                val e2 = expr() ?: return null
                if (nx() != ")") return null
                return Call2(tk, e, e2)
            }
            if (nx() != ")") return null
            return Call(tk, e)
        }
        if (tk == "pi") return Num(PI)
        if (tk == "e") return Num(E)
        if (tk.length == 1 && tk[0] in "xyabct") return Var(tk[0])
        return tk.toDoubleOrNull()?.let { Num(it) }
    }
}

private fun tokens(s: String): List<String>? {
    val tk = TOKEN.findAll(s).map { it.value }.toList()
    if (tk.isEmpty() || tk.joinToString("") != s) return null
    return tk
}

/** Web: parse(src). Returns a function of (x, y, a, b, c) or null if it cannot be read. */
fun parseExpr(src: String): Fn? {
    val s = normalize(src, stripY = true)
    val tk = tokens(s) ?: return null
    val p = Parser(tk)
    val n = p.expr() ?: return null
    if (p.i != tk.size) return null
    return { x, y, a, b, c -> n.ev(x, y, a, b, c) }
}

class ParsedEq(val fn: Fn, val imp: Boolean, val par: ParDef? = null)

private val NOFN: Fn = { _, _, _, _, _ -> Double.NaN }

private fun wholeParen(s: String): Boolean {
    if (!s.startsWith("(") || !s.endsWith(")")) return false
    var d = 0
    for (i in s.indices) {
        if (s[i] == '(') d++
        if (s[i] == ')') d--
        if (d == 0 && i < s.length - 1) return false
    }
    return d == 0
}

private fun splitTop(s: String): List<String> {
    val out = mutableListOf<String>()
    var d = 0
    var st = 0
    for (i in s.indices) {
        when (s[i]) {
            '(' -> d++
            ')' -> d--
            ',' -> if (d == 0) {
                out.add(s.substring(st, i))
                st = i + 1
            }
        }
    }
    out.add(s.substring(st))
    return out
}

private fun parametric(s: String): ParsedEq? {
    var body = s
    if (wholeParen(body)) body = body.substring(1, body.length - 1)
    val parts = splitTop(body)
    if (parts.size != 2) return null
    var px = parts[0]
    var py = parts[1]
    if (px.startsWith("y=") && py.startsWith("x=")) {
        val tmp = px
        px = py
        py = tmp
    }
    px = px.removePrefix("x=")
    py = py.removePrefix("y=")
    if (px.contains('=') || py.contains('=') || px.isEmpty() || py.isEmpty()) return null
    val fx = parseExpr(px) ?: return null
    val fy = parseExpr(py) ?: return null
    return ParsedEq(NOFN, false, ParDef(fx, fy, -10.0, 10.0, false))
}

/**
 * Web: parseEq - y = f(x), f(x), an implicit equation like x^2 + y^2 = 25 or max(|x|,|y|) = 3,
 * a parametric curve (cos(t), sin(t)), or a polar curve r = 2 sin(theta).
 */
fun parseEq(src: String): ParsedEq? {
    val s = fixBars(src.lowercase()).replace(Regex("\\s"), "").replace("θ", "t").replace("theta", "t")
        .replace("[", "(").replace("]", ")").replace(";", ",")
    parametric(s)?.let { return it }
    val k = s.indexOf('=')
    if (k < 0) {
        val f = parseExpr(s) ?: return null
        return ParsedEq(f, 'y' in s)
    }
    val l = s.substring(0, k)
    val r = s.substring(k + 1)
    if (l.isEmpty() || r.isEmpty() || r.contains('=')) return null
    if (l == "r" && 'y' !in r) {
        val f = parseExpr(r) ?: return null
        return ParsedEq(NOFN, false, ParDef(f, f, 0.0, 6 * PI, true))
    }
    if (l == "y" && 'y' !in r) {
        val f = parseExpr(r) ?: return null
        return ParsedEq(f, false)
    }
    val f = parseExpr(l) ?: return null
    val g = parseExpr(r) ?: return null
    return ParsedEq({ x, y, a, b, c -> f(x, y, a, b, c) - g(x, y, a, b, c) }, true)
}

/**
 * Calculus typed the way it is written: dy/dx of x^3, d/dx(sin(x)), d2y/dx2 (x^3), derivative of x^2, y' ...
 * Returns (order, expression). Order 0 means "dy/dx = 2x": the right side is just plotted.
 */
fun derivativeRequest(src: String): Pair<Int, String>? {
    val s = src.lowercase().replace(Regex("\\s"), "").replace("²", "^2").replace("[", "(").replace("]", ")")
    val second = listOf("d^2y/dx^2", "d2y/dx2", "d^2/dx^2", "d2/dx2", "secondderivativeof", "secondderivative", "y''", "f''(x)")
    val first = listOf("dy/dx", "dx/dy", "d/dx", "derivativeof", "derivative", "diff", "y'", "f'(x)")
    for ((order, list) in listOf(2 to second, 1 to first)) {
        for (p in list) {
            if (!s.startsWith(p)) continue
            var rest = s.substring(p.length)
            val isEq = rest.startsWith("=")
            rest = rest.removePrefix("=").removePrefix("of").removePrefix(":")
            if (rest.isEmpty()) return null
            if (wholeParen(rest)) rest = rest.substring(1, rest.length - 1)
            return (if (isEq) 0 else order) to rest
        }
    }
    return null
}

private val TYPING_STAR = Regex("([\\dxyabce)])\\*(?=[xyabce](?![a-z])|\\()")
private val TYPING_SQ = Regex("\\^2(?![\\d.])")
private val TYPING_CU = Regex("\\^3(?![\\d.])")

/** Web typing shortcuts: a*x becomes ax, x^2 becomes x², x^3 becomes x³ (a*sin(bx) and 2*3 are left alone). */
fun tidyTyping(o: String): String =
    o.replace(TYPING_STAR, "\$1").replace(TYPING_SQ, "²").replace(TYPING_CU, "³")

fun usesParams(src: String): Boolean =
    Regex("[abc]").containsMatchIn(
        src.lowercase().replace(Regex("sqrt|cbrt|brt|nrt|asinh|acosh|atanh|gamma|round|frac|hypot|logb|log2|pow|erf|cosec|csc|sec|cot|asin|acos|atan2|atan|sinh|cosh|tanh|sin|cos|tan|abs|exp|floor|ceil|sign|log|ln|max|min|mod|theta"), ""),
    )

// ---------------- maths layout (web: toTex / eqTex) ----------------
private class MathBuilder(val t: List<String>) {
    var i = 0
    fun pk(): String? = t.getOrNull(i)
    fun nx(): String? = t.getOrNull(i++)

    fun ex(): MathNode {
        val items = mutableListOf(tm())
        while (pk() == "+" || pk() == "-") {
            items.add(MathNode.Txt(if (nx() == "+") " + " else " − "))
            items.add(tm())
        }
        return if (items.size == 1) items[0] else MathNode.Seq(items)
    }

    fun tm(): MathNode {
        var o = un()
        while (true) {
            val tk = pk() ?: break
            if (tk == "*") {
                nx()
                val implicit = pk()?.let { it.isNotEmpty() && (it[0] in 'a'..'z' || it[0] == '(') } ?: false
                val r = un()
                o = if (implicit) MathNode.Seq(listOf(o, r)) else MathNode.Seq(listOf(o, MathNode.Txt(" · "), r))
            } else if (tk == "/") {
                nx()
                o = MathNode.Frac(o, un())
            } else if (tk != ")" && tk != "+" && tk != "-" && tk != "**") {
                o = MathNode.Seq(listOf(o, un()))
            } else break
        }
        return o
    }

    fun un(): MathNode {
        if (pk() == "-") { nx(); return MathNode.Seq(listOf(MathNode.Txt("−"), un())) }
        if (pk() == "+") { nx(); return un() }
        return pw()
    }

    fun pw(): MathNode {
        val b = at()
        if (pk() == "**") { nx(); return MathNode.Sup(b, un()) }
        return b
    }

    fun at(): MathNode {
        val tk = nx() ?: return MathNode.Txt("")
        if (tk == "(") {
            val o = ex()
            if (pk() == ")") nx()
            return MathNode.Paren(o)
        }
        if ((tk == "nrt" || tk == "pow" || tk == "logb") && pk() == "(") {
            nx()
            val first = ex()
            var second: MathNode = MathNode.Txt("")
            if (pk() == ",") {
                nx()
                second = ex()
            }
            if (pk() == ")") nx()
            return when (tk) {
                "nrt" -> MathNode.Root(second, first)
                "pow" -> MathNode.Sup(MathNode.Paren(first), second)
                else -> MathNode.Seq(listOf(MathNode.Sub(MathNode.Txt("log"), first), MathNode.Paren(second)))
            }
        }
        if (isF(tk)) {
            val o: MathNode
            if (pk() == "(") {
                nx()
                var inner = ex()
                while (pk() == ",") {
                    nx()
                    inner = MathNode.Seq(listOf(inner, MathNode.Txt(", "), ex()))
                }
                o = inner
                if (pk() == ")") nx()
            } else o = un()
            return when (tk) {
                "sqrt" -> MathNode.Sqrt(o)
                "cbrt" -> MathNode.Root(o, MathNode.Txt("3"))
                "log2" -> MathNode.Seq(listOf(MathNode.Sub(MathNode.Txt("log"), MathNode.Txt("2")), MathNode.Paren(o)))
                "gamma" -> MathNode.Seq(listOf(MathNode.Txt("Γ"), MathNode.Paren(o)))
                "abs" -> MathNode.Seq(listOf(MathNode.Txt("|"), o, MathNode.Txt("|")))
                "exp" -> MathNode.Sup(MathNode.Txt("e"), o)
                else -> MathNode.Seq(listOf(MathNode.Txt(funName(tk)), MathNode.Paren(o)))
            }
        }
        return MathNode.Txt(if (tk == "pi") "π" else tk)
    }
}

private fun funName(t: String) = when (t) { "asin" -> "arcsin"; "acos" -> "arccos"; "atan" -> "arctan"; "asinh" -> "arsinh"; "acosh" -> "arcosh"; "atanh" -> "artanh"; else -> t }

fun toMath(src: String): MathNode {
    val s = normalize(src, stripY = true)
    val tk = TOKEN.findAll(s).map { it.value }.toList()
    return MathBuilder(tk).ex()
}

/** Web: eqTex - "y = ..." or "lhs = rhs". */
fun eqMath(src: String): MathNode {
    val s = fixBars(src.lowercase()).replace(Regex("\\s"), "").replace("θ", "t").replace("theta", "t")
    derivativeRequest(src)?.let { (order, inner) ->
        val head = when (order) { 2 -> "d²y/dx² of "; 1 -> "dy/dx of "; else -> "slope = " }
        return MathNode.Seq(listOf(MathNode.Txt(head), toMath(inner)))
    }
    val top = splitTop(if (wholeParen(s)) s.substring(1, s.length - 1) else s)
    if (top.size == 2) {
        val a0 = top[0].removePrefix("x=")
        val b0 = top[1].removePrefix("y=")
        if (!a0.contains('=') && !b0.contains('=')) {
            return MathNode.Seq(listOf(MathNode.Txt("("), toMath(a0), MathNode.Txt(", "), toMath(b0), MathNode.Txt(")")))
        }
    }
    if (s.startsWith("r=")) return MathNode.Seq(listOf(MathNode.Txt("r = "), toMath(s.substring(2))))
    val k = s.indexOf('=')
    if (k < 0) {
        return if ('y' in s) MathNode.Seq(listOf(toMath(s), MathNode.Txt(" = 0")))
        else MathNode.Seq(listOf(MathNode.Txt("y = "), toMath(s)))
    }
    val l = s.substring(0, k)
    val r = s.substring(k + 1)
    return if (l == "y" && 'y' !in r) MathNode.Seq(listOf(MathNode.Txt("y = "), toMath(r)))
    else MathNode.Seq(listOf(toMath(l), MathNode.Txt(" = "), toMath(r)))
}
