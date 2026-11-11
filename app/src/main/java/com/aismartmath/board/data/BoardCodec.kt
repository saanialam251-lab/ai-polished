package com.aismartmath.board.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.aismartmath.board.board.BoardState
import com.aismartmath.board.board.Layer
import com.aismartmath.board.math.parseEq
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.GraphDef
import com.aismartmath.board.model.Pt
import org.json.JSONArray
import org.json.JSONObject

/** Turns a board into JSON text and back. Only finished (not fading) figures are saved. */
object BoardCodec {
    private fun fin(d: Double): Double = if (d.isFinite()) d else 0.0

    private fun pts(a: List<Pt>): JSONArray {
        val arr = JSONArray()
        for (q in a) arr.put(JSONArray().put(fin(q.x)).put(fin(q.y)))
        return arr
    }

    private fun readPts(a: JSONArray?): List<Pt> {
        if (a == null) return emptyList()
        val out = ArrayList<Pt>()
        for (i in 0 until a.length()) {
            val q = a.getJSONArray(i)
            out.add(Pt(q.getDouble(0), q.getDouble(1)))
        }
        return out
    }

    private fun strings(a: JSONArray?): Set<String> {
        if (a == null) return emptySet()
        val out = HashSet<String>()
        for (i in 0 until a.length()) out.add(a.getString(i))
        return out
    }

    /** [includeCamera] = false gives a text that only changes when the drawing changes (for spotting edits). */
    fun encode(st: BoardState, includeCamera: Boolean = true): String {
        val alive = st.shapes.filter { it.dieAt == 0L }
        val root = JSONObject()
        root.put("v", 1)
        val cam = JSONObject()
        cam.put("z", fin(st.camera.z))
        cam.put("ox", fin(st.camera.ox))
        cam.put("oy", fin(st.camera.oy))
        if (includeCamera) root.put("camera", cam)
        root.put("active", st.activeLayer)
        val la = JSONArray()
        for (l in st.layers) {
            val o = JSONObject()
            o.put("id", l.id)
            o.put("name", l.name)
            o.put("visible", l.visible)
            o.put("locked", l.locked)
            o.put("opacity", l.opacity.toDouble())
            la.put(o)
        }
        root.put("layers", la)
        val arr = JSONArray()
        for (f in alive) arr.put(encodeFig(f, alive))
        root.put("shapes", arr)
        return root.toString()
    }

    private fun encodeFig(f: Fig, alive: List<Fig>): JSONObject {
        val o = JSONObject()
        o.put("t", f.t)
        o.put("p", pts(f.p))
        f.nm?.let { o.put("nm", it) }
        f.col?.let { o.put("col", it.toArgb()) }
        o.put("sz", fin(f.sz))
        o.put("rot", fin(f.rot))
        o.put("layer", f.layer)
        o.put("ov", JSONArray(f.ov.toList()))
        if (f.hide.isNotEmpty()) o.put("hide", JSONArray(f.hide.toList()))
        if (f.only >= 0) o.put("only", f.only)
        o.put("ga", fin(f.ga))
        o.put("gb", fin(f.gb))
        o.put("gc", fin(f.gc))
        if (f.t == "sol") {
            o.put("sk", f.sk)
            val sd = JSONObject()
            for ((k, v) in f.sd) sd.put(k, fin(v))
            o.put("sd", sd)
            o.put("rx", fin(f.rx))
            o.put("ry", fin(f.ry))
            o.put("poly", pts(f.poly))
            o.put("sf", JSONArray(f.sflags.toList()))
        }
        if (f.t == "ink") {
            o.put("inkW", fin(f.inkW))
            o.put("hl", f.hl)
            o.put("ist", f.inkStyle)
        }
        if (f.t == "txt") {
            o.put("txt", f.txt)
            o.put("fsz", fin(f.fsz))
        }
        if (f.t == "img") {
            o.put("img", f.imgKey)
            o.put("crop", JSONArray(f.crop.map { it.toDouble() }))
        }
        if (f.stash.isNotEmpty()) o.put("stash", JSONArray(f.stash.toList()))
        val g = f.gd
        if (g != null) {
            val gj = JSONObject()
            gj.put("src", g.src)
            gj.put("dv", g.dv)
            gj.put("base", if (g.base != null) alive.indexOf(g.base) else -1)
            o.put("gd", gj)
        }
        return o
    }

    private fun decodeFig(o: JSONObject): Fig? {
        val t = o.getString("t")
        val f = Fig(t, readPts(o.optJSONArray("p")), 0L, if (o.has("nm")) o.getString("nm") else null)
        if (o.has("col")) f.col = Color(o.getInt("col"))
        f.sz = o.optDouble("sz", 1.0)
        f.rot = o.optDouble("rot", 0.0)
        f.layer = o.optInt("layer", 0)
        f.ov = strings(o.optJSONArray("ov"))
        f.hide = strings(o.optJSONArray("hide"))
        f.only = o.optInt("only", -1)
        f.ga = o.optDouble("ga", 1.0)
        f.gb = o.optDouble("gb", 0.0)
        f.gc = o.optDouble("gc", 0.0)
        if (t == "sol") {
            f.sk = o.optString("sk", "cube")
            val sd = o.optJSONObject("sd")
            val m = HashMap<String, Double>()
            if (sd != null) {
                val keys = sd.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    m[k] = sd.getDouble(k)
                }
            }
            f.sd = m
            f.rx = o.optDouble("rx", -0.55)
            f.ry = o.optDouble("ry", 0.6)
            f.poly = readPts(o.optJSONArray("poly"))
            f.sflags = strings(o.optJSONArray("sf"))
        }
        if (t == "ink") {
            f.inkW = o.optDouble("inkW", 4.0)
            f.hl = o.optBoolean("hl", false)
            f.inkStyle = o.optInt("ist", 0)
        }
        if (t == "txt") {
            f.txt = o.optString("txt", "")
            f.fsz = o.optDouble("fsz", 0.6)
        }
        if (t == "img") {
            f.imgKey = o.optString("img", "")
            val c = o.optJSONArray("crop")
            if (c != null && c.length() == 4) f.crop = List(4) { c.getDouble(it).toFloat() }
        }
        f.stash = strings(o.optJSONArray("stash"))
        val g = o.optJSONObject("gd")
        if (g != null && g.optInt("dv", 0) == 0) {
            val src = g.getString("src")
            val r = parseEq(src) ?: return null
            f.gd = GraphDef(src, r.fn, r.imp, 0, null, r.par)
        }
        return f
    }

    /** Fills [st] from saved JSON. Finished figures appear at once (no pop-in). */
    fun decode(st: BoardState, root: JSONObject) {
        val cam = root.optJSONObject("camera")
        if (cam != null) {
            st.camera.z = cam.optDouble("z", 1.0)
            st.camera.ox = cam.optDouble("ox", 0.0)
            st.camera.oy = cam.optDouble("oy", 0.0)
        }
        val la = root.optJSONArray("layers")
        if (la != null && la.length() > 0) {
            st.layers.clear()
            for (i in 0 until la.length()) {
                val l = la.getJSONObject(i)
                val layer = Layer(l.getInt("id"), l.optString("name", "Layer"))
                layer.visible = l.optBoolean("visible", true)
                layer.locked = l.optBoolean("locked", false)
                layer.opacity = l.optDouble("opacity", 1.0).toFloat()
                st.layers.add(layer)
            }
            st.resetLayerCounter()
        }
        st.activeLayer = root.optInt("active", st.layers.first().id)
        val arr = root.optJSONArray("shapes") ?: return
        val made = ArrayList<Fig?>()
        for (i in 0 until arr.length()) made.add(decodeFig(arr.getJSONObject(i)))
        // derivative curves need the graph they differentiate
        for (i in 0 until arr.length()) {
            val f = made[i] ?: continue
            val g = arr.getJSONObject(i).optJSONObject("gd") ?: continue
            if (g.optInt("dv", 0) > 0) {
                val b = made.getOrNull(g.optInt("base", -1))
                if (b == null) made[i] = null else f.gd = GraphDef(g.getString("src"), null, false, g.getInt("dv"), b)
            }
        }
        for (f in made) if (f != null) st.shapes.add(f)
        st.clock = System.nanoTime()
    }
}
