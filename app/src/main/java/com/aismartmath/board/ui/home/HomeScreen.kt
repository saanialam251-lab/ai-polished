package com.aismartmath.board.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.model.Topic
import com.aismartmath.board.model.Topics
import com.aismartmath.board.ui.components.IconBtn
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    ready: Boolean,
    sparkles: Boolean,
    onOpen: (Topic) -> Unit,
    onBoards: () -> Unit,
    onSize: () -> Unit,
    onSparkles: () -> Unit,
) {
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    val cols = if (tablet) 3 else 2
    var help by remember { androidx.compose.runtime.mutableStateOf(false) }
    if (help) com.aismartmath.board.ui.components.HelpDialog("home") { help = false }
    val header = remember { Animatable(0f) }
    LaunchedEffect(ready) { if (ready) header.animateTo(1f, tween(600)) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 14.dp, start = 4.dp)
                .graphicsLayer {
                    alpha = header.value
                    translationY = (1f - header.value) * -20.dp.toPx()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Choose a topic",
                color = Color(0xFFF1D3A5),
                fontSize = if (tablet) 26.sp else 20.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconBtn("✨", compact = true, dim = !sparkles) { onSparkles() }
            IconBtn("📂", compact = true) { onBoards() }
            IconBtn("?", compact = true) { help = true }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        ) {
            val last = Topics.all.lastIndex
            itemsIndexed(
                Topics.all,
                span = { i, _ -> GridItemSpan(if (i == last) 2 else 1) },
            ) { i, topic ->
                TopicCard(topic, i, ready, wide = i == last, tablet = tablet) { onOpen(topic) }
            }
        }
    }
}

@Composable
private fun TopicCard(topic: Topic, index: Int, ready: Boolean, wide: Boolean, tablet: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)

    // entrance: springy pop-in, one card after another
    val enter = remember { Animatable(0f) }
    LaunchedEffect(ready) {
        if (ready) {
            delay(100L + index * 90L)
            enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 120f))
        }
    }

    // press: squash
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )

    // icon bob + light sweep, each card on its own phase
    val inf = rememberInfiniteTransition(label = "card$index")
    val bob by inf.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            StartOffset(index * 400, StartOffsetType.FastForward),
        ),
        label = "bob",
    )
    val shine by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0f at 0
                1f at 1400 using FastOutSlowInEasing
                1f at 4000
            },
            initialStartOffset = StartOffset(index * 500, StartOffsetType.FastForward),
        ),
        label = "shine",
    )

    val iconSize = if (tablet) 36.sp else 30.sp
    val nameSize = if (tablet) 16.sp else 13.sp

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val e = enter.value
                alpha = e.coerceIn(0f, 1f)
                val sc = (0.6f + 0.4f * e) * press
                scaleX = sc
                scaleY = sc
                translationX = (if (index % 2 == 0) -1f else 1f) * (1f - e) * 140f
            }
            .shadow(if (pressed) 3.dp else 12.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Brush.linearGradient(listOf(topic.color, topic.color.copy(alpha = 0.55f), Color(0xFF2A1B13))))
            .border(BorderStroke(1.5.dp, Brush.verticalGradient(listOf(Color(0xFFF1D3A5).copy(alpha = 0.7f), Color.Black.copy(alpha = 0.5f)))), shape)
            .drawWithContent {
                drawContent()
                val w = size.width
                val x = -0.4f * w + shine * 1.6f * w
                drawRect(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent),
                        startX = x,
                        endX = x + 0.4f * w,
                    ),
                )
            }
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(vertical = if (tablet) 22.dp else 15.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (wide) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(topic.icon, fontSize = iconSize, color = Color.White, modifier = Modifier.graphicsLayer { translationY = bob })
                Text(topic.name, fontSize = nameSize, fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = 0.7.sp)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(topic.icon, fontSize = iconSize, color = Color.White, modifier = Modifier.graphicsLayer { translationY = bob })
                Text(topic.name, fontSize = nameSize, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center, letterSpacing = 0.7.sp)
            }
        }
    }
}
