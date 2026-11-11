package com.aismartmath.board.board

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * One AI answer in progress. It lives in the board (not in a panel), so it keeps going when the bottom bar is hidden:
 * Gemini's thinking is collected here for the bar, the figures are drawn and the solution is written on the board
 * line by line while the answer is still arriving.
 */
class AiSession(private val state: BoardState, private val scope: CoroutineScope) {
    var busy by mutableStateOf(false)
    var phase by mutableStateOf("")
    var error by mutableStateOf("")
    var question by mutableStateOf("")
    val thoughts = mutableStateListOf<String>()
    val lines = mutableStateListOf<String>()
    private var job: Job? = null
    private var lastThought = ""
    private var baseSubject = "Auto"
    private var basePicture: Bitmap? = null
    private var baseAnchor: DoubleArray? = null

    val hasOutput: Boolean get() = thoughts.isNotEmpty() || lines.isNotEmpty() || error.isNotEmpty()

    /** The latest thing Gemini is thinking, for the one-line ticker shown while the bar is hidden. */
    val ticker: String get() = thoughts.lastOrNull() ?: phase

    private fun tidy(s: String): String =
        s.replace("**", "").replace("__", "").replace("```", "").replace("`", "")
            .replace(Regex("^#+\\s*"), "").replace(Regex("^\\*\\s+"), "• ").trim()

    private fun addThought(s: String) {
        for (raw in s.lines()) {
            val t = tidy(raw)
            if (t.isEmpty() || t == lastThought) continue
            lastThought = t
            thoughts.add(t)
        }
        while (thoughts.size > 60) thoughts.removeAt(0)
    }

    fun clear() {
        cancel()
        thoughts.clear()
        lines.clear()
        error = ""
        phase = ""
        question = ""
        state.clearQuestionDrawing()
    }

    /** A doubt, "do it again" or "explain this step", asked after an answer. */
    fun followUp(request: String, mode: Int) {
        if (busy || question.isEmpty()) return
        val ctx = "Earlier question:\n$question\n\nEarlier answer:\n" + lines.filter { it.isNotBlank() }.joinToString("\n") +
            "\n\nThe student now says: $request"
        start(baseSubject, ctx, basePicture, baseAnchor, mode)
    }

    fun redo() = followUp("Do the whole solution again carefully from the start, check every step, and correct any mistake.", 1)

    fun explainStep(step: String) =
        followUp("Explain this step in much more detail, slowly, with the reason, the formula and every small calculation: $step", 2)

    fun cancel() {
        job?.cancel()
        job = null
        if (busy) {
            busy = false
            phase = "Stopped"
        }
    }

    /** mode 0 = a new question, 1 = do it again (the board is drawn again), 2 = add an explanation under what is there. */
    fun start(subject: String, text: String, picture: Bitmap?, anchor: DoubleArray?, mode: Int = 0) {
        cancel()
        thoughts.clear()
        error = ""
        lastThought = ""
        if (mode == 0) {
            question = text
            baseSubject = subject
            basePicture = picture
            baseAnchor = anchor
        }
        if (mode != 2) lines.clear() else lines.add("↳ More detail")
        busy = true
        phase = "Thinking…"
        state.aiBegin(anchor, keep = mode == 2)
        job = scope.launch {
            val ch = Channel<Pair<Int, String>>(Channel.UNLIMITED)
            launch {
                val err = GeminiClient.stream(subject, text, picture) { kind, s -> ch.trySend(kind to s) }
                ch.trySend(-1 to (err ?: ""))
            }
            val buf = StringBuilder()
            var failure = ""
            loop@ for ((kind, s) in ch) {
                when (kind) {
                    GeminiClient.THOUGHT -> addThought(s)
                    GeminiClient.CHUNK -> {
                        buf.append(s)
                        while (true) {
                            val i = buf.indexOf("\n")
                            if (i < 0) break
                            val ln = buf.substring(0, i)
                            buf.delete(0, i + 1)
                            handleLine(ln)
                        }
                    }
                    else -> {
                        if (buf.isNotBlank()) handleLine(buf.toString())
                        failure = s
                        break@loop
                    }
                }
            }
            state.aiEnd()
            busy = false
            if (failure.isNotEmpty()) {
                error = failure
                phase = "Stopped"
            } else {
                phase = "Done"
            }
        }
    }

    private suspend fun handleLine(raw: String) {
        val line = raw.trim()
        if (line.isEmpty()) return
        if (line.startsWith("@think")) {
            addThought(line.removePrefix("@think"))
            phase = "Thinking…"
            return
        }
        if (line.startsWith("@fig")) {
            phase = "Drawing…"
            val j = try { JSONObject(line.removePrefix("@fig").trim()) } catch (e: Exception) { return }
            if (state.aiDraw(j)) delay(550L)
            return
        }
        if (line.startsWith("@do")) {
            phase = "Editing…"
            val j = try { JSONObject(line.removePrefix("@do").trim()) } catch (e: Exception) { return }
            if (state.aiDo(j)) delay(500L)
            return
        }
        phase = "Writing…"
        val t = tidy(line.removePrefix("@text"))
        if (t.isEmpty()) return
        lines.add(t)
        state.aiWrite(t)
        delay(300L)
    }
}
