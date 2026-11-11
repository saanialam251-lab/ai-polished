package com.aismartmath.board.math

/** A small maths layout tree (the web version used KaTeX for this). Laid out by MathLayout, drawn by ui/components/MathView. */
sealed interface MathNode {
    class Seq(val items: List<MathNode>) : MathNode
    class Txt(val s: String) : MathNode
    class Word(val s: String, val bold: Boolean = false) : MathNode
    class Sup(val base: MathNode, val exp: MathNode) : MathNode
    class Sub(val base: MathNode, val sub: MathNode) : MathNode
    class SubSup(val base: MathNode, val sub: MathNode, val sup: MathNode) : MathNode
    class Frac(val n: MathNode, val d: MathNode) : MathNode
    class Binom(val n: MathNode, val d: MathNode) : MathNode
    class Sqrt(val n: MathNode) : MathNode
    class Root(val n: MathNode, val index: MathNode) : MathNode
    class Paren(val n: MathNode, val open: String = "(", val close: String = ")") : MathNode
    class BigOp(val sym: String, val lo: MathNode?, val hi: MathNode?) : MathNode
    class Matrix(val rows: List<List<MathNode>>, val open: String, val close: String) : MathNode
    class Accent(val n: MathNode, val kind: String) : MathNode
    object Br : MathNode
}

/** Plain text of a node - used to build stable keys for animations. */
fun MathNode.plain(): String = when (this) {
    is MathNode.Seq -> items.joinToString("") { it.plain() }
    is MathNode.Txt -> s
    is MathNode.Word -> s
    is MathNode.Sup -> base.plain() + "^(" + exp.plain() + ")"
    is MathNode.Sub -> base.plain() + "_(" + sub.plain() + ")"
    is MathNode.SubSup -> base.plain() + "_(" + sub.plain() + ")^(" + sup.plain() + ")"
    is MathNode.Frac -> "(" + n.plain() + ")/(" + d.plain() + ")"
    is MathNode.Binom -> "C(" + n.plain() + "," + d.plain() + ")"
    is MathNode.Sqrt -> "sqrt(" + n.plain() + ")"
    is MathNode.Root -> "root" + index.plain() + "(" + n.plain() + ")"
    is MathNode.Paren -> open + n.plain() + close
    is MathNode.BigOp -> sym + "_" + (lo?.plain() ?: "") + "^" + (hi?.plain() ?: "")
    is MathNode.Matrix -> "[" + rows.joinToString(";") { r -> r.joinToString(",") { it.plain() } } + "]"
    is MathNode.Accent -> kind + "(" + n.plain() + ")"
    MathNode.Br -> "\n"
}
