package com.aismartmath.board.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.aismartmath.board.board.BoardScreen
import com.aismartmath.board.data.BoardStore
import com.aismartmath.board.data.Prefs
import com.aismartmath.board.model.Topics
import com.aismartmath.board.ui.boards.BoardsScreen
import com.aismartmath.board.ui.components.FloatingSymbols
import com.aismartmath.board.ui.components.SparkleHost
import com.aismartmath.board.ui.home.HomeScreen
import com.aismartmath.board.ui.splash.SplashScreen
import com.aismartmath.board.ui.theme.BoardBackgrounds
import com.aismartmath.board.ui.theme.BoardColors
import kotlin.math.abs

private val UI_SCALES = listOf(0.75f, 0.85f, 1f, 1.15f, 1.3f, 1.5f)

private fun nextScale(s: Float): Float {
    val i = UI_SCALES.indexOfFirst { abs(it - s) < 0.01f }
    return UI_SCALES[(i + 1) % UI_SCALES.size]
}

@Composable
fun SmartMathApp() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val store = remember { BoardStore(ctx) }
    val base = LocalDensity.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    var scale by remember {
        mutableFloatStateOf(if (prefs.uiScale > 0f) prefs.uiScale else if (widthDp >= 600) 1f else 0.85f)
    }
    var bgIndex by remember { mutableIntStateOf(prefs.bg.coerceIn(0, BoardBackgrounds.size - 1)) }
    var sparkles by remember { mutableStateOf(prefs.sparkles) }
    var splashDone by rememberSaveable { mutableStateOf(false) }
    var nav by rememberSaveable { mutableStateOf<String?>(null) } // "topic|board" while a board is open
    var showBoards by rememberSaveable { mutableStateOf(false) }

    // the interface size setting scales every dp and sp, so touch targets grow on big panels
    CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
        BackHandler(enabled = nav != null || showBoards) {
            if (nav != null) nav = null else showBoards = false
        }

        SparkleHost(sparkles, Modifier.fillMaxSize().background(BoardColors.Bg)) {
            if (nav == null) FloatingSymbols()

            AnimatedContent(
                targetState = nav,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally(tween(350)) { it } + fadeIn(tween(350))) togetherWith
                            (slideOutHorizontally(tween(350)) { -it / 4 } + fadeOut(tween(250)))
                    } else {
                        (slideInHorizontally(tween(350)) { -it / 4 } + fadeIn(tween(350))) togetherWith
                            (slideOutHorizontally(tween(350)) { it } + fadeOut(tween(250)))
                    }
                },
                label = "screen",
            ) { key ->
                val parts = key?.split("|")
                val topic = parts?.let { Topics.byId[it[0]] }
                if (key == null || parts == null || topic == null) {
                    HomeScreen(
                        ready = splashDone,
                        sparkles = sparkles,
                        onOpen = { t -> nav = t.id + "|" + store.newId() },
                        onBoards = { showBoards = true },
                        onSparkles = {
                            sparkles = !sparkles
                            prefs.sparkles = sparkles
                        },
                        onSize = {
                            scale = nextScale(scale)
                            prefs.uiScale = scale
                        },
                    )
                } else {
                    BoardScreen(
                        topic = topic,
                        boardId = parts[1],
                        store = store,
                        bg = BoardBackgrounds[bgIndex].second,
                        bgName = BoardBackgrounds[bgIndex].first,
                        onCycleBg = {
                            bgIndex = (bgIndex + 1) % BoardBackgrounds.size
                            prefs.bg = bgIndex
                        },
                        onBack = { nav = null },
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showBoards && nav == null,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200)),
            ) {
                BoardsScreen(
                    store = store,
                    onOpen = { b ->
                        nav = b.topicId + "|" + b.id
                        showBoards = false
                    },
                    onClose = { showBoards = false },
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !splashDone,
                enter = EnterTransition.None,
                exit = fadeOut(tween(550)) + scaleOut(targetScale = 1.15f, animationSpec = tween(550, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
            ) {
                SplashScreen(onFinished = { splashDone = true })
            }
        }
    }
}
