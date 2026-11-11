package com.aismartmath.board.board

/**
 * Names of the corners / ends of a figure. The text "ABC" is the labels A, B, C.
 * A letter can carry primes or digits: "A'B'C'" (or "A′B′C′") is A′, B′, C′ and "A1B2" is A1, B2.
 */
fun splitLabels(nm: String): List<String> {
    val out = mutableListOf<String>()
    for (ch in nm) {
        if (ch == ' ' || ch == ',') continue
        val extra = ch == '\'' || ch == '′' || ch == '″' || ch.isDigit() || ch in "₀₁₂₃₄₅₆₇₈₉"
        if (extra && out.isNotEmpty()) {
            out[out.size - 1] = out[out.size - 1] + (if (ch == '\'') "′" else ch.toString())
        } else {
            out.add(if (ch == '\'') "′" else ch.toString())
        }
    }
    return out
}

/** The labels of a figure as a small indexable list ("?" when there are not enough). */
class Nm(private val l: List<String>) {
    val length: Int get() = l.size
    operator fun get(i: Int): String = l.getOrElse(i) { "?" }

    companion object {
        fun of(nm: String?): Nm = Nm(splitLabels(nm ?: "ABCDEF"))
    }
}

fun labelAt(nm: String?, i: Int): String = Nm.of(nm)[i]

/** The k-th label in the endless run A, B … Z, A′, B′ … Z′, A″ … */
fun runLabel(k: Int): String {
    val letter = ('A' + k % 26).toString()
    val round = k / 26
    return letter + when {
        round == 0 -> ""
        round == 1 -> "′"
        round == 2 -> "″"
        else -> "′".repeat(round)
    }
}
