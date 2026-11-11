package com.aismartmath.board

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aismartmath.board.ui.SmartMathApp
import com.aismartmath.board.ui.theme.BoardColors
import com.aismartmath.board.ui.theme.SmartMathTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.aismartmath.board.data.ImageStore.init(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            SmartMathTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BoardColors.Bg,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ) {
                    SmartMathApp()
                }
            }
        }
    }
}
