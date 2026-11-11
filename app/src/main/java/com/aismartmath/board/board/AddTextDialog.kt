package com.aismartmath.board.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.ui.components.MathTextView

/** Add your own text (a question, a note). After adding, move it, resize it, change its colour and size. */
@Composable
fun AddTextDialog(state: BoardState) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { state.textDialog = false },
        containerColor = Color(0xFF2A1B12),
        title = { Text("Add text", color = Color(0xFFF1D3A5), fontSize = 15.sp) },
        text = {
            Column {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color(0xFFD08A4E)),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("Type or paste your question…", color = Color(0xFF9C8470), fontSize = 14.sp)
                    inner()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 70.dp, max = 100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF160E0A))
                    .border(BorderStroke(1.dp, Color(0xFF6B4630)), RoundedCornerShape(8.dp))
                    .padding(8.dp),
            )
            if (text.isNotBlank()) {
                Text("Looks like this on the board:", color = Color(0xFFB59A83), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp, bottom = 3.dp))
                // fixed height whatever the length: long text scrolls inside, the box never grows
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF160E0A))
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                ) { MathTextView(text, 16.sp, Color.White, Modifier.fillMaxWidth()) }
            }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isNotBlank()) {
                    state.textDialog = false
                    state.addText(text)
                }
            }) { Text("Add", color = Color(0xFFF1D3A5)) }
        },
        dismissButton = {
            TextButton(onClick = { state.textDialog = false }) { Text("Cancel", color = Color(0xFFB59A83)) }
        },
    )
}
