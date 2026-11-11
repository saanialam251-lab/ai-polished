package com.aismartmath.board.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.aismartmath.board.math.hasMath
import com.aismartmath.board.math.parseRich

/**
 * A pill button whose border and fill animate to the accent colour when it is on.
 * [onLong] is the phone's hover tooltip. [small] makes it more compact.
 */
@Composable
fun Chip(
    label: String,
    on: Boolean,
    accent: Color,
    onLong: (() -> Unit)? = null,
    small: Boolean = false,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val clickNow by rememberUpdatedState(onClick)
    val longNow by rememberUpdatedState(onLong)
    val sc by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "chipPress",
    )
    val bg by animateColorAsState(if (pressed) accent.copy(alpha = 0.75f) else if (on) accent.copy(alpha = 0.45f) else Color(0xFF3B271B), tween(200), label = "chipBg")
    val bd by animateColorAsState(if (on || pressed) accent else Color(0xFF6B4630), tween(160), label = "chipBd")
    val shape = RoundedCornerShape(if (small) 9.dp else 11.dp)
    Box(
        Modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .shadow(if (pressed) 1.dp else 4.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, bd), shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        clickNow()
                    },
                    onLongPress = {
                        val l = longNow
                        if (l != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            l()
                        }
                    },
                )
            }
            .padding(horizontal = if (small) 6.dp else 8.dp, vertical = if (small) 2.dp else 3.dp),
    ) {
        if (hasMath(label)) {
            val node = remember(label) { parseRich(label) }
            MathView(node, if (small) 11.sp else 12.sp, Color.White)
        } else Text(label, color = Color.White, fontSize = if (small) 9.sp else 10.sp)
    }
}
