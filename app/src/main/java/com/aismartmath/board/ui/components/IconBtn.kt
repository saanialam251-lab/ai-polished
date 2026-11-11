package com.aismartmath.board.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Web: .ib button (42 x 38, rounded 10). Squashes when pressed and gives a light tick.
 * [onLongClick] is the phone's version of a hover tooltip. [enabled] = false greys it out; [dim] only fades it.
 */
@Composable
fun IconBtn(
    label: String,
    modifier: Modifier = Modifier,
    floating: Boolean = false,
    enabled: Boolean = true,
    dim: Boolean = false,
    compact: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val clickNow by rememberUpdatedState(onClick)
    val longNow by rememberUpdatedState(onLongClick)
    val enabledNow by rememberUpdatedState(enabled)
    val sc by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "ibPress",
    )
    val fade by animateFloatAsState(if (!enabled) 0.3f else if (dim) 0.5f else 1f, label = "ibFade")
    val shape = RoundedCornerShape(9.dp)
    val base = if (floating) Color(0xE62A1B13) else Color(0xFF3B271B)
    val fill by animateColorAsState(if (pressed) Color(0xFFD08A4E) else base, tween(120), label = "ibFill")
    val edge by animateColorAsState(if (pressed) Color(0xFFE8C38A) else Color(0xFF6B4630), tween(120), label = "ibEdge")
    Box(
        modifier
            .size(width = if (compact) 26.dp else 29.dp, height = 26.dp)
            .graphicsLayer { scaleX = sc; scaleY = sc; alpha = fade }
            .shadow(if (pressed) 1.dp else 5.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, edge), shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        if (enabledNow) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            clickNow()
                        }
                    },
                    onLongPress = {
                        val l = longNow
                        if (enabledNow && l != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            l()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}
