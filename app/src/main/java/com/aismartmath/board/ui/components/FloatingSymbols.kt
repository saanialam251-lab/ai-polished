package com.aismartmath.board.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Soft maths symbols drifting in the background. */
@Composable
fun FloatingSymbols(modifier: Modifier = Modifier) {
    val symbols = listOf("∠", "π", "√", "∑", "∫", "△", "x²", "θ", "∞", "°")
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        symbols.forEachIndexed { i, s ->
            val t = rememberInfiniteTransition(label = "sym$i")
            val drift by t.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(6000 + i * 700, easing = LinearEasing),
                    RepeatMode.Reverse,
                ),
                label = "drift$i",
            )
            val xf = ((i * 37) % 100) / 100f
            val yf = ((i * 53 + 11) % 100) / 100f
            Text(
                text = s,
                color = Color.White.copy(alpha = 0.05f + 0.05f * drift),
                fontSize = (28 + (i % 3) * 10).sp,
                modifier = Modifier.blur((if (i % 3 == 0) 3 else 0).dp).offset {
                    IntOffset((xf * wPx * 0.85f).toInt(), (yf * hPx * 0.85f - drift * 40f).toInt())
                },
            )
        }
    }
}
