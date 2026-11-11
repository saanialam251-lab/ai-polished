package com.aismartmath.board.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream

/** Pictures on the board are kept as files in the app's storage; a board only remembers each picture's key. */
object ImageStore {
    private var dir: File? = null
    private val cache = HashMap<String, ImageBitmap>()

    fun init(ctx: Context) {
        dir = File(ctx.filesDir, "images").also { it.mkdirs() }
    }

    /** Saves a picture and returns its key. */
    fun put(bmp: Bitmap): String {
        val key = "i" + System.currentTimeMillis() + "_" + (System.nanoTime() % 100000)
        try {
            val f = File(dir, key)
            FileOutputStream(f).use { out ->
                if (bmp.hasAlpha()) bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                else bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
        } catch (e: Exception) {
            // the picture still shows this session even if it could not be written
        }
        cache[key] = bmp.asImageBitmap()
        return key
    }

    fun get(key: String): ImageBitmap? {
        cache[key]?.let { return it }
        return try {
            val f = File(dir, key)
            if (!f.exists()) return null
            val b = BitmapFactory.decodeFile(f.absolutePath) ?: return null
            val ib = b.asImageBitmap()
            cache[key] = ib
            ib
        } catch (e: Exception) {
            null
        }
    }

    /** Reads a picture from a content Uri, shrinking big photos so the board stays quick. */
    fun decode(ctx: Context, uri: Uri, maxSide: Int = 1600): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            var w = bounds.outWidth
            var h = bounds.outHeight
            if (w <= 0 || h <= 0) return null
            while (w / 2 >= maxSide || h / 2 >= maxSide) {
                sample *= 2
                w /= 2
                h /= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val rot = try {
                ctx.contentResolver.openInputStream(uri)?.use { ins ->
                    when (android.media.ExifInterface(ins).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                        6 -> 90f
                        3 -> 180f
                        8 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (e: Exception) {
                0f
            }
            if (rot == 0f) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rot) }, true)
        } catch (e: Exception) {
            null
        }
    }
}
