package com.aismartmath.board.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

object BoardColors {
    val Bg = Color(0xFF1A110C)
    val Glow = Color(0xFF4A2E1C)
    val Indigo = Color(0xFFD08A4E) // copper accent (name kept so every panel picks it up)
    val Teal = Color(0xFFE8C38A)
    val Muted = Color(0xFFB8A18E)
    val Panel = Color(0xFF2A1B13)
    val PanelHi = Color(0xFF3B271B)
    val Edge = Color(0xFF5C3D28)
    val Shadow = Color(0xFF000000)
}

@Composable
fun SmartMathTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = BoardColors.Indigo,
            background = BoardColors.Bg,
            surface = BoardColors.Bg,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
    ) {
        // every text gets a little air between the letters - a calmer, more premium look
        ProvideTextStyle(TextStyle(letterSpacing = 0.3.sp), content)
    }
}

/** Board backgrounds (name, colour) - the "paper" the maths is drawn on. */
val BoardBackgrounds = listOf(
    "Walnut" to Color(0xFF1A110C),
    "Blackboard" to Color(0xFF10291C),
    "Espresso" to Color(0xFF241812),
    "Eye-care" to Color(0xFF2B2418),
)
