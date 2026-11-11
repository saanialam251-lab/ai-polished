package com.aismartmath.board.board

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Talks to Google Gemini. The API key is typed once by the user and kept only on the phone. */
object GeminiClient {
    private const val PREFS = "smart_math_ai"
    private const val KEY = "gemini_key"
    private val MODELS = listOf("gemini-2.5-flash", "gemini-2.0-flash")

    const val THOUGHT = 0
    const val CHUNK = 1

    /** The keys written in GeminiConfig.kt, empty ones left out. */
    fun geminiKeys(): List<String> = listOf(GeminiConfig.API_KEY_1, GeminiConfig.API_KEY_2).map { it.trim() }.filter { it.isNotEmpty() }

    fun openaiKeys(): List<String> = listOf(GeminiConfig.OPENAI_KEY_1, GeminiConfig.OPENAI_KEY_2).map { it.trim() }.filter { it.isNotEmpty() }

    /** Every key written in GeminiConfig.kt, OpenAI or Gemini. Empty means nothing is set up yet. */
    fun keys(): List<String> = openaiKeys() + geminiKeys()

    private fun systemPrompt(subject: String, hasPicture: Boolean): String {
        val focus = when (subject) {
            "Mathematics" -> "Mathematics (arithmetic, algebra, quadratic equations, polynomials, triangles and geometry, mensuration, trigonometry, limits, derivatives, integrals, functions, graphs, probability, proofs)."
            "Physics" -> "Physics (mechanics, motion, forces, work-energy, waves, optics, heat, electricity, magnetism, modern physics). State the formula, substitute with units, give the final value with its unit."
            "Chemistry" -> "Chemistry (physical, organic, inorganic; mole concept, equations and balancing, equilibrium, thermodynamics, electrochemistry, kinetics, reactions and mechanisms). Show balanced equations and calculations with units."
            "Biology" -> "Biology (cell biology, genetics, physiology, plants, ecology, evolution, human body, diagrams). Explain the process step by step and name every part and term correctly."
            "English" -> "English (grammar, tenses, voice, narration, error spotting, fill in the blanks, sentence correction, comprehension, vocabulary). Give the correct answer, then the rule behind it."
            else -> "any subject at all: Mathematics, Physics, Chemistry, Biology, English and everything else. Work out the subject yourself."
        }
        val pic = if (hasPicture) "\nThe student also sends a PICTURE of part of their board (and a short text description of the selected items). Read the picture carefully first, say what you see in the @think lines, then answer the doubt about it." else ""
        return """You are an expert, patient teacher of $focus$pic
You are a free, general AI tutor, not a template filler. Answer whatever the student really asks, as well as the best human teacher would: solve, prove, derive, explain, compare, translate, correct, check an answer, give another method, draw, plan. Use the exam layout below for problems to solve or prove; for any other request choose the format that teaches best.
The student may type ANY kind of question: MCQ, assertion-reason, reasoning, prove that, solve, find values, limits, derivatives, graphs, short or long answer, numerical, or a doubt.
You answer on a digital whiteboard, so reply ONLY in this line format (one item per line):

@think <one short line about what you are working out>   (write 3 to 8 of these FIRST: what is asked, which method, which formula, what could go wrong)
@fig <one JSON object>   (a drawing for the board; write the lines of the main figure BEFORE the solution text; extra construction lines may come later, just before the line that mentions them)
Then the solution in EXACTLY this layout, one item per line:
Given: (every fact given in the question: lengths, angles, theta, conditions; one "Given:" line per fact)
Then ONE of these lines saying what is asked: "To find: ...", "To prove: ...", "To solve: ...", "To show: ...", "To simplify: ...", "To evaluate: ...", "To balance: ..." (or "To choose: ..." for an MCQ).
If extra lines must be drawn, one line "Construction: ..." (preceded by its @fig lines). Then the line "Solution:" (or "Proof:" for a proof), followed by numbered lines "Step 1: ...", "Step 2: ..." and so on. Each step must say WHAT you do, the formula or theorem used, the substitution with numbers, and the result. In a proof write every statement with its reason in brackets, for example "Step 3: AB = AC [given]" or "Step 4: ∠B = ∠C [angles opposite equal sides]".
Finally one line "Answer: ..." for the result, or "Hence proved." (then the statement) for a proof, or "Hence shown." for a show-that question.
Explain everything in full detail, including every angle, theta, side and value you use. The student wants to follow each step.
If the message contains an "Earlier question", your "Earlier answer" and a new request from the student, do exactly the new request (for example do it again from the start, or explain one step in much more detail), in the same layout.

@fig JSON forms (coordinates are in board units, y points up, and MUST be to scale with the numbers in the question, because the board prints lengths and angles from the coordinates):
{"type":"tri","pts":[[0,0],[6,0],[2,4]],"names":"ABC","show":["sides","angles"]}
{"type":"quad","pts":[[0,0],[5,0],[5,3],[0,3]],"names":"ABCD","show":["sides"]}   ("pent" and "hex" work the same with 5 and 6 points)
{"type":"rect","pts":[[0,0],[5,3]],"names":"ABCD"}   (two opposite corners)
{"type":"seg","pts":[[2,0],[2,4]],"names":"DA"}   (extra lines such as altitudes, medians, bisectors, diagonals, radii)
{"type":"circ","center":[0,0],"r":3}
{"type":"pt","pts":[[1,2]]}
{"type":"label","pts":[[1,2]],"text":"D"}   (a letter or short words placed at a point)
{"type":"graph","eq":"x^2-4x+3"}   (typed like x^2, sin(x), sqrt(x), 1/x, e^x, ln(x), x^2+y^2=25)
{"type":"solid","kind":"cylinder","dims":{"r":2,"h":5}}   (kinds: cube a; cuboid l,w,h; pyr l,w,h; prism l,h,w; cyl r,h; cone r,h; sph r)
Draw a figure whenever the question has geometry, a graph, a diagram, a vector or ray diagram, a circuit sketch idea, or a 3D solid. When a graph is needed, write the @fig graph line FIRST and then the points and labels in the same coordinates. Skip @fig for pure text, English and calculation questions. Draw graphs with @fig graph and mark key points with pt and label.

More @fig forms (use them whenever they fit):
{"type":"unit","deg":30}   (the unit circle with its point at that angle: shows sin, cos, tan)
{"type":"trig","angle":30,"hyp":6}   (the trigonometry triangle: right angle at B, angle at A, hypotenuse)
Anything else can be drawn freely, so you are never limited by the forms above:
{"type":"path","pts":[[0,0],[1,2],[3,3],[5,1]],"closed":false,"color":"blue","dashed":false,"text":"label"}   (a free line through points: any curve, outline, diagram, molecule skeleton, wave, field line, circuit wire; use many points for smooth curves)
{"type":"arc","center":[0,0],"r":1,"from":0,"to":60}   (a circular arc in degrees, for angle marks, compass arcs, wave fronts, orbits)
{"type":"ellipse","center":[0,0],"rx":3,"ry":2,"rot":0}
Any @fig may carry "color": "red", "green", "blue", "yellow", "pink", "orange" or "white" (use colour to separate given parts from constructed parts).
For "graph", "eq" may be a function (x^2-4x+3, sin(x)), an implicit curve (x^2+y^2=25), a parametric curve ((cos(t), sin(t))), a polar curve (r=2sin(theta)) or a derivative ("dy/dx of x^3" draws the function and its slope curve together).
The board's own tools are what draw these, so: triangles and polygons show their lengths and angles when you add "show", segments show lengths, circles show the radius, graphs show axes, and solids can be turned. Use limits, derivatives, functions, quadratics, polynomials, triangles, circles, trigonometry, parallel lines, solids and graphs in the way the question needs.

@do <one JSON object>   (change something that is already on the board, like the student would with the eye, the three dots and the panels; write these AFTER the @fig lines they refer to)
"target" says what to change: a number (0 is the first figure you drew, 1 the second ...), "last" (default), "all", or "sel" (the figures the student selected).
{"op":"set_length","target":0,"side":"AB","length":5}   (changes one side; a line, a circle's radius, a rectangle "w" or "h" work too)
{"op":"set_angle","target":0,"vertex":"B","deg":50}   (changes the angle at a corner of a polygon)
{"op":"set_direction","target":1,"deg":60}   (turns a line to this angle from the x axis)
{"op":"set_names","target":0,"names":"PQR"}
{"op":"show","target":0,"labels":["sides","angles","letters"]}   ("hide" does the opposite; "show_all" and "hide_all" switch every detail)
{"op":"angle_only","target":0,"vertex":"A"}   (show only that one angle)
{"op":"color","target":2,"color":"red"}   {"op":"move","target":0,"dx":2,"dy":0}   {"op":"regular","target":0}   {"op":"delete","target":3}
Parallel lines: draw two parallel "seg" lines and a transversal "seg" crossing both. The app then finds them and numbers the eight angles 1 to 8. {"op":"parallel","mode":"alt"} highlights a pair type: "cor" corresponding, "alt" alternate interior, "ext" alternate exterior, "co" co-interior, "vert" vertically opposite, or "all"; add "angle":3 to highlight one angle.
Graphs: {"op":"param","name":"a","value":2} sets a slider of the last graph (the letters a, b, c can be used in the equation, like a*x^2+b*x+c); {"op":"tangent","x":1} puts the tangent line at x on the last graph; {"op":"unit","deg":45} moves the unit circle's point.
If the student selected things and asks to change them (for example "make angle A 50 degrees"), use "target":"sel" in a @do line instead of drawing again.

CONSTRUCTIONS and DIAGRAMS. When a proof or a solution needs extra lines, draw them with the app's own lines, in orange, at the moment you mention them: write the "@fig" line(s) just before the "Construction:" line, then continue. The board finds the points by their letters, so you do not need coordinates:
{"type":"join","points":"AD"}   (a line between two lettered points)
{"type":"midpoint","of":"BC","name":"D"}   (marks the midpoint and calls it D)
{"type":"perp","from":"A","to":"BC","name":"D"}   (drops the perpendicular from A to line BC, foot D)
{"type":"bisector","at":"A","arms":"BC","name":"D"}   (bisector of angle BAC, meeting BC at D)
{"type":"parallel","through":"E","to":"BC","name":"F"}   (a line through E parallel to BC; add "len" for its length)
{"type":"extend","line":"AB","beyond":"B","by":2,"name":"E"}   (produce line AB beyond B)
{"type":"circum","of":"ABC","name":"O"}   {"type":"incircle","of":"ABC","name":"I"}   (circumcircle and incircle)
{"type":"pt","pts":[[1,2]],"name":"P"}   (a lettered point of your own)
{"type":"arrow","pts":[[0,0],[3,2]],"text":"F"}   (an arrow with a head, for forces, vectors, velocities, reactions, rays)
{"type":"intersect","a":"AB","b":"CD","name":"E"}   (marks where line AB and line CD cross)
{"type":"tangent","at":"P","center":"O","ends":"TS"}   (the tangent to the circle with centre O at the point P on it; "ends" names its two ends)
{"type":"anglemark","at":"B","arms":"AC","text":"60°"}   (a small arc between BA and BC, with a label)   {"type":"rightmark","at":"B","arms":"AC"}   (the small square of a right angle)
A circle may carry "name":"O" to mark and name its centre: {"type":"circ","center":[0,0],"r":3,"name":"O"}. Points on a circle, tangent points, chord ends and so on are lettered points: {"type":"pt","pts":[[x,y]],"name":"P"}, with coordinates computed as centre + r(cos t, sin t).
MIXED FIGURES. A question may mix triangles, quadrilaterals, circles, tangents, chords, parallel lines, graphs, vectors and solids in one figure. Draw all of it, in the order a student would draw it, using as many @fig lines as needed. First work out ALL coordinates in your @think lines (use trigonometry for angles and lengths, check that every given length, angle, tangent, parallel or perpendicular condition really holds for your numbers), and only then write the @fig lines. A cyclic quadrilateral is four lettered points on one circle plus the joining sides; a tangent is perpendicular to the radius at the point of contact; a trigonometry proof on a triangle is a drawn triangle with its lettered sides and angles; a limit question is the graph with lettered or marked points and a small label at the point of discontinuity; a physics question is the diagram (arrows, incline, circuit, rays) and, if asked, the graph of the motion or equation.
Only use a construction that is allowed in the proof, and name every new point in the "Construction:" line (for example "Construction: Draw AD perpendicular to BC").
For PHYSICS draw free-body and ray diagrams with arrows, lines, circles, rectangles and labels; for CHEMISTRY draw rings (hex) and bond lines (seg) when a structure helps, and write reactions as lines of text with \rightarrow; for MATHS draw every figure and graph.
If a PICTURE is attached, first read it exactly (printed or handwritten text, figure, numbers) and write the question you read as one line "Question: ...", then solve it. If the picture holds several questions, solve each one in turn, labelled "Question 1", "Question 2".
Long equations: break them into several short lines, one step of the working per line, so each fits on a phone screen.

Rules for the solution text:
- Complete, step by step, like a model exam answer. Never skip a step. Give a reason in brackets for every step of a proof.
- For MCQ and assertion-reason check every option or statement briefly, then state the correct option.
- Write every formula in proper LaTeX, WITHOUT dollar signs or \( \): \frac{a}{b}, \sqrt{x}, \sqrt[3]{x}, x^{2}, x_{1}, \lim_{x \to 0}, \int_{0}^{1} x\,dx, \sum_{i=1}^{n}, \frac{dy}{dx}, \sin\theta, \pi, \infty, \times, \cdot, \leq, \geq, \neq, \approx, \rightarrow, \Rightarrow, \vec{v}, \overline{AB}, \angle ABC, \triangle ABC, \cong, \parallel, \perp, \begin{pmatrix} a & b \\ c & d \end{pmatrix}, \begin{cases} ... \end{cases}. Chemistry as H_{2}O, SO_{4}^{2-}, \rightarrow, \rightleftharpoons. Degrees as ^{\circ}. Write plain words normally and put only the maths in LaTeX.
- Put one idea on each line. No markdown (no **, ##, backticks, tables). Keep each line short enough to read on a phone.
- Reply in the language of the question."""
    }

    private fun toJpegBase64(b: Bitmap): String {
        val maxSide = 1400
        val s = minOf(1f, maxSide.toFloat() / maxOf(b.width, b.height))
        val bmp = if (s < 1f) Bitmap.createScaledBitmap(b, (b.width * s).toInt().coerceAtLeast(1), (b.height * s).toInt().coerceAtLeast(1), true) else b
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Streams the answer. [onPiece] is called (on a background thread) with THOUGHT for Gemini's own thinking summary and
     * CHUNK for pieces of the answer. Returns null when it finished, or the message to show the student.
     */
    private suspend fun streamGemini(
        subject: String,
        question: String,
        picture: Bitmap?,
        onPiece: (Int, String) -> Unit,
    ): String? = withContext(Dispatchers.IO) {
        val ks = geminiKeys()
        if (ks.isEmpty()) return@withContext "No Gemini key set."
        var last = "Could not reach Gemini."
        val errs = ArrayList<String>()
        keyLoop@ for ((ki, key) in ks.withIndex()) {
            if (key.length < 20 || key.any { it.isWhitespace() || it == '"' || it == '\'' }) {
                errs.add("Key ${ki + 1} looks wrong: it is ${key.length} characters" +
                    (if (key.any { it.isWhitespace() || it == '"' || it == '\'' }) " and contains a space, quote or line break" else " (too short)") +
                    ". Copy the whole key again.")
                continue@keyLoop
            }
            modeLoop@ for (mode in 0..2) {
            for (m in MODELS) {
                var gotData = false
                try {
                    // 0: Gemini API, key in the header. 1: Gemini API, key in the address. 2: Google Cloud (Vertex AI) address, which is where keys that start with AQ. made in Google Cloud work.
                    val ek = java.net.URLEncoder.encode(key, "UTF-8")
                    val url = URL(
                        when (mode) {
                            0 -> "https://generativelanguage.googleapis.com/v1beta/models/$m:streamGenerateContent?alt=sse"
                            1 -> "https://generativelanguage.googleapis.com/v1beta/models/$m:streamGenerateContent?alt=sse&key=$ek"
                            else -> "https://aiplatform.googleapis.com/v1/publishers/google/models/$m:streamGenerateContent?alt=sse&key=$ek"
                        },
                    )
                    val con = url.openConnection() as HttpURLConnection
                    con.requestMethod = "POST"
                    con.connectTimeout = 20000
                    con.readTimeout = 120000
                    con.doOutput = true
                    con.setRequestProperty("Content-Type", "application/json")
                    if (mode == 0) con.setRequestProperty("x-goog-api-key", key)
                    val parts = JSONArray().put(JSONObject().put("text", question))
                    if (picture != null) {
                        parts.put(JSONObject().put("inlineData", JSONObject().put("mimeType", "image/jpeg").put("data", toJpegBase64(picture))))
                    }
                    val gen = JSONObject().put("temperature", 0.3).put("maxOutputTokens", 16384)
                    if (m.startsWith("gemini-2.5")) gen.put("thinkingConfig", JSONObject().put("includeThoughts", true))
                    val body = JSONObject()
                        .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt(subject, picture != null)))))
                        .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
                        .put("generationConfig", gen)
                    con.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    val code = con.responseCode
                    if (code !in 200..299) {
                        val txt = con.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                        val msg = try { JSONObject(txt).getJSONObject("error").optString("message") } catch (e: Exception) { txt.take(160) }
                        val which = "key ${ki + 1}"
                        if (code == 429) {
                            errs.add("Key ${ki + 1}: free limit reached. Wait a minute, or use the other key.")
                            continue@keyLoop
                        }
                        if (code == 400 && msg.contains("API key", ignoreCase = true) || code == 401 || code == 403) {
                            if (mode < 2) continue@modeLoop
                            errs.add("Key ${ki + 1} refused by Google: $msg")
                            continue@keyLoop
                        }
                        last = "Gemini error $code: $msg"
                        continue
                    }
                    con.inputStream.bufferedReader(Charsets.UTF_8).use { r ->
                        while (true) {
                            val line = r.readLine() ?: break
                            if (!line.startsWith("data:")) continue
                            val payload = line.substring(5).trim()
                            if (payload.isEmpty() || payload == "[DONE]") continue
                            val j = try { JSONObject(payload) }
                            val ps = j.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: continue
                            for (i in 0 until ps.length()) {
                                val p = ps.getJSONObject(i)
                                val t = p.optString("text")
                                if (t.isEmpty()) continue
                                gotData = true
                                onPiece(if (p.optBoolean("thought", false)) THOUGHT else CHUNK, t)
                            }
                        }
                    }
                    if (gotData) return@withContext null
                    last = "Gemini sent an empty answer. Try rewording the question."
                } catch (e: java.net.UnknownHostException) {
                    return@withContext "No internet connection."
                } catch (e: Exception) {
                    if (gotData) return@withContext "The connection broke while answering: " + (e.message ?: "")
                    last = "Could not reach Gemini: " + (e.message ?: e.javaClass.simpleName)
                }
            }
            break@modeLoop
            }
        }
        if (errs.isNotEmpty() && last == "Could not reach Gemini.") errs.joinToString("\n") + "\nCheck the keys in GeminiConfig.kt." else last
    }
 
    private val OPENAI_BASE = listOf("gpt-4.1", "gpt-4o", "gpt-4o-mini")
 
    /** Newest chat models this key may use, found by asking OpenAI. Used when the usual model names are refused. */
    private fun discoverOpenAiModels(key: String): List<String> {
        return try {
            val con = URL("https://api.openai.com/v1/models").openConnection() as HttpURLConnection
            con.connectTimeout = 20000
            con.readTimeout = 30000
            con.setRequestProperty("Authorization", "Bearer $key")
            if (con.responseCode !in 200..299) return emptyList()
            val arr = JSONObject(con.inputStream.bufferedReader().use { it.readText() }).optJSONArray("data") ?: return emptyList()
            val bad = listOf("audio", "realtime", "image", "tts", "transcribe", "search", "instruct", "embed", "moderation", "whisper", "dall", "sora", "codex", "deep-research", "computer")
            val found = ArrayList<Pair<Long, String>>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id")
                if (id.startsWith("gpt-") && bad.none { id.contains(it) }) found.add(o.optLong("created", 0L) to id)
            }
            found.sortedByDescending { it.first }.map { it.second }.take(4)
        } catch (e: Exception) {
            emptyList()
        }
    }
 
    /** OpenAI (ChatGPT) keys. Returns null when an answer was streamed, otherwise what went wrong. */
    private fun streamOpenAI(subject: String, question: String, picture: Bitmap?, onPiece: (Int, String) -> Unit): String? {
        val errs = ArrayList<String>()
        keyLoop@ for ((ki, key) in openaiKeys().withIndex()) {
            if (key.length < 20 || key.any { it.isWhitespace() || it == '"' || it == '\'' }) {
                errs.add("OpenAI key ${ki + 1} looks wrong (${key.length} characters; spaces, quotes and line breaks are not allowed). Copy the whole key again.")
                continue@keyLoop
            }
            val chosen = GeminiConfig.OPENAI_MODEL.trim()
            var models = if (chosen.isNotEmpty()) listOf(chosen) + OPENAI_BASE else OPENAI_BASE
            var discovered = false
            while (true) {
                var modelProblem = ""
                for (m in models) {
                    var got = false
                    try {
                        val con = URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection
                        con.requestMethod = "POST"
                        con.connectTimeout = 20000
                        con.readTimeout = 120000
                        con.doOutput = true
                        con.setRequestProperty("Content-Type", "application/json")
                        con.setRequestProperty("Authorization", "Bearer $key")
                        val user: Any = if (picture == null) question else JSONArray()
                            .put(JSONObject().put("type", "text").put("text", question))
                            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64," + toJpegBase64(picture))))
                        val body = JSONObject()
                            .put("model", m)
                            .put("stream", true)
                            .put("max_completion_tokens", 16000)
                            .put(
                                "messages",
                                JSONArray()
                                    .put(JSONObject().put("role", "system").put("content", systemPrompt(subject, picture != null)))
                                    .put(JSONObject().put("role", "user").put("content", user)),
                            )
                        con.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                        val code = con.responseCode
                        if (code !in 200..299) {
                            val txt = con.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                            val msg = try { JSONObject(txt).getJSONObject("error").optString("message") } catch (e: Exception) { txt.take(160) }
                            if (code == 401) {
                                errs.add("OpenAI key ${ki + 1} refused: $msg")
                                continue@keyLoop
                            }
                            if (code == 429) {
                                errs.add(
                                    "OpenAI key ${ki + 1}: " +
                                        (if (txt.contains("insufficient_quota")) "no credit left on this account (add billing at platform.openai.com)." else "too many requests, wait a minute."),
                                )
                                continue@keyLoop
                            }
                            if (code == 400 || code == 403 || code == 404) {
                                modelProblem = "$m: $msg"
                                continue
                            }
                            errs.add("OpenAI error $code: $msg")
                            continue
                        }
                        con.inputStream.bufferedReader(Charsets.UTF_8).use { r ->
                            while (true) {
                                val line = r.readLine() ?: break
                                if (!line.startsWith("data:")) continue
                                val payload = line.substring(5).trim()
                                if (payload.isEmpty() || payload == "[DONE]") continue
                                val j = try { JSONObject(payload) } catch (e: Exception) { continue }
                                val delta = j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta") ?: continue
                                if (delta.isNull("content")) continue
                                val t = delta.optString("content", "")
                                if (t.isEmpty()) continue
                                got = true
                                onPiece(CHUNK, t)
                            }
                        }
                        if (got) return null
                        errs.add("OpenAI sent an empty answer.")
                    } catch (e: java.net.UnknownHostException) {
                        return "No internet connection."
                    } catch (e: Exception) {
                        if (got) return "The connection broke while answering: " + (e.message ?: "")
                        errs.add("Could not reach OpenAI: " + (e.message ?: e.javaClass.simpleName))
                    }
                }
                if (!discovered && modelProblem.isNotEmpty()) {
                    discovered = true
                    val d = discoverOpenAiModels(key).filter { it !in models }
                    if (d.isNotEmpty()) {
                        models = d
                        continue
                    }
                }
                if (modelProblem.isNotEmpty()) errs.add("OpenAI key ${ki + 1}: no model worked ($modelProblem). Set OPENAI_MODEL in GeminiConfig.kt to a model your account can use.")
                continue@keyLoop
            }
        }
        return errs.joinToString("\n").ifEmpty { "OpenAI failed." }
    }
 
    /** Tries OpenAI (ChatGPT) keys first when there are any, then Gemini keys. */
    suspend fun stream(
        subject: String,
        question: String,
        picture: Bitmap?,
        onPiece: (Int, String) -> Unit,
    ): String? = withContext(Dispatchers.IO) {
        if (keys().isEmpty()) return@withContext "No API key yet. Open GeminiConfig.kt and put your key in OPENAI_KEY_1 (ChatGPT) or API_KEY_1 (Gemini)."
        var any = false
        val wrap: (Int, String) -> Unit = { k, t ->
            any = true
            onPiece(k, t)
        }
        val errs = ArrayList<String>()
        if (openaiKeys().isNotEmpty()) {
            val r = streamOpenAI(subject, question, picture, wrap) ?: return@withContext null
            if (any) return@withContext r
            errs.add(r)
        }
        if (geminiKeys().isNotEmpty()) {
            val r = streamGemini(subject, question, picture, wrap) ?: return@withContext null
            errs.add(r)
        }
        errs.joinToString("\n")
    }
}
 
                            
