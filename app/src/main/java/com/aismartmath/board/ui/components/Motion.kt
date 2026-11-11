package com.aismartmath.board.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * A small, cheap "pop" used all over the app: the content grows from 70 % with a soft spring and fades in.
 * [index] staggers neighbours (35 ms apart, at most 12 steps) so lists and button columns ripple in instead of
 * appearing all at once. It only animates alpha and scale in a graphics layer, so it is light on a phone.
 */
@Composable
fun PopIn(index: Int = 0, modifier: Modifier = Modifier, fromBelow: Boolean = false, content: @Composable () -> Unit) {
    val k = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(minOf(index, 12) * 35L)
        k.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
    }
    Box(
        modifier.graphicsLayer {
            val v = k.value
            alpha = v.coerceIn(0f, 1f)
            val s = 0.7f + 0.3f * v
            scaleX = s
            scaleY = s
            if (fromBelow) translationY = (1f - v) * 24f
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        },
    ) { content() }
}
