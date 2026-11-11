package com.aismartmath.board.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The panel look used by every box: a warm brown wood-like gradient, a bevelled edge
 * (light on top, dark at the bottom) and a layered box shadow - a dark one for depth
 * plus a soft one in the accent colour.
 */
fun Modifier.glass(shape: Shape, accent: Color = Color(0xFFD08A4E), elevation: Dp = 10.dp): Modifier = this
    .shadow(elevation + 4.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
    .shadow(elevation, shape, clip = false, ambientColor = accent, spotColor = accent)
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Color(0xF53B271B), Color(0xF5201410))))
    .border(
        BorderStroke(
            1.5.dp,
            Brush.verticalGradient(
                listOf(Color(0xFFE8C38A).copy(alpha = 0.55f), accent.copy(alpha = 0.35f), Color(0xFF000000).copy(alpha = 0.55f)),
            ),
        ),
        shape,
    )

/** A smaller raised "button" slab used for tiles inside panels. */
fun Modifier.slab(shape: Shape, fill: Color, edge: Color, elevation: Dp = 4.dp): Modifier = this
    .shadow(elevation, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
    .clip(shape)
    .background(Brush.verticalGradient(listOf(fill.copy(alpha = (fill.alpha + 0.12f).coerceAtMost(1f)), fill)))
    .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), edge))), shape)
