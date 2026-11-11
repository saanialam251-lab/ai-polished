package com.aismartmath.board.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** The short description of a saved board (read without opening the whole board). */
class BoardMeta(
    val id: String,
    val name: String,
    val topicId: String,
    val updated: Long,
    val count: Int,
    /** Id of the folder the board is in; "" = the main list. */
    val folder: String = "",
)

/** A folder of boards (one level: folders hold boards, not other folders). */
class Folder(val id: String, val name: String)

/**
 * Boards are saved as JSON files in the app's own storage:
 * `<id>.json` holds the drawing, `<id>.meta` holds the name and date so the list opens fast.
 */
class BoardStore(ctx: Context) {
    private val dir: File = File(ctx.filesDir, "boards").also { it.mkdirs() }

    private fun dataFile(id: String) = File(dir, "$id.json")
    private fun metaFile(id: String) = File(dir, "$id.meta")

    fun newId(): String = "b" + System.currentTimeMillis()

    private fun readMeta(f: File): BoardMeta? = try {
        val o = JSONObject(f.readText())
        BoardMeta(o.getString("id"), o.getString("name"), o.getString("topic"), o.getLong("updated"), o.optInt("count", 0), o.optString("folder", ""))
    } catch (e: Exception) {
        null
    }

    private fun writeMeta(id: String, name: String, topicId: String, count: Int, folder: String, updated: Long = System.currentTimeMillis()) {
        val o = JSONObject()
        o.put("id", id)
        o.put("name", name)
        o.put("topic", topicId)
        o.put("updated", updated)
        o.put("count", count)
        o.put("folder", folder)
        metaFile(id).writeText(o.toString())
    }

    fun list(): List<BoardMeta> {
        val files = dir.listFiles()?.filter { it.name.endsWith(".meta") } ?: emptyList()
        return files.mapNotNull { readMeta(it) }.sortedByDescending { it.updated }
    }

    fun meta(id: String): BoardMeta? {
        val f = metaFile(id)
        return if (f.exists()) readMeta(f) else null
    }

    fun save(id: String, name: String, topicId: String, count: Int, data: String) {
        dataFile(id).writeText(data)
        // a board keeps its folder when it is saved again
        writeMeta(id, name, topicId, count, meta(id)?.folder ?: "")
    }

    fun load(id: String): JSONObject? = try {
        val f = dataFile(id)
        if (f.exists()) JSONObject(f.readText()) else null
    } catch (e: Exception) {
        null
    }

    fun rename(id: String, name: String) {
        val m = meta(id) ?: return
        writeMeta(id, name, m.topicId, m.count, m.folder)
    }

    fun delete(id: String) {
        dataFile(id).delete()
        metaFile(id).delete()
    }

    /** Deletes many boards at once; returns how many were removed. */
    fun deleteAll(ids: Collection<String>): Int {
        ids.forEach { delete(it) }
        return ids.size
    }

    /** Puts boards into a folder ("" = back to the main list). Their dates stay as they were. */
    fun move(ids: Collection<String>, folder: String) {
        for (id in ids) {
            val m = meta(id) ?: continue
            writeMeta(id, m.name, m.topicId, m.count, folder, m.updated)
        }
    }

    // ---- folders ----
    private fun foldersFile() = File(dir, "folders.json")

    fun folders(): List<Folder> = try {
        val f = foldersFile()
        if (!f.exists()) emptyList()
        else {
            val a = JSONArray(f.readText())
            (0 until a.length()).map { Folder(a.getJSONObject(it).getString("id"), a.getJSONObject(it).getString("name")) }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun writeFolders(l: List<Folder>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name)) }
        foldersFile().writeText(a.toString())
    }

    fun newFolder(name: String): Folder {
        val f = Folder("f" + System.currentTimeMillis(), name)
        writeFolders(folders() + f)
        return f
    }

    fun renameFolder(id: String, name: String) {
        writeFolders(folders().map { if (it.id == id) Folder(id, name) else it })
    }

    /** Removes a folder. Its boards are deleted too when [withBoards], otherwise they go back to the main list. */
    fun deleteFolder(id: String, withBoards: Boolean) {
        val inside = list().filter { it.folder == id }.map { it.id }
        if (withBoards) deleteAll(inside) else move(inside, "")
        writeFolders(folders().filter { it.id != id })
    }

    /** Copies a board; returns the new board's id. */
    fun duplicate(id: String): String? {
        val m = meta(id) ?: return null
        val src = dataFile(id)
        if (!src.exists()) return null
        val nid = newId()
        src.copyTo(dataFile(nid), overwrite = true)
        writeMeta(nid, m.name + " copy", m.topicId, m.count, m.folder)
        return nid
    }
}
