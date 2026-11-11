package com.aismartmath.board.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.components.MathTextView
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.theme.BoardColors

private val SUBJECTS = listOf(
    "Mathematics" to "➕ Mathematics",
    "Physics" to "⚛️ Physics",
    "Chemistry" to "🧪 Chemistry",
    "English" to "📖 English",
    "Auto" to "🔎 Any subject",
)

private val HINTS = mapOf(
    "Mathematics" to "Type any maths question — solve, prove, find, limits, derivatives, graphs, triangles, MCQ…",
    "Physics" to "Type any physics question — numerical, theory, MCQ, assertion-reason…",
    "Chemistry" to "Type any chemistry question — reactions, mole concept, organic, MCQ…",
    "English" to "Type any English question — grammar, tenses, voice, error spotting, MCQ…",
)

private val Gold = Color(0xFFD9B88F)
private val FieldBg = Color(0xFF160E0A)
private val FieldBorder = BorderStroke(1.dp, Color(0xFF6B4630))
private const val NO_KEY = "No API key yet — open GeminiConfig.kt and put your Gemini key in API_KEY_1."

private fun isHead(l: String) =
    l.startsWith("Given") || l.startsWith("To ") || l.startsWith("Solution") || l.startsWith("Proof") ||
        l.startsWith("Hence") || l.startsWith("Question") || l.startsWith("Construction") || l.startsWith("Diagram") || l.startsWith("Construction")

/** Ask a doubt, do the answer again, or tap a step to have it explained in more detail. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FollowUp(state: BoardState) {
    val acc = BoardColors.Indigo
    val ai = state.ai
    var q by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text("Not clear? Tap any Step above to have it explained, or ask your doubt:", color = BoardColors.Muted, fontSize = 9.sp)
        BasicTextField(
            value = q,
            onValueChange = { q = it },
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            cursorBrush = SolidColor(Color(0xFFD08A4E)),
            decorationBox = { inner ->
                if (q.isEmpty()) Text("e.g. explain step 3 · why is ∠B = ∠C? · do it by another method", color = Color(0xFF9C8470), fontSize = 12.sp)
                inner()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp, max = 70.dp).clip(RoundedCornerShape(6.dp))
                .background(FieldBg).border(FieldBorder, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 5.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Chip("Ask ✓", true, acc, small = true) {
                if (q.isNotBlank()) {
                    ai.followUp(q.trim(), 2)
                    q = ""
                }
            }
            Chip("↻ Do it again", false, acc, small = true) { ai.redo() }
            Chip("📌 Keep on board", false, acc, small = true) { state.keepQuestionDrawing() }
        }
    }
}

/** The live thinking and solution of the AI. It reads the session in the board, so it is the same wherever it is shown. */
@Composable
fun AiLive(state: BoardState) {
    val ai = state.ai
    val scroll = rememberScrollState()
    LaunchedEffect(ai.thoughts.size, ai.lines.size) { scroll.animateScrollTo(scroll.maxValue) }
    if (!ai.hasOutput && !ai.busy) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 230.dp).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (ai.thoughts.isNotEmpty() || ai.busy) {
                Text(
                    if (ai.busy) "🧠 ${ai.phase.ifEmpty { "Thinking…" }}" else "🧠 Thought about",
                    color = Gold,
                    fontSize = 11.sp,
                )
                ai.thoughts.takeLast(14).forEach { t ->
                    MathTextView(t, 10.sp, BoardColors.Muted, Modifier.fillMaxWidth())
                }
            }
            if (ai.lines.isNotEmpty()) {
                Text("✍️ Solution (also written on the board)", color = Gold, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                ai.lines.forEach { l ->
                    val isStep = l.startsWith("Step")
                    val c = when {
                        l.startsWith("Answer:") -> BoardColors.Teal
                        isStep -> Color.White
                        isHead(l) -> Gold
                        else -> Color(0xFFE8E0D8)
                    }
                    MathTextView(
                        l,
                        if (l.startsWith("Answer:")) 14.sp else 12.sp,
                        c,
                        if (isStep && !ai.busy) Modifier.fillMaxWidth().clickable { ai.explainStep(l) } else Modifier.fillMaxWidth(),
                    )
                }
            }
            if (ai.error.isNotBlank()) Text("⚠ ${ai.error}", color = Color(0xFFE59A7A), fontSize = 11.sp)
        }
        if (!ai.busy && ai.lines.isNotEmpty()) FollowUp(state)
    }
}

/**
 * Ask-AI tab. Pick a subject, type the question (shown typeset while typing), press Solve. Gemini thinks (shown here),
 * draws the figures and graphs on the board and writes the full solution, in the Given / To find / Solution layout.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiPanel(state: BoardState) {
    val acc = BoardColors.Indigo
    val ai = state.ai
    var subject by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    fun ask() {
        val s = subject
        if (s == null) { note = "Pick a subject first."; return }
        if (text.isBlank()) { note = "Type the question first."; return }
        if (GeminiClient.keys().isEmpty()) { note = NO_KEY; return }
        note = ""
        ai.start(s, text, null, null)
    }

    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(12.dp), acc, 8.dp).padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("🤖 Ask AI — pick a subject, type your question", color = Color.White, fontSize = 10.sp)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SUBJECTS.forEach { (id, label) ->
                Chip(label, subject == id, acc, small = true) { subject = id }
            }
        }

        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            cursorBrush = SolidColor(Color(0xFFD08A4E)),
            decorationBox = { inner ->
                if (text.isEmpty()) Text(HINTS[subject ?: "Mathematics"] ?: "", color = Color(0xFF9C8470), fontSize = 13.sp)
                inner()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp, max = 100.dp).clip(RoundedCornerShape(6.dp))
                .background(FieldBg).border(FieldBorder, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 5.dp),
        )
        if (text.isNotBlank()) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 80.dp).clip(RoundedCornerShape(6.dp)).background(FieldBg)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
            ) { MathTextView(text, 14.sp, Color(0xFFFFE2B8), Modifier.fillMaxWidth()) }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Chip(if (ai.busy) "Solving…" else "Solve ✓", true, acc, small = true) { if (!ai.busy) ask() }
            if (ai.busy) Chip("■ Stop", false, acc, small = true) { ai.cancel() }
            Chip("Clear", false, acc, small = true) {
                text = ""
                note = ""
                ai.clear()
            }
        }
        if (note.isNotBlank()) Text("⚠ $note", color = Color(0xFFE59A7A), fontSize = 11.sp)
        AiLive(state)
    }
}

/** Shown inside the Content bar when ✨ Gemini is tapped: ask about the selected things (a picture, a figure, text, a question). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiDoubtRow(state: BoardState) {
    val acc = BoardColors.Indigo
    val ai = state.ai
    var q by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("✨ Gemini — ask about what you selected (leave empty to have it solved step by step)", color = Color.White, fontSize = 10.sp)
        BasicTextField(
            value = q,
            onValueChange = { q = it },
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            cursorBrush = SolidColor(Color(0xFFD08A4E)),
            decorationBox = { inner ->
                if (q.isEmpty()) Text("e.g. Solve this · Why is this step true? · Prove it · Find ∠A", color = Color(0xFF9C8470), fontSize = 12.sp)
                inner()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp, max = 80.dp).clip(RoundedCornerShape(6.dp))
                .background(FieldBg).border(FieldBorder, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 5.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Chip(if (ai.busy) "Thinking…" else "Ask Gemini ✓", true, acc, small = true) {
                if (ai.busy) return@Chip
                if (GeminiClient.keys().isEmpty()) { note = NO_KEY; return@Chip }
                val g = state.group
                if (g.isEmpty()) { note = "Select something first."; return@Chip }
                val desc = AiVision.describe(g)
                val ask = if (q.isBlank()) "Solve this completely, step by step, in full detail." else q.trim()
                val full = "Student's request: $ask\n\nWhat the student selected on the board:\n$desc"
                note = ""
                ai.start("Auto", full, AiVision.render(g), AiVision.bounds(g))
            }
            if (ai.busy) Chip("■ Stop", false, acc, small = true) { ai.cancel() }
        }
        if (note.isNotBlank()) Text("⚠ $note", color = Color(0xFFE59A7A), fontSize = 11.sp)
        AiLive(state)
    }
}
