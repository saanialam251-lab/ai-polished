package com.aismartmath.board.data

import android.content.Context

/** Small settings kept on the device: interface size and board background. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("smart_math_board", Context.MODE_PRIVATE)

    /** 0 means "not chosen yet" - the app then picks a size from the screen. */
    var uiScale: Float
        get() = sp.getFloat("ui_scale2", 0f)
        set(v) {
            sp.edit().putFloat("ui_scale2", v).apply()
        }

    var sparkles: Boolean
        get() = sp.getBoolean("sparkles", true)
        set(v) {
            sp.edit().putBoolean("sparkles", v).apply()
        }

    var bg: Int
        get() = sp.getInt("board_bg", 0)
        set(v) {
            sp.edit().putInt("board_bg", v).apply()
        }
}
